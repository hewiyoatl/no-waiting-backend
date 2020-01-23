
CREATE TABLE talachitas.contact (
  email                varchar(500) PRIMARY KEY,
  subject           varchar(500),
  message         varchar(1000),
  phone           varchar(500)
);

create table talachitas.users (
id bigint AUTO_INCREMENT primary key,
email  varchar(1000),
nickname varchar(1000),
password varchar(1000));


ALTER table talachitas.users add phone varchar(50);
alter table talachitas.users add f_name varchar(500);
ALTER TABLE talachitas.USERS add l_name varchar(1000);
ALTER TABLE talachitas.USERS add roles varchar(1000);
