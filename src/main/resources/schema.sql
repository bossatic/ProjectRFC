-- H2 Database Schema for IDoc Monitoring

-- Main events table
CREATE TABLE IF NOT EXISTS idoc_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL UNIQUE,
    timestamp TIMESTAMP NOT NULL,
    event_type VARCHAR(50) NOT NULL,

    -- IDoc identification
    idoc_type VARCHAR(50),
    doc_num VARCHAR(50),
    tid VARCHAR(100),

    -- Processing details
    stage VARCHAR(50),
    status VARCHAR(20),
    processing_time_ms INTEGER,

    -- Sizes
    xml_size_bytes BIGINT,
    json_size_bytes BIGINT,

    -- Kafka details
    kafka_topic VARCHAR(100),
    kafka_partition INTEGER,
    kafka_offset BIGINT,

    -- Error details
    error_message VARCHAR(2000),
    error_stage VARCHAR(50),
    is_recoverable BOOLEAN,

    -- Source
    source_system VARCHAR(50),

    -- Full payload
    payload_json CLOB
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_timestamp ON idoc_events(timestamp);
CREATE INDEX IF NOT EXISTS idx_event_type ON idoc_events(event_type);
CREATE INDEX IF NOT EXISTS idx_idoc_type ON idoc_events(idoc_type);
CREATE INDEX IF NOT EXISTS idx_doc_num ON idoc_events(doc_num);
CREATE INDEX IF NOT EXISTS idx_status ON idoc_events(status);
CREATE INDEX IF NOT EXISTS idx_timestamp_type ON idoc_events(timestamp, event_type);

-- Hourly aggregates table
CREATE TABLE IF NOT EXISTS idoc_hourly_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stat_hour TIMESTAMP NOT NULL,
    idoc_type VARCHAR(50) NOT NULL,

    total_received INTEGER DEFAULT 0,
    total_processed INTEGER DEFAULT 0,
    total_kafka_published INTEGER DEFAULT 0,
    total_errors INTEGER DEFAULT 0,

    avg_processing_time_ms INTEGER,
    total_xml_bytes BIGINT,
    total_json_bytes BIGINT,

    CONSTRAINT unique_hour_type UNIQUE (stat_hour, idoc_type)
);

CREATE INDEX IF NOT EXISTS idx_stat_hour ON idoc_hourly_stats(stat_hour);

-- Application metadata table
CREATE TABLE IF NOT EXISTS app_metadata (
    metadata_key VARCHAR(100) PRIMARY KEY,
    metadata_value VARCHAR(500),
    updated_at TIMESTAMP
);
