#!/usr/bin/env python3
"""Exercise the bundled SQLite schema on the host; Android repository tests remain separate."""
from pathlib import Path
import sqlite3
import tempfile
import unittest
from concurrent.futures import ThreadPoolExecutor

SCHEMA = (Path(__file__).resolve().parents[1] / "app/src/main/assets/database/schema_v2.sql").read_text(encoding="utf-8")
LONG_MAX = 2**63 - 1


def connect(path=":memory:"):
    database = sqlite3.connect(path, timeout=15, isolation_level=None)
    database.execute("PRAGMA foreign_keys=ON")
    return database


def adopt(database, template="question"):
    return database.execute(
        "INSERT INTO user_routines(hero_id,template_id,title,local_time,weekday_mask,created_on) VALUES(?,?,?,?,?,?)",
        ("franklin", template, "한글 🌿 질문", "07:00", 127, "2026-10-12"),
    ).lastrowid


def reward(database, routine_id, date="2026-10-12"):
    """Native schema/transaction exercise, not a substitute for AppRepository device tests."""
    database.execute("BEGIN IMMEDIATE")
    try:
        hero_id, template_id = database.execute("SELECT hero_id,template_id FROM user_routines WHERE id=?", (routine_id,)).fetchone()
        if database.execute("SELECT 1 FROM completion_records WHERE hero_id=? AND template_id=? AND local_date=?", (hero_id, template_id, date)).fetchone():
            database.execute("COMMIT")
            return False
        changed = database.execute("UPDATE wallet SET balance=balance+10 WHERE singleton_id=1 AND balance<=?", (LONG_MAX - 10,)).rowcount
        if changed != 1:
            database.execute("ROLLBACK")
            return False
        database.execute("INSERT INTO completion_records VALUES(?,?,?,?,?,?,?)", (routine_id, date, hero_id, template_id, "한글 🌿 질문", 10, 1))
        database.execute("COMMIT")
        return True
    except Exception:
        database.execute("ROLLBACK")
        raise


class DatabaseSchemaTest(unittest.TestCase):
    def setUp(self):
        self.db = connect()
        self.db.executescript(SCHEMA)

    def tearDown(self):
        self.db.close()

    def test_schema_creates_zero_wallet_and_enables_foreign_keys(self):
        self.assertEqual((0,), self.db.execute("SELECT balance FROM wallet").fetchone())
        self.assertEqual((1,), self.db.execute("PRAGMA foreign_keys").fetchone())

    def test_unicode_title_is_retained(self):
        identifier = adopt(self.db)
        self.assertEqual(("한글 🌿 질문",), self.db.execute("SELECT title FROM user_routines WHERE id=?", (identifier,)).fetchone())

    def test_blank_title_invalid_time_and_weekdays_are_rejected(self):
        identifier = adopt(self.db)
        for column, value in (("title", " "), ("local_time", "24:00"), ("local_time", "09:60"), ("weekday_mask", 0), ("weekday_mask", 128)):
            with self.subTest(column=column, value=value), self.assertRaises(sqlite3.IntegrityError):
                self.db.execute(f"UPDATE user_routines SET {column}=? WHERE id=?", (value, identifier))

    def test_live_template_unique_but_deleted_adoption_gets_new_id(self):
        old = adopt(self.db)
        with self.assertRaises(sqlite3.IntegrityError):
            adopt(self.db)
        self.db.execute("UPDATE user_routines SET deleted=1,enabled=0 WHERE id=?", (old,))
        self.assertGreater(adopt(self.db), old)

    def test_no_orphan_completion_or_unowned_applied_item(self):
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute("INSERT INTO completion_records VALUES(999,'2026-10-12','franklin','question','질문',10,1)")
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute("INSERT INTO applied_item VALUES(1,'unowned')")

    def test_duplicate_completion_has_one_wallet_credit(self):
        identifier = adopt(self.db)
        self.assertTrue(reward(self.db, identifier))
        self.assertFalse(reward(self.db, identifier))
        self.assertEqual((10,), self.db.execute("SELECT balance FROM wallet").fetchone())
        self.assertEqual((1,), self.db.execute("SELECT count(*) FROM completion_records").fetchone())

    def test_readoption_cannot_repeat_reward_and_status_maps_to_current_id(self):
        old = adopt(self.db)
        self.assertTrue(reward(self.db, old))
        self.db.execute("UPDATE user_routines SET deleted=1,enabled=0 WHERE id=?", (old,))
        current = adopt(self.db)
        self.assertGreater(current, old)
        self.assertFalse(reward(self.db, current))
        self.assertEqual((10,), self.db.execute("SELECT balance FROM wallet").fetchone())
        self.assertEqual((old,), self.db.execute("SELECT routine_id FROM completion_records").fetchone())
        mapped = self.db.execute(
            "SELECT u.id FROM user_routines u JOIN completion_records c ON c.hero_id=u.hero_id AND c.template_id=u.template_id WHERE u.deleted=0 AND c.local_date=?",
            ("2026-10-12",),
        ).fetchall()
        self.assertEqual([(current,)], mapped)
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute("INSERT INTO completion_records VALUES(?,?,?,?,?,?,?)", (current, "2026-10-12", "franklin", "question", "새 제목", 10, 1))
        self.assertTrue(reward(self.db, current, "2026-10-13"))
        self.assertEqual((20,), self.db.execute("SELECT balance FROM wallet").fetchone())

    def test_insert_failure_rolls_back_credit(self):
        identifier = adopt(self.db)
        self.db.execute("CREATE TRIGGER reject_completion BEFORE INSERT ON completion_records BEGIN SELECT RAISE(ABORT,'injected'); END")
        with self.assertRaises(sqlite3.IntegrityError):
            reward(self.db, identifier)
        self.assertEqual((0,), self.db.execute("SELECT balance FROM wallet").fetchone())

    def test_negative_and_overflow_balance_are_rejected(self):
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute("UPDATE wallet SET balance=-1")
        self.db.execute("UPDATE wallet SET balance=?", (LONG_MAX,))
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute("UPDATE wallet SET balance=balance+10")
        self.assertEqual((LONG_MAX,), self.db.execute("SELECT balance FROM wallet").fetchone())

    def test_guarded_overflow_produces_no_completion(self):
        identifier = adopt(self.db)
        self.db.execute("UPDATE wallet SET balance=?", (LONG_MAX - 9,))
        self.assertFalse(reward(self.db, identifier))
        self.assertEqual((0,), self.db.execute("SELECT count(*) FROM completion_records").fetchone())

    def test_purchase_failure_rolls_back_debit_and_ownership(self):
        self.db.execute("UPDATE wallet SET balance=30")
        self.db.execute("CREATE TRIGGER reject_purchase BEFORE INSERT ON owned_items BEGIN SELECT RAISE(ABORT,'injected'); END")
        self.db.execute("BEGIN IMMEDIATE")
        with self.assertRaises(sqlite3.IntegrityError):
            try:
                self.db.execute("UPDATE wallet SET balance=balance-30 WHERE singleton_id=1 AND balance>=30")
                self.db.execute("INSERT INTO owned_items VALUES('badge',30,1)")
                self.db.execute("COMMIT")
            except Exception:
                self.db.execute("ROLLBACK")
                raise
        self.assertEqual((30,), self.db.execute("SELECT balance FROM wallet").fetchone())
        self.assertEqual((0,), self.db.execute("SELECT count(*) FROM owned_items").fetchone())

    def test_history_prevents_hard_delete_and_retains_snapshot(self):
        identifier = adopt(self.db)
        reward(self.db, identifier)
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute("DELETE FROM user_routines WHERE id=?", (identifier,))
        self.db.execute("UPDATE user_routines SET title='새 제목',deleted=1 WHERE id=?", (identifier,))
        self.assertEqual(("한글 🌿 질문",), self.db.execute("SELECT title FROM completion_records").fetchone())

    def test_legacy_history_does_not_reference_new_ids_or_credit_wallet(self):
        self.db.execute("INSERT OR IGNORE INTO legacy_completions VALUES('sejong','2026-10-12','1')")
        self.db.execute("INSERT OR IGNORE INTO legacy_completions VALUES('sejong','2026-10-12','1')")
        self.assertEqual((1,), self.db.execute("SELECT count(*) FROM legacy_completions").fetchone())
        self.assertEqual((0,), self.db.execute("SELECT balance FROM wallet").fetchone())

    def test_date_queries_have_indexes(self):
        indexes = {row[1] for row in self.db.execute("PRAGMA index_list(completion_records)")}
        self.assertTrue({"completion_date", "completion_hero_date"}.issubset(indexes))
        plan = str(self.db.execute("EXPLAIN QUERY PLAN SELECT routine_id FROM completion_records WHERE local_date=?", ("2026-10-12",)).fetchall())
        self.assertIn("completion_date", plan)

    def test_concurrent_connections_credit_same_day_once(self):
        with tempfile.TemporaryDirectory(prefix="routine-sqlite-") as folder:
            path = str(Path(folder) / "test.db")
            database = connect(path)
            database.executescript(SCHEMA)
            identifier = adopt(database)
            database.close()

            def work(_):
                connection = connect(path)
                try:
                    return reward(connection, identifier)
                finally:
                    connection.close()

            with ThreadPoolExecutor(max_workers=6) as pool:
                results = list(pool.map(work, range(24)))
            self.assertEqual(1, sum(results))
            database = connect(path)
            try:
                self.assertEqual((10,), database.execute("SELECT balance FROM wallet").fetchone())
                self.assertEqual((1,), database.execute("SELECT count(*) FROM completion_records").fetchone())
                self.assertEqual(("ok",), database.execute("PRAGMA integrity_check").fetchone())
            finally:
                database.close()


class DatabaseV2Test(unittest.TestCase):
    def test_actual_v1_fixture_preserves_all_columns_and_balance(self):
        import shutil
        root = Path(__file__).resolve().parents[1]
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "upgrade.db"
            shutil.copy(root / "app/src/androidTest/assets/historical_routines_v1.db", path)
            db = connect(path)
            self.assertEqual((1,), db.execute("PRAGMA user_version").fetchone())
            tables = ("user_routines", "completion_records", "wallet", "owned_items", "applied_item", "legacy_completions", "app_metadata")
            before = {table: db.execute(f"SELECT * FROM {table}").fetchall() for table in tables}
            db.executescript("BEGIN IMMEDIATE;" + (root / "app/src/main/assets/database/migration_1_2.sql").read_text() + "PRAGMA user_version=2;COMMIT;")
            for table in tables:
                after = db.execute(f"SELECT * FROM {table}").fetchall()
                self.assertEqual(before[table], [row[:-1] for row in after] if table == "user_routines" else after)
            self.assertEqual(("PERSONAL",), db.execute("SELECT source FROM user_routines").fetchone())
            self.assertEqual((80,), db.execute("SELECT balance FROM wallet").fetchone())
            fresh = connect(); fresh.executescript(SCHEMA)
            def structure(database):
                return sorted(database.execute("SELECT type,name,tbl_name,sql FROM sqlite_master WHERE name NOT LIKE 'sqlite_%'").fetchall())
            self.assertEqual(structure(fresh), structure(db))
            self.assertEqual(("ok",), db.execute("PRAGMA integrity_check").fetchone())
            self.assertEqual([], db.execute("PRAGMA foreign_key_check").fetchall())
            fresh.close(); db.close()

    def test_sources_allow_separate_adoption_but_one_reward(self):
        db = connect(); db.executescript(SCHEMA)
        personal = adopt(db)
        hero = db.execute("INSERT INTO user_routines(hero_id,template_id,title,local_time,weekday_mask,created_on,source) VALUES('franklin','question','하루','09:00',127,'2026-10-12','HERO_DAY')").lastrowid
        self.assertTrue(reward(db, personal)); self.assertFalse(reward(db, hero)); self.assertFalse(reward(db, personal))
        self.assertEqual((10,), db.execute("SELECT balance FROM wallet").fetchone())
        with self.assertRaises(sqlite3.IntegrityError):
            db.execute("UPDATE user_routines SET source='OTHER' WHERE id=?", (hero,))
        db.execute("UPDATE user_routines SET local_time='10:00',enabled=0,alarm_enabled=1 WHERE id=?", (personal,))
        self.assertEqual(("09:00",1,0), db.execute("SELECT local_time,enabled,alarm_enabled FROM user_routines WHERE source='HERO_DAY'").fetchone())
        db.close()

    def test_whole_day_rolls_back_on_block_failure(self):
        db = connect(); db.executescript(SCHEMA)
        db.execute("BEGIN IMMEDIATE")
        with self.assertRaises(sqlite3.IntegrityError):
            for index in range(8):
                db.execute("INSERT INTO user_routines(hero_id,template_id,title,local_time,weekday_mask,created_on,source) VALUES('sejong',?,?,?,127,'2026-10-12','HERO_DAY')", (f"day_{index}", "하루", "24:00" if index == 4 else "07:00"))
        db.execute("ROLLBACK")
        self.assertEqual((0,), db.execute("SELECT count(*) FROM user_routines").fetchone())
        self.assertEqual((0,), db.execute("SELECT balance FROM wallet").fetchone())
        db.close()

    def test_catalog_full_days_preserve_v2_template_ids_and_verification(self):
        import json
        import subprocess
        root = Path(__file__).resolve().parents[1]
        catalog = json.loads((root / "docs/historical-catalog.json").read_text())
        original = json.loads(subprocess.check_output(["git", "show", "4388a30:docs/historical-catalog.json"], cwd=root))
        self.assertEqual(30, len(catalog))
        self.assertEqual({h["id"] for h in original}, {h["id"] for h in catalog[:15]})
        self.assertEqual({q["id"] for h in original for q in h["routines"]}, {q["id"] for h in catalog[:15] for q in h["routines"]})
        all_ids = set()
        for hero in catalog:
            blocks = hero["heroDay"]["blocks"]
            self.assertTrue(5 <= len(blocks) <= 8)
            self.assertTrue({q["id"] for q in hero["routines"]}.issubset({b["id"] for b in blocks}))
            for block in blocks:
                self.assertNotIn(block["id"], all_ids); all_ids.add(block["id"])
                self.assertIn(block["grade"], ("RECORDED", "RECONSTRUCTED", "MODERN"))
                self.assertTrue(block["source"]["title"] and block["source"]["url"] and block["source"]["locator"])
                if block["grade"] == "RECORDED":
                    self.assertTrue(block["recordedTimeVerified"] and block["recordedActionVerified"] and block["historicalTime"])
                if block["source"]["verification"]["status"] == "UNVERIFIED": self.assertEqual("MODERN", block["grade"])



if __name__ == "__main__":
    unittest.main(verbosity=2)
