--liquibase formatted sql


--changeset yvinogradov:create_schema_iroha
CREATE SCHEMA iroha

--changeset yvinogradov:create_table_block
CREATE TABLE iroha.block
(
  block_number bigint NOT NULL,
  "timestamp" bigint,
  CONSTRAINT block_pkey PRIMARY KEY (block_number)
)

--changeset yvinogradov:create_table_transaction
CREATE TABLE iroha.transaction
(
  id bigserial NOT NULL,
  creator_id character varying(255),
  quorum integer,
  block_number bigint,
  CONSTRAINT transaction_pkey PRIMARY KEY (id),
  CONSTRAINT fk2bc78pqajc8yuds2kh88vi0ic FOREIGN KEY (block_number)
      REFERENCES iroha.block (block_number) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)

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
      REFERENCES iroha.transaction (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
