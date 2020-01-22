--liquibase formatted sql

--changeset bmingela:alter_billing_redesign_add_fee_description
ALTER TABLE billing ADD COLUMN fee_description varchar(64) NOT NULL;

--changeset bmingela:alter_billing_redesign_add_destination
ALTER TABLE billing ADD COLUMN destination varchar(128) NOT NULL;

--changeset bmingela:alter_billing_redesign_add_fee_nature
ALTER TABLE billing ADD COLUMN fee_nature varchar(32) NOT NULL;

--changeset bmingela:alter_billing_redesign_add_fee_computation
ALTER TABLE billing ADD COLUMN fee_computation varchar(32) NOT NULL;

--changeset bmingela:alter_billing_redesign_add_fee_account
ALTER TABLE billing ADD COLUMN fee_account varchar(128) NULL;

--changeset bmingela:alter_billing_redesign_add_min_amount
ALTER TABLE billing ADD COLUMN min_amount numeric(18,8) NOT NULL;

--changeset bmingela:alter_billing_redesign_add_max_amount
ALTER TABLE billing ADD COLUMN max_amount numeric(18,8) NOT NULL;

--changeset bmingela:alter_billing_redesign_add_min_fee
ALTER TABLE billing ADD COLUMN min_fee numeric(18,8) NOT NULL;

--changeset bmingela:alter_billing_redesign_add_max_fee
ALTER TABLE billing ADD COLUMN max_fee numeric(18,8) NOT NULL;

--changeset bmingela:alter_billing_redesign_change_fee_fraction_type
ALTER TABLE billing ALTER COLUMN fee_fraction TYPE numeric(18,8);