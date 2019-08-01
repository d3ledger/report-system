--liquibase formatted sql


--changeset yvinogradov:create_state_table
CREATE TABLE state (
	id bigserial NOT NULL,
	title varchar(128) NOT NULL,
	value varchar(128) NOT NULL,
	CONSTRAINT state_pkey PRIMARY KEY (id)
);

INSERT INTO state VALUES
    (0,'SEEN', '0');

INSERT INTO state VALUES
    (1,'PROCESSED', '0');

--changeset yvinogradov:create_billing_table
CREATE TABLE billing (
	id bigserial NOT NULL,
	billing_type varchar(64) NULL,
	domain_name varchar(128) NULL,
	asset varchar(128) NULL,
	fee_fraction numeric(8,6) NULL,
	created bigint NOT NULL,
	updated bigint NOT NULL,
	CONSTRAINT billing_pkey PRIMARY KEY (id)
);

--changeset yvinogradov:create_table_account_asset_custody_context
CREATE TABLE public.account_asset_custody_context
(
    id bigserial NOT NULL,
    account_id character varying(128),
    asset_id character varying(128),
    commulative_fee_amount numeric(19,10),
    last_asset_sum numeric(21,10),
    last_transfer_timestamp bigint,
    CONSTRAINT account_asset_custody_context_pkey PRIMARY KEY (id)
)

