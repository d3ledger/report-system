--liquibase formatted sql

--changeset bmingela:alter_billing_rename_accountid_to_domain
ALTER TABLE billing RENAME COLUMN account_id TO domain_name;