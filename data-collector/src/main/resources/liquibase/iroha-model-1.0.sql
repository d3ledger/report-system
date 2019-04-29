--liquibase formatted sql

--changeset yvinogradov:create_table_block
CREATE TABLE block
(
  block_number bigint NOT NULL,
  block_creation_time bigint,
  CONSTRAINT block_pkey PRIMARY KEY (block_number)
)

--changeset yvinogradov:create_table_transaction
CREATE TABLE transaction (
	id bigserial NOT NULL,
	creator_id varchar(255) NULL,
	quorum int4 NULL,
	rejected bool NOT NULL,
	block_number int8 NULL,
	CONSTRAINT transaction_pkey PRIMARY KEY (id),
	CONSTRAINT fk2bc78pqajc8yuds2kh88vi0ic FOREIGN KEY (block_number) REFERENCES block(block_number)
);

--changeset yvinogradov:create_table_command
CREATE TABLE public.command (
	dtype varchar(31) NOT NULL,
	id bigserial NOT NULL,
	account_id varchar(255) NULL,
	detail_key varchar(255) NULL,
	detail_value varchar(255) NULL,
	amount numeric(19,2) NULL,
	asset_id varchar(255) NULL,
	description varchar(255) NULL,
	dest_account_id varchar(255) NULL,
	src_account_id varchar(255) NULL,
	asset_name varchar(255) NULL,
	decimal_precision int4 NULL,
	domain_id varchar(255) NULL,
	account_name varchar(255) NULL,
	public_key varchar(255) NULL,
	transaction_id int8 NULL,
	CONSTRAINT command_pkey PRIMARY KEY (id),
	CONSTRAINT fkc90i3gykxnwq7xqlwttfnquya FOREIGN KEY (transaction_id) REFERENCES transaction(id)
);


