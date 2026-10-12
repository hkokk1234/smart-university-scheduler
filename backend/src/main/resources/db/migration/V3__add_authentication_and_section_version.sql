ALTER TABLE app_users ADD COLUMN password_hash VARCHAR(100);
ALTER TABLE app_users ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'STUDENT';
ALTER TABLE app_users ADD CONSTRAINT ck_user_role CHECK (role IN ('STUDENT', 'ADMIN'));
ALTER TABLE app_users ADD CONSTRAINT ck_user_password_hash
    CHECK (password_hash IS NULL OR password_hash ~ '^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$');
-- Existing catalog fixtures have no password and cannot authenticate.
ALTER TABLE sections ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE sections ADD CONSTRAINT ck_section_version CHECK (version >=0);
