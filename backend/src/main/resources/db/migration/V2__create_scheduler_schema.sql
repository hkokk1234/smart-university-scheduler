CREATE TABLE app_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(254) NOT NULL UNIQUE,
    display_name VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_user_email_normalized CHECK (email =lower(btrim(email)) AND email <> ''),
    CONSTRAINT ck_user_display_name CHECK (btrim(display_name) <> '')
);

CREATE TABLE semesters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL,
    time_zone VARCHAR(64) NOT NULL,
    CONSTRAINT ck_semester_dates CHECK (starts_on < ends_on),
    CONSTRAINT ck_semester_text CHECK (btrim(code) <> '' AND btrim(name) <> '' AND btrim(time_zone) <> '')
);

CREATE TABLE instructors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    staff_code VARCHAR(32) NOT NULL UNIQUE,
    display_name VARCHAR(120) NOT NULL,
    email VARCHAR(254) UNIQUE,
    CONSTRAINT ck_instructor_text CHECK (btrim(staff_code) <> '' AND btrim(display_name) <> ''),
    CONSTRAINT ck_instructor_email_normalized CHECK (email IS NULL OR (email =lower(btrim(email)) AND email <> ''))
);

CREATE TABLE rooms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    building VARCHAR(120) NOT NULL,
    code VARCHAR(32) NOT NULL,
    capacity INTEGER NOT NULL,
    CONSTRAINT uq_room_building_code UNIQUE (building, code),
    CONSTRAINT ck_room_capacity CHECK (capacity > 0),
    CONSTRAINT ck_room_text CHECK (btrim(building) <> '' AND btrim(code) <> '')
);

CREATE TABLE courses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    semester_id UUID NOT NULL REFERENCES semesters(id) ON DELETE RESTRICT,
    code VARCHAR(32) NOT NULL,
    title VARCHAR(200) NOT NULL,
    credits NUMERIC(4,1) NOT NULL,
    CONSTRAINT uq_course_semester_code UNIQUE (semester_id, code),
    CONSTRAINT uq_course_id_semester UNIQUE (id, semester_id),
    CONSTRAINT ck_course_credits CHECK (credits > 0 AND credits <=60),
    CONSTRAINT ck_course_text CHECK (btrim(code) <> '' AND btrim(title) <> '')
);

CREATE TABLE section_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id UUID NOT NULL,
    semester_id UUID NOT NULL,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    type VARCHAR(16) NOT NULL,
    CONSTRAINT fk_group_course_semester FOREIGN KEY (course_id, semester_id)
        REFERENCES courses(id, semester_id) ON DELETE RESTRICT,
    CONSTRAINT uq_group_course_code UNIQUE (course_id, code),
    CONSTRAINT uq_group_id_semester UNIQUE (id, semester_id),
    CONSTRAINT ck_group_type CHECK (type IN ('LECTURE', 'LABORATORY', 'TUTORIAL', 'SEMINAR')),
    CONSTRAINT ck_group_text CHECK (btrim(code) <> '' AND btrim(name) <> '')
);
CREATE INDEX idx_group_semester ON section_groups(semester_id);

CREATE TABLE sections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    section_group_id UUID NOT NULL,
    semester_id UUID NOT NULL,
    code VARCHAR(32) NOT NULL,
    capacity INTEGER NOT NULL,
    CONSTRAINT fk_section_group_semester FOREIGN KEY (section_group_id, semester_id)
        REFERENCES section_groups(id, semester_id) ON DELETE RESTRICT,
    CONSTRAINT uq_section_group_code UNIQUE (section_group_id, code),
    CONSTRAINT uq_section_id_group_semester UNIQUE (id, section_group_id, semester_id),
    CONSTRAINT ck_section_capacity CHECK (capacity > 0),
    CONSTRAINT ck_section_code CHECK (btrim(code) <> '')
);
CREATE INDEX idx_section_semester ON sections(semester_id);

CREATE TABLE meetings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    section_id UUID NOT NULL REFERENCES sections(id) ON DELETE CASCADE,
    weekday INTEGER NOT NULL,
    start_time TIME WITHOUT TIME ZONE NOT NULL,
    end_time TIME WITHOUT TIME ZONE NOT NULL,
    instructor_id UUID REFERENCES instructors(id) ON DELETE RESTRICT,
    room_id UUID REFERENCES rooms(id) ON DELETE RESTRICT,
    CONSTRAINT ck_meeting_weekday CHECK (weekday BETWEEN 1 AND 7),
    CONSTRAINT ck_meeting_times CHECK (start_time < end_time AND end_time < TIME '24:00:00'),
    CONSTRAINT uq_meeting_slot UNIQUE (section_id, weekday, start_time, end_time)
);
CREATE INDEX idx_meeting_instructor ON meetings(instructor_id) WHERE instructor_id IS NOT NULL;
CREATE INDEX idx_meeting_room ON meetings(room_id) WHERE room_id IS NOT NULL;

CREATE TABLE saved_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    semester_id UUID NOT NULL REFERENCES semesters(id) ON DELETE RESTRICT,
    name VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_schedule_user_semester_name UNIQUE (user_id, semester_id, name),
    CONSTRAINT uq_schedule_id_semester UNIQUE (id, semester_id),
    CONSTRAINT ck_schedule_name CHECK (btrim(name) <> '')
);
CREATE INDEX idx_schedule_semester ON saved_schedules(semester_id);

CREATE TABLE schedule_sections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    saved_schedule_id UUID NOT NULL,
    section_id UUID NOT NULL,
    section_group_id UUID NOT NULL,
    semester_id UUID NOT NULL,
    CONSTRAINT fk_selection_schedule_semester FOREIGN KEY (saved_schedule_id, semester_id)
        REFERENCES saved_schedules(id, semester_id) ON DELETE CASCADE,
    CONSTRAINT fk_selection_section_group_semester FOREIGN KEY (section_id, section_group_id, semester_id)
        REFERENCES sections(id, section_group_id, semester_id) ON DELETE RESTRICT,
    CONSTRAINT uq_selection_schedule_group UNIQUE (saved_schedule_id, section_group_id)
);
CREATE INDEX idx_selection_section ON schedule_sections(section_id);

-- Deferred so a section and its meetings can be inserted in the same transaction.
-- Lock the parent to serialize concurrent attempts to remove its remaining meetings.
CREATE FUNCTION enforce_section_has_meeting() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    target_section UUID;
BEGIN
    IF TG_TABLE_NAME ='sections' THEN
        target_section :=NEW.id;
    ELSE
        target_section :=OLD.section_id;
    END IF;

    PERFORM 1 FROM sections WHERE id =target_section FOR UPDATE;
    IF FOUND AND NOT EXISTS (SELECT 1 FROM meetings WHERE section_id =target_section) THEN
        RAISE EXCEPTION 'Section % must contain at least one meeting', target_section
            USING ERRCODE ='23514', CONSTRAINT ='ck_section_has_meeting';
    END IF;
    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER ck_section_has_meeting_on_section
    AFTER INSERT OR UPDATE ON sections DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION enforce_section_has_meeting();

CREATE CONSTRAINT TRIGGER ck_section_has_meeting_on_meeting
    AFTER DELETE OR UPDATE ON meetings DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION enforce_section_has_meeting();
