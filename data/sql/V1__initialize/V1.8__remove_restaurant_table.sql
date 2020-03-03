ALTER TABLE talachitas.sucursal
	DROP FOREIGN KEY `sucursal_ibfk_1`;

ALTER TABLE talachitas.sucursal
  DROP COLUMN restaurant_id;


drop table talachitas.restaurant;

RENAME TABLE talachitas.sucursal TO talachitas.restaurant;

ALTER TABLE talachitas.reservation
	DROP FOREIGN KEY `reservation_ibfk_1`;

ALTER TABLE talachitas.reservation
	DROP FOREIGN KEY `reservation_ibfk_2`;

ALTER TABLE talachitas.reservation
change column sucursal_id restaurant_id BIGINT;

alter table talachitas.reservation
ADD FOREIGN KEY (user_id) REFERENCES users(ID);

alter table talachitas.reservation
ADD FOREIGN KEY (restaurant_id) REFERENCES restaurant(ID);

drop table talachitas.client;

ALTER TABLE talachitas.rest_user
	DROP FOREIGN KEY `rest_user_ibfk_1`;

ALTER TABLE talachitas.rest_user
  DROP COLUMN sucursal_id,
  drop column first_name,
  drop column last_name,
  drop column phone_number,
  drop column user_name,
  drop column password;

ALTER TABLE talachitas.rest_user
ADD COLUMN restaurant_id bigint;

alter table talachitas.rest_user
ADD FOREIGN KEY (restaurant_id) REFERENCES restaurant(ID);

ALTER TABLE talachitas.rest_user
ADD COLUMN user_id bigint;

alter table talachitas.rest_user
ADD FOREIGN KEY (restaurant_id) REFERENCES restaurant(ID);

drop table talachitas.reservation_event;

ALTER TABLE talachitas.restaurant
ADD COLUMN business_name varchar(50);

ALTER TABLE talachitas.restaurant
ADD COLUMN suffix_zip_code varchar(10);

ALTER TABLE talachitas.restaurant
change longitud longitude float;

ALTER TABLE talachitas.restaurant
add updated_timestamp TIMESTAMP default current_timestamp;

alter table talachitas.restaurant
change country country varchar(200);

ALTER TABLE talachitas.users
add created_timestamp TIMESTAMP default current_timestamp,
add updated_timestamp TIMESTAMP default current_timestamp,
add deleted BOOLEAN DEFAULT FALSE;


drop table talachitas.service_time;

drop table talachitas.rest_user;

ALTER TABLE talachitas.reservation
DROP COLUMN user_type;

ALTER TABLE talachitas.reservation
add deleted BOOLEAN DEFAULT FALSE;

ALTER TABLE talachitas.reservation
DROP COLUMN status;

alter table talachitas.reservation
add status ENUM('STARTED','IN_QUEUE', 'AVAILABLE', 'COMPLETED','CANCELLED');

ALTER TABLE talachitas.reservation
add updated_timestamp TIMESTAMP default current_timestamp;

CREATE TABLE talachitas.reservation_logs (
  id                bigint AUTO_INCREMENT PRIMARY KEY,
  reservation_id    BIGINT,
  status ENUM('STARTED','IN_QUEUE', 'AVAILABLE', 'COMPLETED','CANCELLED'),
  created_timestamp TIMESTAMP default current_timestamp,
  updated_timestamp TIMESTAMP default current_timestamp,
  FOREIGN KEY (reservation_id) REFERENCES talachitas.reservation (id)
);

alter table talachitas.reservation
add column source_latitude          FLOAT  NOT NULL,
add column source_longitude          FLOAT  NOT NULL;

alter table talachitas.restaurant
add column average_waiting_time long;

alter table talachitas.reservation
add column comments varchar(500);

alter table talachitas.reservation
add column waiting_time_creation long,
add column waiting_time_counting long;

CREATE TABLE talachitas.address (
  id                bigint AUTO_INCREMENT PRIMARY KEY,
  address_1         VARCHAR(500),
  address_2         VARCHAR(500),
  zip_code          VARCHAR(10),
  suffix_zip_code   varchar(10),
  state             VARCHAR(10),
  city              VARCHAR(100),
  country           VARCHAR(100),
  latitude          VARCHAR(20),
  longitude         VARCHAR(20),
  created_timestamp TIMESTAMP default current_timestamp,
  deleted           BOOLEAN DEFAULT FALSE
);

alter table talachitas.restaurant
drop column address_1,
drop column address_2,
drop column zip_code,
drop column state,
drop column city,
drop column country,
drop column latitude,
drop column longitude,
drop column suffix_zip_code;

alter table talachitas.restaurant
add column address_id BIGINT not null;

alter table talachitas.restaurant
ADD FOREIGN KEY (address_id) REFERENCES address(ID);

alter table talachitas.users
add column address_id BIGINT null;

alter table talachitas.users
ADD FOREIGN KEY (address_id) REFERENCES address(ID);

alter table talachitas.reservation
drop column source_latitude,
drop column source_longitude;

alter table talachitas.reservation
add column source_address_id bigint not null,
add column destination_address_id bigint not null;


alter table talachitas.reservation
ADD FOREIGN KEY (source_address_id) REFERENCES users(address_id),
add foreign key (destination_address_id) references restaurant(address_id);
