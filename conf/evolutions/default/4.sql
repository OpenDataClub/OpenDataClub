# --- !Ups

alter table external_data_sources add column class_name text;
update external_data_sources set class_name = 'com.opendataclub.scrapers.ine.IneEpaScraper' where id = -1;
alter table external_data_sources alter column class_name set not null;

# --- !Downs

alter table external_data_sources drop column class_name;