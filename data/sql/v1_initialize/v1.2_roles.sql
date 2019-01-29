-- CREATE USER 'no_admin'@'%'
--   IDENTIFIED BY 'no_admin';
-- CREATE USER 'no_app'@'%'
--   IDENTIFIED BY 'no_app';
--
-- GRANT USAGE ON hewiyoat_no_waiting.* TO 'no_app'@'%'
-- IDENTIFIED BY 'no_app';
-- GRANT USAGE ON hewiyoat_no_waiting.* TO 'no_app'@'localhost'
-- IDENTIFIED BY 'no_app';
-- GRANT INSERT, DELETE ON hewiyoat_no_waiting.* TO 'no_app'@'%'
-- IDENTIFIED BY 'no_app';
-- GRANT INSERT, DELETE ON hewiyoat_no_waiting.* TO 'no_app'@'localhost'
-- IDENTIFIED BY 'no_app';
-- GRANT SELECT, UPDATE ON hewiyoat_no_waiting.* TO 'no_app'@'%'
-- IDENTIFIED BY 'no_app';
-- GRANT SELECT, UPDATE ON hewiyoat_no_waiting.* TO 'no_app'@'localhost'
-- IDENTIFIED BY 'no_app';
--
-- GRANT USAGE ON hewiyoat_no_waiting.* TO 'no_admin'@'%'
-- IDENTIFIED BY 'no_admin';
-- GRANT USAGE ON hewiyoat_no_waiting.* TO 'no_admin'@'localhost'
-- IDENTIFIED BY 'no_admin';
-- GRANT ALL PRIVILEGES ON hewiyoat_no_waiting.* TO 'no_admin'@'%'
-- IDENTIFIED BY 'no_admin';
-- GRANT ALL PRIVILEGES ON hewiyoat_no_waiting.* TO 'no_admin'@'localhost'
-- IDENTIFIED BY 'no_admin';

DO $$
BEGIN
perform * FROM pg_roles r
WHERE r.rolname = 'no_admin';
IF NOT FOUND THEN
CREATE ROLE no_admin WITH PASSWORD 'no_admin';
END IF;
END;
$$ LANGUAGE plpgsql;

DO $$
BEGIN
perform * FROM pg_roles r
WHERE r.rolname = 'no_app';
IF NOT FOUND THEN
CREATE ROLE no_app WITH PASSWORD 'no_app';
END IF;
END;
$$ LANGUAGE plpgsql;


GRANT ALL PRIVILEGES ON DATABASE documents TO no_app;

GRANT EXECUTE ON FUNCTION public.uuid_generate_v4() TO no_app;

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA no_waiting TO no_app;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA no_waiting TO no_app;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA no_waiting TO no_app;
