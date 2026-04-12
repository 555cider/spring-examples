create schema if not exists my_schema;

drop table if exists my_schema.client;

create table if not exists my_schema.oauth2_registered_client (
    id varchar(100) primary key,
    client_id varchar(100) not null unique,
    client_id_issued_at timestamp with time zone not null default current_timestamp,
    client_secret varchar(200) default null,
    client_secret_expires_at timestamp with time zone default null,
    client_name varchar(200) not null,
    client_authentication_methods varchar(1000) not null,
    authorization_grant_types varchar(1000) not null,
    redirect_uris varchar(1000) default null,
    post_logout_redirect_uris varchar(1000) default null,
    scopes varchar(1000) not null,
    client_settings varchar(2000) not null,
    token_settings varchar(2000) not null
);
