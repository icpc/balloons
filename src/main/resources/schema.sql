create table if not exists volunteer (
    id bigserial not null primary key,
    login varchar(255) not null unique,
    password_hash varchar(255) not null,
    can_access boolean not null default false,
    can_manage boolean not null default false
);

create table if not exists balloon (
    id bigserial not null primary key,
    problem_id varchar(255) not null,
    team_id varchar(255) not null,
    volunteer_id bigint references volunteer (id),
    delivered boolean not null default false,
    unique (problem_id, team_id)
);

create table if not exists secret_key (
    data binary(32) not null
);