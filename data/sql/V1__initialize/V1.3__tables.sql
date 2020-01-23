--create schema talachitas;
--
--CREATE TABLE talachitas.restaurant (
--  id   SERIAL PRIMARY KEY,
--  name VARCHAR(100)
--);
--
--CREATE TABLE talachitas.sucursal (
--  id                SERIAL PRIMARY KEY,
--  address_1         VARCHAR(500),
--  address_2         VARCHAR(500),
--  zip_code          VARCHAR(5),
--  state             VARCHAR(2),
--  city              VARCHAR(100),
--  country           VARCHAR(10),
--  phone_number      VARCHAR(20),
--  restaurant_id     BIGINT NOT NULL,
--  latitude          FLOAT  NOT NULL,
--  longitud          FLOAT  NOT NULL,
--  created_timestamp TIMESTAMP,
--  deleted           BOOLEAN DEFAULT FALSE,
--  FOREIGN KEY (restaurant_id) REFERENCES talachitas.restaurant (id)
--);
--
--CREATE TABLE talachitas.service_time (
--  id          SERIAL PRIMARY KEY,
--  sucursal_id BIGINT,
--  day         INT,
--  start_time  TIMESTAMP,
--  end_time    TIMESTAMP,
--  FOREIGN KEY (sucursal_id) REFERENCES talachitas.sucursal (id)
--);
--
--CREATE TABLE talachitas.rest_user (
--  id                SERIAL PRIMARY KEY,
--  sucursal_id       BIGINT,
--  first_name        VARCHAR(100),
--  last_name         VARCHAR(100),
--  phone_number      VARCHAR(20),
--  user_name         VARCHAR(50),
--  password          VARCHAR(64),
--  created_timestamp TIMESTAMP,
--  deleted           BOOLEAN DEFAULT FALSE,
--  FOREIGN KEY (sucursal_id) REFERENCES talachitas.sucursal (id)
--);
--
--CREATE TABLE talachitas.client (
--  id                SERIAL PRIMARY KEY,
--  phone_number      VARCHAR(20) UNIQUE,
--  first_name        VARCHAR(100),
--  last_name         VARCHAR(100),
--  email             VARCHAR(300),
--  user_name         VARCHAR(100),
--  password          VARCHAR(64),
--  created_timestamp TIMESTAMP,
--  deleted           BOOLEAN DEFAULT FALSE NOT NULL
--);
--
--CREATE TABLE talachitas.reservation (
--  id                SERIAL PRIMARY KEY,
--  user_id           BIGINT,
--  user_type         BIGINT,
--  sucursal_id       BIGINT,
--  status            INT,
--  created_timestamp TIMESTAMP,
--  FOREIGN KEY (user_id) REFERENCES talachitas.client (id),
--  FOREIGN KEY (sucursal_id) REFERENCES talachitas.sucursal (id)
--);
--
--CREATE TABLE talachitas.reservation_event (
--  id                SERIAL PRIMARY KEY,
--  user_id           BIGINT,
--  user_type         BIGINT,
--  reservation_id    BIGINT,
--  status            INT,
--  created_timestamp TIMESTAMP,
--  FOREIGN KEY (reservation_id) REFERENCES talachitas.reservation (id)
--);


CREATE TABLE talachitas.restaurant (
  id    bigint AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100)
);

CREATE TABLE talachitas.sucursal (
  id                bigint AUTO_INCREMENT PRIMARY KEY,
  address_1         VARCHAR(500),
  address_2         VARCHAR(500),
  zip_code          VARCHAR(5),
  state             VARCHAR(2),
  city              VARCHAR(100),
  country           VARCHAR(10),
  phone_number      VARCHAR(20),
  restaurant_id     BIGINT NOT NULL,
  latitude          FLOAT  NOT NULL,
  longitud          FLOAT  NOT NULL,
  created_timestamp TIMESTAMP default current_timestamp,
  deleted           BOOLEAN DEFAULT FALSE,
  FOREIGN KEY (restaurant_id) REFERENCES talachitas.restaurant (id)
);

CREATE TABLE talachitas.service_time (
  id          bigint AUTO_INCREMENT PRIMARY KEY,
  sucursal_id BIGINT,
  day         INT,
  start_time  TIMESTAMP default current_timestamp,
  end_time    TIMESTAMP null,
  FOREIGN KEY (sucursal_id) REFERENCES talachitas.sucursal (id)
);

CREATE TABLE talachitas.rest_user (
  id                bigint AUTO_INCREMENT PRIMARY KEY,
  sucursal_id       BIGINT,
  first_name        VARCHAR(100),
  last_name         VARCHAR(100),
  phone_number      VARCHAR(20),
  user_name         VARCHAR(50),
  password          VARCHAR(64),
  created_timestamp TIMESTAMP default current_timestamp,
  deleted           BOOLEAN DEFAULT FALSE,
  FOREIGN KEY (sucursal_id) REFERENCES talachitas.sucursal (id)
);

CREATE TABLE talachitas.client (
  id                bigint AUTO_INCREMENT PRIMARY KEY,
  phone_number      VARCHAR(20) UNIQUE,
  first_name        VARCHAR(100),
  last_name         VARCHAR(100),
  email             VARCHAR(300),
  user_name         VARCHAR(100),
  password          VARCHAR(64),
  created_timestamp TIMESTAMP default current_timestamp,
  deleted           BOOLEAN DEFAULT FALSE NOT NULL
);

CREATE TABLE talachitas.reservation (
  id                bigint AUTO_INCREMENT PRIMARY KEY,
  user_id           BIGINT,
  user_type         BIGINT,
  sucursal_id       BIGINT,
  status            INT,
  created_timestamp TIMESTAMP default current_timestamp,
  FOREIGN KEY (user_id) REFERENCES talachitas.client (id),
  FOREIGN KEY (sucursal_id) REFERENCES talachitas.sucursal (id)
);

CREATE TABLE talachitas.reservation_event (
  id                bigint AUTO_INCREMENT PRIMARY KEY,
  user_id           BIGINT,
  user_type         BIGINT,
  reservation_id    BIGINT,
  status            INT,
  created_timestamp TIMESTAMP default current_timestamp,
  FOREIGN KEY (reservation_id) REFERENCES talachitas.reservation (id)
);

