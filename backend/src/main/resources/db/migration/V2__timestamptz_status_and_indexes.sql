-- Server-generated and form timestamps become timezone-aware. Existing values were
-- stored as naive local times; ${legacy_timezone} (LEGACY_DATA_TIMEZONE, default UTC)
-- is the zone those values were written in. Each conversion is conditional because
-- some databases were updated from sql/schema.sql before Flyway was introduced.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'users'
          AND column_name = 'created_at'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE users
            ALTER COLUMN created_at TYPE TIMESTAMPTZ
            USING created_at AT TIME ZONE '${legacy_timezone}';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'sites'
          AND column_name = 'created_at'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE sites
            ALTER COLUMN created_at TYPE TIMESTAMPTZ
            USING created_at AT TIME ZONE '${legacy_timezone}';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'safety_forms'
          AND column_name = 'form_date'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE safety_forms
            ALTER COLUMN form_date TYPE TIMESTAMPTZ
            USING form_date AT TIME ZONE '${legacy_timezone}';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'safety_forms'
          AND column_name = 'submitted_at'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE safety_forms
            ALTER COLUMN submitted_at TYPE TIMESTAMPTZ
            USING submitted_at AT TIME ZONE '${legacy_timezone}';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'photos'
          AND column_name = 'created_at'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE photos
            ALTER COLUMN created_at TYPE TIMESTAMPTZ
            USING created_at AT TIME ZONE '${legacy_timezone}';
    END IF;
END $$;

-- Submission review status. These guards also support databases created directly
-- from the current sql/schema.sql snapshot.
ALTER TABLE safety_forms
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'submitted';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'safety_forms'::regclass
          AND conname = 'chk_safety_forms_status'
    ) THEN
        ALTER TABLE safety_forms
            ADD CONSTRAINT chk_safety_forms_status
            CHECK (status IN ('submitted', 'reviewed'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_safety_forms_user_id ON safety_forms (user_id);
CREATE INDEX IF NOT EXISTS idx_safety_forms_site_id ON safety_forms (site_id);
CREATE INDEX IF NOT EXISTS idx_safety_forms_form_date ON safety_forms (form_date);
CREATE INDEX IF NOT EXISTS idx_photos_safety_form_id ON photos (safety_form_id);
CREATE INDEX IF NOT EXISTS idx_sites_safety_checklist_id ON sites (safety_checklist_id);
