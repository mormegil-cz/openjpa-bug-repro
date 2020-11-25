create table users(id int not null, name nvarchar(100) not null, email varchar(100) not null);
create table orders(id int not null, product_code varchar(10) not null, user_id int not null);
alter table users add constraint pk_users primary key(id);
alter table orders add constraint pk_orders primary key(id);
alter table orders add constraint fk_orders_users foreign key (user_id) references users(id);
create unique index ix_users_name on users(name);
create index ix_users_email on users(email);
