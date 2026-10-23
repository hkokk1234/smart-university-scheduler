CREATE TABLE section_changes (
    id UUID PRIMARY KEY,
    section_id UUID NOT NULL,
    course_id UUID NOT NULL,
    course_code VARCHAR(32) NOT NULL,
    course_title VARCHAR(200) NOT NULL,
    actor_id UUID REFERENCES app_users(id) ON DELETE SET NULL,
    actor_name VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    before_snapshot TEXT NOT NULL,
    after_snapshot TEXT NOT NULL
);
CREATE INDEX idx_section_changes_history ON section_changes(section_id, created_at DESC, id);

CREATE TABLE user_notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    change_id UUID NOT NULL REFERENCES section_changes(id) ON DELETE CASCADE,
    read_at TIMESTAMPTZ,
    CONSTRAINT uq_notification_user_change UNIQUE(user_id, change_id)
);
CREATE INDEX idx_notifications_user ON user_notifications(user_id, read_at);
