CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE OR REPLACE FUNCTION update_updated_at() RETURNS trigger
    LANGUAGE plpgsql
AS
$$
BEGIN
    IF NEW IS DISTINCT FROM OLD AND NEW.updated_at IS NOT DISTINCT FROM OLD.updated_at
    THEN
        NEW.updated_at = NOW();
    END IF;
    RETURN NEW;
END
$$;

-- Create table
CREATE TABLE IF NOT EXISTS public.feature_flags
(
    id          uuid                              DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
    name        VARCHAR(255)             NOT NULL,
    code        VARCHAR(255)             NOT NULL UNIQUE,
    description TEXT,
    enabled     BOOLEAN                  NOT NULL DEFAULT FALSE,
    metadata    TEXT,
    type VARCHAR(50),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE,
    deleted_at  TIMESTAMP WITH TIME ZONE
);

DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'update_feature_flag_updated_at') THEN
            CREATE TRIGGER update_feature_flag_updated_at
                BEFORE UPDATE
                ON feature_flags
                FOR EACH ROW
            EXECUTE PROCEDURE update_updated_at();
        END IF;
    END
$$;
