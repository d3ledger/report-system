--liquibase formatted sql


--changeset yvinogradov:create_schema_iroha
CREATE SCHEMA iroha

--changeset yvinogradov:create_table_block
CREATE TABLE iroha.block
(
  block_number bigint NOT NULL,
  block_creation_time bigint,
  CONSTRAINT block_pkey PRIMARY KEY (block_number)
)

--changeset yvinogradov:create_table_transaction
CREATE TABLE iroha.transaction (
	id bigserial NOT NULL,
	creator_id varchar(255) NULL,
	quorum int4 NULL,
	rejected bool NOT NULL,
	block_number int8 NULL,
	CONSTRAINT transaction_pkey PRIMARY KEY (id),
	CONSTRAINT fk2bc78pqajc8yuds2kh88vi0ic FOREIGN KEY (block_number) REFERENCES iroha.block(block_number)
);

--changeset yvinogradov:create_table_transfer_asset
CREATE TABLE iroha.transfer_asset
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
      REFERENCES iroha.transaction (id)
)

--changeset yvinogradov:create_table_create_account
CREATE TABLE iroha.create_account
(
  id bigserial NOT NULL,
  type character varying(255),
  account_name character varying(255),
  domain_id character varying(255),
  public_key character varying(255),
  transaction_id bigint,
  CONSTRAINT create_account_pkey PRIMARY KEY (id),
  CONSTRAINT fkpj7ghlhtb9irsx8vlsv3mjg2v FOREIGN KEY (transaction_id)
      REFERENCES iroha.transaction (id)
)
