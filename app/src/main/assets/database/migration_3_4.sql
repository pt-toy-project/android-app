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
