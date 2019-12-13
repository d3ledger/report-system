--liquibase formatted sql

--changeset dolgopolov:create_eth_proof_table
CREATE TABLE eth_withdrawal_proof (
    id varchar(128) PRIMARY KEY,
    account_id_to_notify varchar(128) NOT NULL,
    tx_time bigint NOT NULL,
    amount numeric NOT NULL,
    relay varchar(128) NOT NULL,
    iroha_tx_hash varchar(128) NOT NULL,
    token_contract_address varchar(128) NOT NULL,
    dest_address varchar(128) NOT NULL,
    proofs_json text NOT NULL,
    block_num bigint NOT NULL,
    tx_index int NOT NULL
);
