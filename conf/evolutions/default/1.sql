# --- !Ups

create table "meta_open_data_club" ("id" SERIAL NOT NULL PRIMARY KEY, "last_data_update" timestamptz, "created_at" timestamptz not null default now());
insert into meta_open_data_club (last_data_update) values ('2015/06/14 11:24');

# --- !Downs

drop table "meta_open_data_club";