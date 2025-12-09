-- Инициализация user_service_db
-- Используется: User Service (dedicated database)

-- Создание схемы
CREATE SCHEMA IF NOT EXISTS user_service;

-- Настройка search_path для базы
ALTER DATABASE user_service_db SET search_path TO public, user_service;

-- Комментарий к схеме
COMMENT ON SCHEMA user_service IS 'User Service - пользователи, организации, аутентификация';

-- Extension для UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
