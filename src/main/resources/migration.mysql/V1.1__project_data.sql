INSERT INTO categories (name) VALUES ('Laptops');
INSERT INTO categories (name) VALUES ('Phones');

INSERT INTO suppliers (name) VALUES ('ASUS');
INSERT INTO suppliers (name) VALUES ('Apple');

INSERT INTO items (name, description, amount, price, status, category_id, supplier_id)
VALUES ('ASUS TUF GAMING FX504', 'Awesome laptop', 10, 3000, 'IN_PROCESSING', 1, 1);
INSERT INTO items (name, description, amount, price, status, category_id, supplier_id)
VALUES ('Iphone', 'Awesome phone', 100, 750, 'IN_PROCESSING', 2, 2);

INSERT INTO users (username, password, email) VALUES ('some_username', '2adafawrawa2', 'someemail@gmail.com');
INSERT INTO users (username, password, email) VALUES ('another_username', 'jwah12ttwqahd', 'anotheremail@gmail.com');

INSERT INTO orders (total, status, user_id) VALUES (3000, 'ORDERED', 1);
INSERT INTO orders (total, status, user_id) VALUES (1500, 'ORDERED', 2);

INSERT INTO orders_items (order_id, item_id, quantity) VALUES (1, 1, 1);
INSERT INTO orders_items (order_id, item_id, quantity) VALUES (2, 2, 2);