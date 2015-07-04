# --- !Ups

alter table data_imports rename column external_data_id to external_data_source_id; 

# --- !Downs

alter table data_imports rename column external_data_source_id to external_data_id;