--liquibase formatted sql


--changeset bmingela:alter_rates_table_link_column
ALTER TABLE rates ALTER COLUMN link TYPE VARCHAR(256);
