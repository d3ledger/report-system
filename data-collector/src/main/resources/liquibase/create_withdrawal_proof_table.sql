--liquibase formatted sql

--changeset dolgopolov:create_eth_proof_table
CREATE TABLE eth_withdrawal_proof (
    id varchar(128) PRIMARY KEY,
    account_id_to_notify varchar(128) NOT NULL,
    time bigint NOT NULL,
    amount varchar(128) NOT NULL,
    relay varchar(128) NOT NULL,
    iroha_tx_hash varchar(128) NOT NULL,
    token_contract_address varchar(128) NOT NULL,
    dest_address varchar(128) NOT NULL,
    proofs_json text NOT NULL
);
