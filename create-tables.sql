-- Script para criar tabelas no RDS PostgreSQL

CREATE TABLE IF NOT EXISTS feedbacks (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    description VARCHAR(500) NOT NULL,
    score INTEGER NOT NULL,
    urgency_level VARCHAR(10) NOT NULL
);

CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    sent_at TIMESTAMP NOT NULL,
    description VARCHAR(500) NOT NULL,
    urgency_level VARCHAR(10) NOT NULL
);

CREATE TABLE IF NOT EXISTS weekly_reports (
    id BIGSERIAL PRIMARY KEY,
    generated_at TIMESTAMP NOT NULL,
    average_score DOUBLE PRECISION NOT NULL,
    total_feedbacks BIGINT NOT NULL,
    high_urgency_count BIGINT NOT NULL,
    medium_urgency_count BIGINT NOT NULL,
    low_urgency_count BIGINT NOT NULL
);

-- Verificar tabelas criadas
SELECT table_name FROM information_schema.tables WHERE table_schema = 'public';
