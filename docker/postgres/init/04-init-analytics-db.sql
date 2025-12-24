-- Инициализация analytics_service_db
-- Используется: Analytics Service (TimescaleDB для метрик и аналитики)

-- Extension для UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- TimescaleDB extension
CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;

-- Создание схемы
CREATE SCHEMA IF NOT EXISTS analytics_service;

-- Настройка search_path для базы
ALTER DATABASE analytics_service_db SET search_path TO public, analytics_service;

-- Комментарий к схеме
COMMENT ON SCHEMA analytics_service IS 'Analytics Service - метрики, события, time-series данные';

-- ========================================
-- Функции для Row Level Security (RLS)
-- ========================================

-- Функция для получения текущего tenant_id из session variable
CREATE OR REPLACE FUNCTION current_tenant_id() RETURNS UUID AS $$
BEGIN
    RETURN NULLIF(current_setting('app.tenant_id', true), '')::UUID;
EXCEPTION
    WHEN OTHERS THEN
        RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER STABLE;

COMMENT ON FUNCTION current_tenant_id() IS 'Возвращает tenant_id из session variable app.tenant_id для RLS политик';

-- Функция для создания стандартных RLS политик
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

    EXECUTE format('ALTER TABLE %I.%I ENABLE ROW LEVEL SECURITY', schema_name, table_name);
    EXECUTE format(
        'CREATE POLICY %I ON %I.%I FOR ALL USING (tenant_id = current_tenant_id()) WITH CHECK (tenant_id = current_tenant_id())',
        policy_name, schema_name, table_name
    );

    RAISE NOTICE 'RLS политика создана для таблицы %', full_table_name;
END;
$$ LANGUAGE plpgsql;

-- Роль для приложения (без superuser привилегий, подчиняется RLS)
-- ВАЖНО: В production использовать безопасный пароль через environment variable
-- или создавать роль через отдельный защищённый скрипт
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'aqstream_app') THEN
        CREATE ROLE aqstream_app WITH LOGIN PASSWORD 'aqstream_app';
    END IF;
END $$;

-- Права для роли приложения
GRANT USAGE ON SCHEMA analytics_service TO aqstream_app;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA analytics_service TO aqstream_app;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA analytics_service TO aqstream_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA analytics_service GRANT ALL PRIVILEGES ON TABLES TO aqstream_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA analytics_service GRANT ALL PRIVILEGES ON SEQUENCES TO aqstream_app;
