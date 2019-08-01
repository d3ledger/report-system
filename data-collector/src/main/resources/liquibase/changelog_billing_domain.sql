--liquibase formatted sql

--changeset bmingela:alter_billing_rename_accountid_to_domain
ALTER TABLE billing CHANGE account_id domain_name varchar(128) COMMENT 'Client domain name'