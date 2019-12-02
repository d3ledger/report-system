--liquibase formatted sql

--changeset dolgopolov:change_eth_proofs_table.sql
ALTER TABLE eth_withdrawal_proof RENAME COLUMN time TO tx_time;
ALTER TABLE eth_withdrawal_proof ADD COLUMN block_num bigint NOT NULL;
ALTER TABLE eth_withdrawal_proof ADD COLUMN tx_index int NOT NULL;
