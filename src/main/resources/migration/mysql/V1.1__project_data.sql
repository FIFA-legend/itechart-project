INSERT INTO categories (name) VALUES ('Laptops');
INSERT INTO categories (name) VALUES ('Phones');

INSERT INTO suppliers (name) VALUES ('ASUS');
INSERT INTO suppliers (name) VALUES ('Apple');

INSERT INTO items (name, description, amount, price, status, supplier_id)
VALUES ('ASUS TUF GAMING FX504', 'Awesome laptop', 10, 3000, 'IN_PROCESSING', 1);
INSERT INTO items (name, description, amount, price, status, supplier_id)
VALUES ('Iphone', 'Awesome phone', 100, 750, 'IN_PROCESSING', 2);

INSERT INTO users (username, password, email) VALUES ('some_username', '2adafawrawa2', 'someemail@gmail.com');
INSERT INTO users (username, password, email) VALUES ('another_username', 'jwah12ttwqahd', 'anotheremail@gmail.com');

INSERT INTO users_subscriptions_on_suppliers (user_id, supplier_id) VALUES (1, 1);
INSERT INTO users_subscriptions_on_suppliers (user_id, supplier_id) VALUES (2, 2);
INSERT INTO users_subscriptions_on_suppliers (user_id, supplier_id) VALUES (1, 2);

INSERT INTO users_subscriptions_on_categories (user_id, category_id) VALUES (1, 1);
INSERT INTO users_subscriptions_on_categories (user_id, category_id) VALUES (2, 2);
INSERT INTO users_subscriptions_on_categories (user_id, category_id) VALUES (2, 1);

INSERT INTO items_categories (item_id, category_id) VALUES (1, 1);
INSERT INTO items_categories (item_id, category_id) VALUES (2, 2);

INSERT INTO user_groups (name) VALUES ('Бандиты');

INSERT INTO users_to_groups (user_id, group_id) VALUES (1, 1);
INSERT INTO users_to_groups (user_id, group_id) VALUES (2, 1);

INSERT INTO items_to_groups (item_id, group_id) VALUES (1, 1);
INSERT INTO items_to_groups (item_id, group_id) VALUES (2, 1);

INSERT INTO orders (total, address, status, user_id) VALUES (3000, 'Some address', 'ORDERED', 1);
INSERT INTO orders (total, address, status, user_id) VALUES (1500, 'Some address', 'ORDERED', 2);

INSERT INTO carts (quantity, item_id, user_id, order_id) VALUES (1, 1, 1, 1);
INSERT INTO carts (quantity, item_id, user_id, order_id) VALUES (2, 2, 2, 2);