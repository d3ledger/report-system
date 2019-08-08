--liquibase formatted sql


--changeset bmingela:create_rates_table
CREATE TABLE rates (
	asset varchar(128) NOT NULL,
	link varchar(128) NULL,
	rate varchar(128) NULL,
	updated bigint NOT NULL,
	CONSTRAINT rates_pkey PRIMARY KEY (asset)
);
