--liquibase formatted sql

--changeset yvinogradov:create_command_account_id_index
CREATE INDEX  command_by_name  ON command (account_name);
CREATE INDEX  command_by_id  ON command (account_id);
CREATE INDEX  command_by_accountid_and_assetid  ON command (account_id, asset_id);
CREATE INDEX  command_by_domainid  ON command (domain_id);
