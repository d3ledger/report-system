--liquibase formatted sql


--changeset yvinogradov:create_state_table
CREATE TABLE state (
	id serial NOT NULL,
	title varchar(255) NOT NULL,
	value varchar(255) NOT NULL,
	CONSTRAINT state_pkey PRIMARY KEY (id)
);

INSERT INTO state VALUES
    (0,'LAST_BLOCK', '0');


