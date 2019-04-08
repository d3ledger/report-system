--liquibase formatted sql

--changeset yvinogradov:alter_databasechange_lock_create_id
ALTER TABLE DATABASECHANGELOG ADD COLUMN primary_key BIGINT PRIMARY KEY AUTO_INCREMENT;