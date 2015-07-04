# --- !Ups

create table "sources" ("id" SERIAL NOT NULL PRIMARY KEY, name text not null, url text, "created_at" timestamptz not null default now(), "updated_at" timestamptz not null default now());
insert into sources (id, name, url) values (-1, 'INE', 'http://www.ine.es/');

create table "external_data_sources" ("id" SERIAL NOT NULL PRIMARY KEY, "source_id" integer not null, "name" text not null, "description" text, url text, download_url text, "created_at" timestamptz not null default now(), "updated_at" timestamptz not null default now());
alter table "external_data_sources" add constraint "external_data_sources_source_fk" foreign key (source_id) references sources(id); 
insert into external_data_sources (id, source_id, name, description, url, download_url) values (-1, -1, 'EPA por sexo y rango de edad', 'Ocupados por sexo y grupo de edad. Valores absolutos y porcentajes respecto del total de cada sexo', 'http://www.ine.es/dynt3/inebase/es/index.htm?padre=982&capsel=985', 'http://www.ine.es/jaxiT3/files/es/4076c.csv?t=4076&nocab=1');

create table "data_imports" ("id" SERIAL NOT NULL PRIMARY KEY, "external_data_id" integer not null, "created_at" timestamptz not null default now(), content jsonb);
alter table "data_imports" add constraint "data_imports_external_data_fk" foreign key (external_data_id) references external_data_sources(id);

# --- !Downs

drop table "data_imports";
drop table "external_data_sources";
drop table "sources";