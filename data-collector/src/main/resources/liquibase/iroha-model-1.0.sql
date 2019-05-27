--liquibase formatted sql

--changeset yvinogradov:create_table_block
CREATE TABLE block
(
  block_number bigint NOT NULL,
  block_creation_time bigint,
  CONSTRAINT block_pkey PRIMARY KEY (block_number)
)

--changeset yvinogradov:create_table_transactionBatch
CREATE TABLE public.transaction_batch (
	id bigserial NOT NULL,
	batch_type varchar(128) NULL,
	CONSTRAINT transaction_batch_pkey PRIMARY KEY (id)
);


--changeset yvinogradov:create_table_transaction
CREATE TABLE transaction (
	id bigserial NOT NULL,
	creator_id varchar(255) NULL,
	quorum int4 NULL,
	rejected bool NOT NULL,
	batch_id bigint NULL,
	block_number bigint NULL,
	CONSTRAINT transaction_pkey PRIMARY KEY (id),
	CONSTRAINT fk2bc78pqajc8yuds2kh88vi0ic FOREIGN KEY (block_number) REFERENCES block(block_number),
	CONSTRAINT fkkv0fdmcpe8erod3aho90kiemy FOREIGN KEY (batch_id) REFERENCES transaction_batch(id)
);

--changeset yvinogradov:create_table_command
CREATE TABLE public.command (
	dtype varchar(32) NOT NULL,
	id bigserial NOT NULL,
	account_id varchar(255) NULL,
	detail_key varchar(255) NULL,
	detail_value varchar(10485760) NULL,
	public_key varchar(255) NULL,
	amount numeric NULL,
	asset_id varchar(255) NULL,
	description varchar(8000) NULL,
	dest_account_id varchar(255) NULL,
	src_account_id varchar(255) NULL,
	quorum int4 NULL,
	asset_name varchar(255) NULL,
	decimal_precision int4 NULL,
	domain_id varchar(255) NULL,
	account_name varchar(255) NULL,
	transaction_id bigint NULL,
	CONSTRAINT command_pkey PRIMARY KEY (id),
	CONSTRAINT fkc90i3gykxnwq7xqlwttfnquya FOREIGN KEY (transaction_id) REFERENCES transaction(id)
);

