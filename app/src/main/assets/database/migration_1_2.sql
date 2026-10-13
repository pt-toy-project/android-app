ALTER TABLE user_routines ADD COLUMN source TEXT NOT NULL DEFAULT 'PERSONAL' CHECK(source IN ('PERSONAL','HERO_DAY'));
DROP INDEX user_routine_active_template;
CREATE UNIQUE INDEX user_routine_active_template ON user_routines(hero_id,template_id,source) WHERE deleted=0;
CREATE INDEX user_routine_source ON user_routines(source,deleted,local_time);
