-- Инициализация shared_services_db для интеграционных тестов
-- Создаёт схемы и функции для RLS

-- Extension для UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Создание схем для каждого сервиса
CREATE SCHEMA IF NOT EXISTS event_service;
CREATE SCHEMA IF NOT EXISTS notification_service;
CREATE SCHEMA IF NOT EXISTS media_service;

-- Функция для получения текущего tenant_id из session variable
-- Используется в RLS политиках для изоляции данных между организациями
CREATE OR REPLACE FUNCTION current_tenant_id() RETURNS UUID AS $$
BEGIN
    RETURN NULLIF(current_setting('app.tenant_id', true), '')::UUID;
EXCEPTION
    WHEN OTHERS THEN
        RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER STABLE;

COMMENT ON FUNCTION current_tenant_id() IS 'Возвращает tenant_id из session variable app.tenant_id для RLS политик';

-- Функция для получения текущего user_id из session variable
-- Используется в RLS политиках для доступа пользователей к своим данным
CREATE OR REPLACE FUNCTION current_user_id() RETURNS UUID AS $$
BEGIN
    RETURN NULLIF(current_setting('app.user_id', true), '')::UUID;
EXCEPTION
    WHEN OTHERS THEN
        RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER STABLE;

COMMENT ON FUNCTION current_user_id() IS 'Возвращает user_id из session variable app.user_id для RLS политик';

-- Функция для создания стандартных RLS политик на таблице
CREATE OR REPLACE FUNCTION create_tenant_rls_policies(
    schema_name TEXT,
    table_name TEXT
) RETURNS VOID AS $$
DECLARE
    full_table_name TEXT;
    policy_name TEXT;
BEGIN
    full_table_name := schema_name || '.' || table_name;
    policy_name := 'tenant_isolation_' || table_name;

    -- Включаем RLS на таблице
    EXECUTE format('ALTER TABLE %I.%I ENABLE ROW LEVEL SECURITY', schema_name, table_name);

    -- Политика для всех операций: доступ только к данным своего tenant
    EXECUTE format(
        'CREATE POLICY %I ON %I.%I FOR ALL USING (tenant_id = current_tenant_id()) WITH CHECK (tenant_id = current_tenant_id())',
        policy_name, schema_name, table_name
    );

    RAISE NOTICE 'RLS политика создана для таблицы %', full_table_name;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION create_tenant_rls_policies(TEXT, TEXT) IS 'Создаёт стандартные RLS политики для таблицы с tenant_id';

-- Роль для приложения (без superuser привилегий, подчиняется RLS)
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'aqstream_app') THEN
        CREATE ROLE aqstream_app WITH LOGIN PASSWORD 'aqstream_app';
    END IF;
END $$;

-- Права для роли приложения на схемы
GRANT USAGE ON SCHEMA event_service TO aqstream_app;
GRANT USAGE ON SCHEMA notification_service TO aqstream_app;
GRANT USAGE ON SCHEMA media_service TO aqstream_app;

-- Права по умолчанию для будущих таблиц
ALTER DEFAULT PRIVILEGES IN SCHEMA event_service GRANT ALL PRIVILEGES ON TABLES TO aqstream_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA notification_service GRANT ALL PRIVILEGES ON TABLES TO aqstream_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA media_service GRANT ALL PRIVILEGES ON TABLES TO aqstream_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA event_service GRANT ALL PRIVILEGES ON SEQUENCES TO aqstream_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA notification_service GRANT ALL PRIVILEGES ON SEQUENCES TO aqstream_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA media_service GRANT ALL PRIVILEGES ON SEQUENCES TO aqstream_app;
