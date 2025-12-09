-- Инициализация shared_services_db
-- Используется: Event Service, Notification Service, Media Service

-- Создание схем для каждого сервиса
CREATE SCHEMA IF NOT EXISTS event_service;
CREATE SCHEMA IF NOT EXISTS notification_service;
CREATE SCHEMA IF NOT EXISTS media_service;

-- Настройка search_path для базы
ALTER DATABASE shared_services_db SET search_path TO public, event_service, notification_service, media_service;

-- Комментарии к схемам
COMMENT ON SCHEMA event_service IS 'Event Service - события, регистрации, билеты';
COMMENT ON SCHEMA notification_service IS 'Notification Service - уведомления, email, telegram';
COMMENT ON SCHEMA media_service IS 'Media Service - файлы, изображения';

-- Extension для UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
