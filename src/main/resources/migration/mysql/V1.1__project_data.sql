INSERT INTO categories (name) VALUES ('Laptops');
INSERT INTO categories (name) VALUES ('Phones');
INSERT INTO categories (name) VALUES ('Tablets');

INSERT INTO suppliers (name) VALUES ('ASUS');
INSERT INTO suppliers (name) VALUES ('Apple');

INSERT INTO items (name, description, amount, price, status, supplier_id)
VALUES ('ASUS TUF GAMING FX504', 'Awesome gaming laptop', 50, 1400, 'AVAILABLE', 1);
INSERT INTO items (name, description, amount, price, status, supplier_id)
VALUES ('Iphone 13', 'New Apple phone', 100, 750, 'IN_PROCESSING', 2);
INSERT INTO items (name, description, amount, price, status, supplier_id)
VALUES ('Iphone 12', 'Apple phone on sale now', 0, 550, 'NOT_AVAILABLE', 2);

INSERT INTO items_categories (item_id, category_id) VALUES (1, 1);
INSERT INTO items_categories (item_id, category_id) VALUES (2, 2);
INSERT INTO items_categories (item_id, category_id) VALUES (3, 2);

INSERT INTO attachments_to_items (link, item_id) VALUES ('1.jpg', 1);
INSERT INTO attachments_to_items (link, item_id) VALUES ('2.jpg', 2);
INSERT INTO attachments_to_items (link, item_id) VALUES ('3.jpeg', 3);

INSERT INTO users (username, password, email, role) 
VALUES ('ShopManager', 'dVLlP0EYdyL3PLGDNHCKUA==', 'manageremail@gmail.com', 'MANAGER');
INSERT INTO users (username, password, email, role) 
VALUES ('ShopCourier', 'dVLlP0EYdyL3PLGDNHCKUA==', 'courieremail@gmail.com', 'COURIER');
INSERT INTO users (username, password, email, role) 
VALUES ('KolodkoNikita', 'dVLlP0EYdyL3PLGDNHCKUA==', 'kolodkonikitos@gmail.com', 'CLIENT');
INSERT INTO users (username, password, email, role) 
VALUES ('KolodkoNikitaOfficial', 'dVLlP0EYdyL3PLGDNHCKUA==', 'kolodkonikita20010508@gmail.com', 'CLIENT');

INSERT INTO users_subscriptions_on_suppliers (user_id, supplier_id) VALUES (3, 2);

INSERT INTO users_subscriptions_on_categories (user_id, category_id) VALUES (3, 1);
INSERT INTO users_subscriptions_on_categories (user_id, category_id) VALUES (3, 3);

INSERT INTO orders (total, address, status, user_id) VALUES (1400, 'ул. П. Бровки, 6', 'DELIVERED', 3);
INSERT INTO orders (total, address, status, user_id) VALUES (1100, 'ул. П. Бровки, 6', 'ORDERED', 3);

INSERT INTO carts (quantity, item_id, user_id, order_id) VALUES (1, 1, 3, 1);
INSERT INTO carts (quantity, item_id, user_id, order_id) VALUES (2, 3, 3, 2);

INSERT INTO user_groups (name) VALUES ('Ребята из ItechArt');

INSERT INTO users_to_groups (user_id, group_id) VALUES (3, 1);
INSERT INTO users_to_groups (user_id, group_id) VALUES (4, 1);

INSERT INTO items_to_groups (item_id, group_id) VALUES (2, 1);