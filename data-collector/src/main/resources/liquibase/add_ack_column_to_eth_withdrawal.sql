--liquibase formatted sql

--changeset dolgopolov:add_ack_to_eth_withdrawal
ALTER TABLE eth_withdrawal_proof ADD COLUMN ack BOOLEAN
