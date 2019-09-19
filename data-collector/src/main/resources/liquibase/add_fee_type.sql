--liquibase formatted sql

--changeset bmingela:alter_billing_create_fee_type
ALTER TABLE billing ADD COLUMN fee_type varchar(64) NOT NULL;