-- Drop the trigger if it exists
DO
$$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'update_feature_flag_updated_at') THEN
        DROP TRIGGER update_feature_flag_updated_at ON feature_flags;
    END IF;
END
$$;

-- Drop the function if it exists
DROP FUNCTION IF EXISTS update_updated_at();

-- Drop the table if it exists
DROP TABLE IF EXISTS public.feature_flags;

-- Optionally drop the uuid-ossp extension if not used elsewhere
DROP EXTENSION IF EXISTS "uuid-ossp";
