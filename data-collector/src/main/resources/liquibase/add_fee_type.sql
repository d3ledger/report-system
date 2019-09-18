--liquibase formatted sql

ALTER TABLE billing ADD COLUMN fee_type varchar(64) NOT NULL;