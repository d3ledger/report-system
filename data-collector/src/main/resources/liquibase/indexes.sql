--liquibase formatted sql

--changeset yvinogradov:create_command_account_id_index
CREATE INDEX  account_by_id  ON command (account_name);
