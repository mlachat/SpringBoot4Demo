-- Schema DDL for ElstarData entity
CREATE TABLE IF NOT EXISTS elstar_daten (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    xml_nachricht TEXT,
    creation_date DATE,
    status INTEGER
);
