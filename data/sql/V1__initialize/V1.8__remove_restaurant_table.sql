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

