-- Инициализация payment_service_db
-- Используется: Payment Service (dedicated database для PCI DSS compliance)

-- Создание схемы
CREATE SCHEMA IF NOT EXISTS payment_service;

-- Настройка search_path для базы
ALTER DATABASE payment_service_db SET search_path TO public, payment_service;

-- Комментарий к схеме
COMMENT ON SCHEMA payment_service IS 'Payment Service - платежи, подписки, финансовые операции';

-- Extension для UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
