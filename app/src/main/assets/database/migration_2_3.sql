-- Additive migration: existing adoption IDs, history, wallet and source settings are untouched.
CREATE TABLE quest_checks (
    routine_id INTEGER NOT NULL REFERENCES user_routines(id) ON DELETE RESTRICT,
    quest_id TEXT NOT NULL CHECK(length(quest_id) BETWEEN 1 AND 160),
    local_date TEXT NOT NULL CHECK(length(local_date)=10 AND local_date GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'),
    checked_at INTEGER NOT NULL CHECK(typeof(checked_at)='integer' AND checked_at>=0),
    PRIMARY KEY(routine_id,quest_id,local_date)
);
CREATE INDEX quest_checks_date ON quest_checks(local_date,routine_id);
