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

--changeset yvinogradov:create_table_transfer_asset
CREATE TABLE transfer_asset
(
  id bigserial NOT NULL,
  type character varying(255),
  amount numeric(19,2),
  asset_id character varying(255),
  description character varying(255),
  dest_account_id character varying(255),
  src_account_id character varying(255),
  transaction_id bigint,
  CONSTRAINT transfer_asset_pkey PRIMARY KEY (id),
  CONSTRAINT fkovti4rff89omo1oiy300jhxm2 FOREIGN KEY (transaction_id)
      REFERENCES transaction (id)
)

--changeset yvinogradov:create_table_create_account
CREATE TABLE create_account
(
  id bigserial NOT NULL,
  type character varying(255),
  account_name character varying(255),
  domain_id character varying(255),
  public_key character varying(255),
  transaction_id bigint,
  CONSTRAINT create_account_pkey PRIMARY KEY (id),
  CONSTRAINT fkpj7ghlhtb9irsx8vlsv3mjg2v FOREIGN KEY (transaction_id)
      REFERENCES transaction (id)
)

--changeset yvinogradov:create_table_create_asset
CREATE TABLE create_asset (
	id bigserial NOT NULL,
	asset_name varchar(255) NULL,
	domain_id varchar(255) NULL,
	decimal_precision int4 NOT NULL,
	transaction_id int8 NULL,
	CONSTRAINT create_asset_pkey PRIMARY KEY (id),
	CONSTRAINT fkb2nstk6qj5w3prlw9ks80vm6 FOREIGN KEY (transaction_id) REFERENCES transaction(id)
);
