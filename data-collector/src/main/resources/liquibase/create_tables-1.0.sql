--liquibase formatted sql


--changeset yvinogradov:create_state_table
CREATE TABLE state (
	id bigserial NOT NULL,
	title varchar(128) NOT NULL,
	value varchar(128) NOT NULL,
	CONSTRAINT state_pkey PRIMARY KEY (id)
);

INSERT INTO state VALUES
    (0,'LAST_BLOCK', '0');

INSERT INTO state VALUES
    (1,'IROHA_REQUEST_COUNTER', '0');

--changeset yvinogradov:create_billing_table
CREATE TABLE billing (
	id bigserial NOT NULL,
	billing_type varchar(64) NULL,
	account_id varchar(128) NULL,
	asset varchar(128) NULL,
	fee_fraction numeric(8,6) NULL,
	created bigint NOT NULL,
	updated bigint NULL,
	CONSTRAINT billing_pkey PRIMARY KEY (id)
);


