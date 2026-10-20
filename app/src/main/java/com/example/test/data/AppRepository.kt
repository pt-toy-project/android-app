package com.example.test.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.test.Routine.RoutineDate
import java.io.Closeable

/**
 * Blocking local database API. Construct, use and close on a worker thread.
 * Database transactions, constraints and immutable record snapshots protect history/rewards.
 */
class AppRepository(
    context: Context,
    databaseName: String = "historical_routines.db",
    legacyPreferencesName: String = "routine_state_v1",
    private val todayProvider: () -> RoutineDate = { RoutineDate.today() }
) : Closeable {
    private val helper = AppDatabase(context.applicationContext, databaseName)
    private val db: SQLiteDatabase get() = helper.writableDatabase

    init {
        try {
            importLegacy(context.applicationContext, legacyPreferencesName)
        } catch (exception: Exception) {
            // The constructor may fail before the caller enters use {}, so close here as well.
            runCatching { helper.close() }.onFailure(exception::addSuppressed)
            throw exception
        }
    }

    var selectedHeroId: String
        get() = metadata("selected_hero")?.takeIf { it.matches(Regex("[a-zA-Z0-9_-]{1,80}")) } ?: "sejong"
        set(value) {
            require(value.matches(Regex("[a-zA-Z0-9_-]{1,80}")))
            putMetadata("selected_hero", value)
        }

    /** Without a date includes disabled routines for management; with a date returns that day's schedule. */
    fun listRoutines(heroId: String? = null, date: RoutineDate? = null, source: RoutineSource? = null): List<UserRoutine> {
        val selection = if (heroId == null) "deleted=0" else "deleted=0 AND hero_id=?"
        val arguments = heroId?.let { arrayOf(it) }
        val routines = db.query("user_routines", null, selection, arguments, null, null, "local_time,id").use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.routine()) }
        }
        val scoped = routines.filter { source == null || it.source == source }
        if (date == null) return scoped
        val completedIds = completedRoutineIds(date, heroId)
        return scoped.filter {
            // Do not present routines before adoption or disabled routines as missed work.
            it.id in completedIds || (it.enabled && date.iso >= it.createdOn && RoutineRules.scheduledOn(it.weekdayMask, date))
        }
    }

    fun getRoutine(id: Long): UserRoutine? = queryRoutine(id, includeDeleted = false)

    fun addRoutine(heroId: String, templateId: String, title: String, time: String, weekdayMask: Int = 127, source: RoutineSource = RoutineSource.PERSONAL): Long {
        RoutineRules.validateRoutine(heroId, templateId, title, time, weekdayMask)
        return transaction {
            db.query("user_routines", arrayOf("id"), "hero_id=? AND template_id=? AND source=? AND deleted=0", arrayOf(heroId, templateId, source.name), null, null, null).use {
                if (it.moveToFirst()) return@transaction it.getLong(0)
            }
            db.insertOrThrow("user_routines", null, ContentValues().apply {
                put("hero_id", heroId); put("template_id", templateId); put("title", title.trim())
                put("local_time", time); put("weekday_mask", weekdayMask)
                put("enabled", 1); put("alarm_enabled", 0); put("deleted", 0)
                put("created_on", todayProvider().iso); put("source", source.name)
            })
        }
    }

    /** A new local plan has its own stable identity and uses the existing reward/history schema. */
    fun addPersonalPlan(title: String, time: String, weekdayMask: Int = 127,
                        enabled: Boolean = true, alarmEnabled: Boolean = false): Long = transaction {
        val templateId = "manual_" + java.util.UUID.randomUUID().toString().replace("-", "")
        val id = addRoutine(RoutineIdentity.PERSONAL_HERO_ID, templateId, title, time, weekdayMask,
            RoutineSource.PERSONAL)
        db.update("user_routines", ContentValues().apply {
            put("enabled", if (enabled) 1 else 0)
            put("alarm_enabled", if (alarmEnabled) 1 else 0)
        }, "id=?", arrayOf(id.toString()))
        id
    }

    /** All blocks commit together. Existing adoptions are returned without updating user settings. */
    fun startHeroDay(hero: com.example.test.Hero.Hero): List<Long> = transaction {
        val day = com.example.test.Hero.HeroDayCatalog.forHero(hero)
        val ids = day.blocks.map { block ->
            addRoutine(hero.id, block.id, block.title, block.suggestedTime, source = RoutineSource.HERO_DAY)
        }
        selectedHeroId = hero.id
        ids
    }

    fun updateRoutine(routine: UserRoutine) {
        RoutineRules.validateRoutine(routine.heroId, routine.templateId, routine.title, routine.time, routine.weekdayMask)
        transaction {
            val original = getRoutine(routine.id) ?: return@transaction
            require(original.heroId == routine.heroId && original.templateId == routine.templateId && original.source == routine.source) { "Routine identity cannot change" }
            db.update("user_routines", ContentValues().apply {
                put("title", routine.title.trim()); put("local_time", routine.time); put("weekday_mask", routine.weekdayMask)
                put("enabled", if (routine.enabled) 1 else 0); put("alarm_enabled", if (routine.alarmEnabled) 1 else 0)
            }, "id=? AND deleted=0", arrayOf(routine.id.toString()))
        }
    }

    /** Retain ID, historical snapshots and rewards. IDs are never reused. */
    fun deleteRoutine(id: Long) {
        db.update("user_routines", ContentValues().apply {
            put("deleted", 1); put("enabled", 0); put("alarm_enabled", 0)
        }, "id=?", arrayOf(id.toString()))
    }

    fun complete(id: Long, date: RoutineDate, today: RoutineDate = todayProvider()): CompletionOutcome = transaction {
        val routine = queryRoutine(id, includeDeleted = true)
        RoutineRules.completionFailure(routine, date, today, isCompleted(id, date), balance())?.let {
            return@transaction it
        }
        requireNotNull(routine)
        // Integer boundary is checked both in Kotlin and in the UPDATE predicate.
        val updated = db.compileStatement("UPDATE wallet SET balance=balance+? WHERE singleton_id=1 AND balance<=?").use {
            it.bindLong(1, RoutineRules.COMPLETION_REWARD)
            it.bindLong(2, Long.MAX_VALUE - RoutineRules.COMPLETION_REWARD)
            it.executeUpdateDelete()
        }
        if (updated != 1) return@transaction CompletionOutcome.BALANCE_OVERFLOW
        db.insertOrThrow("completion_records", null, ContentValues().apply {
            put("routine_id", id); put("local_date", date.iso); put("hero_id", routine.heroId)
            put("template_id", routine.templateId)
            put("title", routine.title); put("coins", RoutineRules.COMPLETION_REWARD)
            put("completed_at", System.currentTimeMillis().coerceAtLeast(0))
        })
        CompletionOutcome.COMPLETED
    }

    fun isCompleted(id: Long, date: RoutineDate): Boolean = db.rawQuery(
        "SELECT 1 FROM user_routines u JOIN completion_records c " +
            "ON c.hero_id=u.hero_id AND c.template_id=u.template_id WHERE u.id=? AND c.local_date=? LIMIT 1",
        arrayOf(id.toString(), date.iso)
    ).use { it.moveToFirst() }

    /** Map canonical habit/date records to current adoption IDs, including after delete + re-adopt. */
    fun completedRoutineIds(date: RoutineDate, heroId: String? = null): Set<Long> = db.rawQuery(
        "SELECT u.id FROM user_routines u JOIN completion_records c " +
            "ON c.hero_id=u.hero_id AND c.template_id=u.template_id WHERE u.deleted=0 AND c.local_date=?" +
            if (heroId == null) "" else " AND u.hero_id=?",
        if (heroId == null) arrayOf(date.iso) else arrayOf(date.iso, heroId)
    ).use { buildSet { while (it.moveToNext()) add(it.getLong(0)) } }

    /** Each adoption/source has its own checklist. Reading never changes wallet or history. */
    fun questChecks(id: Long, date: RoutineDate): Set<String> = db.query(
        "quest_checks", arrayOf("quest_id"), "routine_id=? AND local_date=?",
        arrayOf(id.toString(), date.iso), null, null, null
    ).use { cursor -> buildSet { while (cursor.moveToNext()) add(cursor.getString(0)) } }

    fun questChecksForDate(date: RoutineDate): Map<Long, Set<String>> = db.query(
        "quest_checks", arrayOf("routine_id", "quest_id"), "local_date=?", arrayOf(date.iso), null, null, null
    ).use { cursor ->
        val rows = linkedMapOf<Long, MutableSet<String>>()
        while (cursor.moveToNext()) rows.getOrPut(cursor.getLong(0)) { linkedSetOf() }.add(cursor.getString(1))
        rows
    }

    /** Reject stale, forged and unavailable checklist writes inside the same transaction as the check. */
    fun setQuestChecked(id: Long, questId: String, date: RoutineDate, checked: Boolean): Boolean = transaction {
        val routine = getRoutine(id) ?: return@transaction false
        if (date > todayProvider() || date.iso < routine.createdOn || !routine.enabled ||
            !RoutineRules.scheduledOn(routine.weekdayMask, date)) return@transaction false
        val block = com.example.test.Hero.HeroDayCatalog.find(routine.heroId, routine.templateId)
            ?: return@transaction false
        if (block.quests.none { it.id == questId }) return@transaction false
        if (checked) {
            db.insertWithOnConflict("quest_checks", null, ContentValues().apply {
                put("routine_id", id); put("quest_id", questId); put("local_date", date.iso)
                put("checked_at", System.currentTimeMillis().coerceAtLeast(0))
            }, SQLiteDatabase.CONFLICT_IGNORE)
        } else db.delete("quest_checks", "routine_id=? AND quest_id=? AND local_date=?",
            arrayOf(id.toString(), questId, date.iso))
        true
    }

    fun balance(): Long = db.rawQuery("SELECT balance FROM wallet WHERE singleton_id=1", null).use {
        check(it.moveToFirst()) { "Wallet missing" }; it.getLong(0)
    }

    fun history(heroId: String? = null, limit: Int = 100, offset: Int = 0): List<CompletionRecord> {
        require(limit in 1..500 && offset >= 0)
        return db.query(
            "completion_records", null, if (heroId == null) null else "hero_id=?", heroId?.let { arrayOf(it) },
            null, null, "local_date DESC,routine_id", "$offset,$limit"
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val date = RoutineDate.parse(cursor.string("local_date")) ?: continue
                    add(CompletionRecord(cursor.long("routine_id"), date, cursor.string("title"), cursor.string("hero_id"), cursor.long("coins")))
                }
            }
        }
    }

    /** Whole-app calendar: immutable earned records survive deletion and are never counted twice.
     * Schedule denominators reflect current settings; this is not a historical schedule snapshot.
     * Bounded range queries avoid the paginated history API's 100-record default limit.
     */
    fun calendarActivity(start: RoutineDate, end: RoutineDate): List<com.example.test.Routine.DayActivity> {
        require(start <= end)
        val dates = mutableListOf<RoutineDate>()
        var day = start
        while (day <= end) {
            require(dates.size < 62) { "Calendar range is limited to 62 days" }
            dates += day
            if (day == end) break
            day = day.plusDays(1)
        }
        val earned = db.rawQuery(
            "SELECT local_date,COUNT(*),SUM(coins) FROM completion_records WHERE local_date BETWEEN ? AND ? GROUP BY local_date",
            arrayOf(start.iso, end.iso)
        ).use { cursor -> buildMap {
            while (cursor.moveToNext()) put(cursor.getString(0), cursor.getInt(1) to cursor.getLong(2))
        } }
        val checks = db.rawQuery(
            "SELECT local_date,COUNT(*) FROM quest_checks WHERE local_date BETWEEN ? AND ? GROUP BY local_date",
            arrayOf(start.iso, end.iso)
        ).use { cursor -> buildMap {
            while (cursor.moveToNext()) put(cursor.getString(0), cursor.getInt(1))
        } }
        val routines = listRoutines()
        val questSizes = routines.associate { routine -> routine.id to
            (com.example.test.Hero.HeroDayCatalog.find(routine.heroId, routine.templateId)?.quests?.size ?: 0) }
        return dates.map { date ->
            val scheduled = routines.filter { it.enabled && date.iso >= it.createdOn && RoutineRules.scheduledOn(it.weekdayMask, date) }
            // Completion rewards share a hero/template identity across the two sources.
            val planned = scheduled.distinctBy { it.heroId to it.templateId }.size
            val record = earned[date.iso]
            com.example.test.Routine.DayActivity(date, planned, record?.first ?: 0,
                scheduled.sumOf { questSizes.getValue(it.id) }, checks[date.iso] ?: 0, record?.second ?: 0)
        }
    }

    /** Achievements are derived from retained records and never grant additional rewards. */
    fun progressSummary(today: RoutineDate = todayProvider()): ProgressSummary {
        val earned = db.rawQuery("SELECT COUNT(*),COALESCE(SUM(coins),0) FROM completion_records WHERE local_date<=?", arrayOf(today.iso)).use {
            it.moveToFirst(); it.getInt(0) to it.getLong(1)
        }
        val dates = db.rawQuery(
            "SELECT local_date FROM completion_records WHERE local_date<=? UNION SELECT local_date FROM quest_checks WHERE local_date<=?",
            arrayOf(today.iso, today.iso)
        ).use { cursor -> buildSet { while (cursor.moveToNext()) RoutineDate.parse(cursor.getString(0))?.let(::add) } }
        return ProgressSummary(earned.first, dates.size, earned.second, ProgressSummary.streak(dates, today))
    }

    fun completedCount(heroId: String, date: RoutineDate): Int = db.rawQuery(
        "SELECT count(*) FROM completion_records WHERE hero_id=? AND local_date=?", arrayOf(heroId, date.iso)
    ).use { it.moveToFirst(); it.getInt(0) }

    fun purchase(itemId: String): PurchaseOutcome = transaction {
        val item = ShopCatalog.find(itemId)
        RoutineRules.purchaseFailure(item, owns(itemId), balance())?.let { return@transaction it }
        requireNotNull(item)
        val updated = db.compileStatement("UPDATE wallet SET balance=balance-? WHERE singleton_id=1 AND balance>=?").use {
            it.bindLong(1, item.price); it.bindLong(2, item.price); it.executeUpdateDelete()
        }
        if (updated != 1) return@transaction PurchaseOutcome.INSUFFICIENT_FUNDS
        db.insertOrThrow("owned_items", null, ContentValues().apply {
            put("item_id", item.id); put("price_paid", item.price)
            put("purchased_at", System.currentTimeMillis().coerceAtLeast(0))
        })
        PurchaseOutcome.PURCHASED
    }

    fun ownedItemIds(): Set<String> = db.query("owned_items", arrayOf("item_id"), null, null, null, null, "item_id").use {
        buildSet { while (it.moveToNext()) add(it.getString(0)) }
    }

    fun appliedItemId(): String? = db.rawQuery("SELECT item_id FROM applied_item WHERE singleton_id=1", null).use {
        if (it.moveToFirst()) it.getString(0) else null
    }

    fun applyItem(itemId: String): Boolean = transaction {
        if (!owns(itemId)) return@transaction false
        db.insertWithOnConflict("applied_item", null, ContentValues().apply {
            put("singleton_id", 1); put("item_id", itemId)
        }, SQLiteDatabase.CONFLICT_REPLACE)
        true
    }

    fun legacyHistory(limit: Int = 100, heroId: String? = null): List<LegacyCompletion> {
        require(limit in 1..500)
        return db.query("legacy_completions", null, if (heroId == null) null else "hero_id=?",
            heroId?.let { arrayOf(it) }, null, null, "local_date DESC,hero_id,sample_id", limit.toString()).use {
            buildList { while (it.moveToNext()) add(LegacyCompletion(it.string("hero_id"), it.string("local_date"), it.string("sample_id"))) }
        }
    }

    fun legacyHistoryCount(heroId: String? = null): Int = db.rawQuery(
        "SELECT count(*) FROM legacy_completions" + if (heroId == null) "" else " WHERE hero_id=?",
        heroId?.let { arrayOf(it) }
    ).use {
        it.moveToFirst(); it.getInt(0)
    }

    override fun close() = helper.close()

    private fun owns(itemId: String): Boolean = db.query("owned_items", arrayOf("item_id"), "item_id=?", arrayOf(itemId), null, null, null, "1").use { it.moveToFirst() }

    private fun queryRoutine(id: Long, includeDeleted: Boolean): UserRoutine? = db.query(
        "user_routines", null, if (includeDeleted) "id=?" else "id=? AND deleted=0", arrayOf(id.toString()), null, null, null, "1"
    ).use { if (it.moveToFirst()) it.routine() else null }

    private fun metadata(key: String): String? = db.query("app_metadata", arrayOf("value"), "key=?", arrayOf(key), null, null, null, "1").use {
        if (it.moveToFirst()) it.getString(0) else null
    }

    private fun putMetadata(key: String, value: String) {
        db.insertWithOnConflict("app_metadata", null, ContentValues().apply { put("key", key); put("value", value) }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun importLegacy(context: Context, preferencesName: String) = transaction {
        if (metadata("legacy_import_v1") == "1") return@transaction
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val values = preferences.all
        val selected = values["selected_hero"] as? String
        if (metadata("selected_hero") == null && !selected.isNullOrBlank()) putMetadata("selected_hero", selected)
        val pattern = Regex("^completed:([^:]+):(\\d{4}-\\d{2}-\\d{2})$")
        values.forEach { (key, value) ->
            val match = pattern.matchEntire(key) ?: return@forEach
            // Original preferences are retained even when an old entry cannot be interpreted.
            val date = RoutineDate.parse(match.groupValues[2]) ?: return@forEach
            (value as? Set<*>)?.filterIsInstance<String>()?.forEach { sampleId ->
                db.insertWithOnConflict("legacy_completions", null, ContentValues().apply {
                    put("hero_id", match.groupValues[1]); put("local_date", date.iso); put("sample_id", sampleId)
                }, SQLiteDatabase.CONFLICT_IGNORE)
            }
        }
        // Import is committed atomically and grants no retrospective rewards.
        putMetadata("legacy_import_v1", "1")
        putMetadata("storage_version", "1")
    }

    private inline fun <T> transaction(block: () -> T): T {
        val database = db
        database.beginTransaction()
        try {
            val result = block()
            database.setTransactionSuccessful()
            return result
        } finally {
            database.endTransaction()
        }
    }

    private fun Cursor.string(column: String): String = getString(getColumnIndexOrThrow(column))
    private fun Cursor.long(column: String): Long = getLong(getColumnIndexOrThrow(column))
    private fun Cursor.routine() = UserRoutine(
        long("id"), string("hero_id"), string("template_id"), string("title"), string("local_time"),
        getInt(getColumnIndexOrThrow("weekday_mask")), getInt(getColumnIndexOrThrow("enabled")) == 1,
        getInt(getColumnIndexOrThrow("alarm_enabled")) == 1, string("created_on"), getInt(getColumnIndexOrThrow("deleted")) == 1,
        RoutineSource.valueOf(string("source"))
    )
}
