-- The bundled versioned schema is also exercised by scripts/verify_database.py.
-- Foreign keys are enabled by SQLiteOpenHelper.onConfigure before these statements.
CREATE TABLE app_metadata (
    key TEXT PRIMARY KEY NOT NULL,
    value TEXT NOT NULL
);
CREATE TABLE user_routines (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    hero_id TEXT NOT NULL CHECK(length(hero_id) BETWEEN 1 AND 80),
    template_id TEXT NOT NULL CHECK(length(template_id) BETWEEN 1 AND 120),
    title TEXT NOT NULL CHECK(length(trim(title)) BETWEEN 1 AND 160),
    local_time TEXT NOT NULL CHECK(length(local_time) = 5 AND local_time GLOB '[0-2][0-9]:[0-5][0-9]' AND substr(local_time,1,2) <= '23'),
    weekday_mask INTEGER NOT NULL CHECK(typeof(weekday_mask) = 'integer' AND weekday_mask BETWEEN 1 AND 127),
    enabled INTEGER NOT NULL DEFAULT 1 CHECK(enabled IN (0,1)),
    alarm_enabled INTEGER NOT NULL DEFAULT 0 CHECK(alarm_enabled IN (0,1)),
    deleted INTEGER NOT NULL DEFAULT 0 CHECK(deleted IN (0,1)),
    created_on TEXT NOT NULL CHECK(length(created_on) = 10 AND created_on GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]')
);
CREATE UNIQUE INDEX user_routine_active_template ON user_routines(hero_id,template_id) WHERE deleted=0;
CREATE INDEX user_routine_hero ON user_routines(hero_id,deleted,local_time);
CREATE TABLE wallet (
    singleton_id INTEGER PRIMARY KEY CHECK(singleton_id=1),
    balance INTEGER NOT NULL DEFAULT 0 CHECK(typeof(balance)='integer' AND balance >= 0)
);
INSERT INTO wallet(singleton_id,balance) VALUES(1,0);
CREATE TABLE completion_records (
    routine_id INTEGER NOT NULL REFERENCES user_routines(id) ON DELETE RESTRICT,
    local_date TEXT NOT NULL CHECK(length(local_date)=10 AND local_date GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'),
    hero_id TEXT NOT NULL,
    template_id TEXT NOT NULL,
    title TEXT NOT NULL,
    coins INTEGER NOT NULL CHECK(typeof(coins)='integer' AND coins=10),
    completed_at INTEGER NOT NULL CHECK(typeof(completed_at)='integer' AND completed_at >= 0),
    PRIMARY KEY(routine_id,local_date),
    UNIQUE(hero_id,template_id,local_date)
);
CREATE INDEX completion_hero_date ON completion_records(hero_id,local_date);
CREATE INDEX completion_date ON completion_records(local_date,routine_id);
CREATE TABLE owned_items (
    item_id TEXT PRIMARY KEY NOT NULL,
    price_paid INTEGER NOT NULL CHECK(typeof(price_paid)='integer' AND price_paid > 0),
    purchased_at INTEGER NOT NULL CHECK(typeof(purchased_at)='integer' AND purchased_at >= 0)
);
CREATE TABLE applied_item (
    singleton_id INTEGER PRIMARY KEY CHECK(singleton_id=1),
    item_id TEXT NOT NULL REFERENCES owned_items(item_id) ON DELETE RESTRICT
);
-- Old sample routine numbers are intentionally isolated from the new routine IDs.
CREATE TABLE legacy_completions (
    hero_id TEXT NOT NULL,
    local_date TEXT NOT NULL,
    sample_id TEXT NOT NULL,
    PRIMARY KEY(hero_id,local_date,sample_id)
);
ALTER TABLE user_routines ADD COLUMN source TEXT NOT NULL DEFAULT 'PERSONAL' CHECK(source IN ('PERSONAL','HERO_DAY'));
DROP INDEX user_routine_active_template;
CREATE UNIQUE INDEX user_routine_active_template ON user_routines(hero_id,template_id,source) WHERE deleted=0;
CREATE INDEX user_routine_source ON user_routines(source,deleted,local_time);
-- Additive migration: existing adoption IDs, history, wallet and source settings are untouched.
CREATE TABLE quest_checks (
    routine_id INTEGER NOT NULL REFERENCES user_routines(id) ON DELETE RESTRICT,
    quest_id TEXT NOT NULL CHECK(length(quest_id) BETWEEN 1 AND 160),
    local_date TEXT NOT NULL CHECK(length(local_date)=10 AND local_date GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'),
    checked_at INTEGER NOT NULL CHECK(typeof(checked_at)='integer' AND checked_at>=0),
    PRIMARY KEY(routine_id,quest_id,local_date)
);
CREATE INDEX quest_checks_date ON quest_checks(local_date,routine_id);
-- Classes have no relationship to completion records or wallet credits.
CREATE TABLE timetable_entries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL CHECK(length(trim(title)) BETWEEN 1 AND 120),
    weekday INTEGER NOT NULL CHECK(typeof(weekday)='integer' AND weekday BETWEEN 1 AND 7),
    start_minute INTEGER NOT NULL CHECK(typeof(start_minute)='integer' AND start_minute BETWEEN 0 AND 1439),
    end_minute INTEGER NOT NULL CHECK(typeof(end_minute)='integer' AND end_minute BETWEEN 1 AND 1440 AND end_minute>start_minute),
    location TEXT NOT NULL DEFAULT '' CHECK(length(location)<=160),
    instructor TEXT NOT NULL DEFAULT '' CHECK(length(instructor)<=80),
    UNIQUE(title,weekday,start_minute,end_minute,location)
);
CREATE INDEX timetable_weekday_time ON timetable_entries(weekday,start_minute);
