# --- !Ups

create table "data_tables" ("id" SERIAL NOT NULL PRIMARY KEY, "data_import_id" integer not null, "schema" text not null, "name" text not null, "created_at" timestamptz not null default now());
alter table "data_tables" add constraint "data_tables_data_imports_fk" foreign key (data_import_id) references data_imports(id);
alter table "data_tables" add constraint "data_tables_schema_name_uq" unique(schema, name);

# --- !Downs

drop table "data_tables";