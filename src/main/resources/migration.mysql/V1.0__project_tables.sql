CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(256) NOT NULL,
    email VARCHAR(50) NOT NULL,
    role ENUM ('CLIENT', 'COURIER', 'MANAGER') DEFAULT 'CLIENT',
    PRIMARY KEY(id)
);

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    PRIMARY KEY(id)
);

CREATE TABLE IF NOT EXISTS suppliers (
    id BIGINT AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    PRIMARY KEY(id)
);

CREATE TABLE IF NOT EXISTS users_subscriptions_on_categories (
    id BIGINT AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE IF NOT EXISTS users_subscriptions_on_suppliers (
    id BIGINT AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    supplier_id BIGINT NOT NULL,
    PRIMARY KEY(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
);

CREATE TABLE IF NOT EXISTS items (
    id BIGINT AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    amount INT NOT NULL,
    price DECIMAL(15,2) NOT NULL,
    status ENUM ('IN_PROCESSING', 'AVAILABLE', 'NOT_AVAILABLE') DEFAULT 'IN_PROCESSING',
    supplier_id BIGINT NOT NULL,
    PRIMARY KEY(id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
);

CREATE TABLE IF NOT EXISTS attachments_to_items (
     id BIGINT AUTO_INCREMENT,
     link VARCHAR(256) NOT NULL,
     item_id BIGINT NOT NULL,
     PRIMARY KEY(id),
     FOREIGN KEY (item_id) REFERENCES items(id)
);

CREATE TABLE IF NOT EXISTS items_categories (
    id BIGINT AUTO_INCREMENT,
    item_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY(id),
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE IF NOT EXISTS items_to_single_user (
    id BIGINT AUTO_INCREMENT,
    item_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY(id),
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS user_groups (
    id BIGINT AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    PRIMARY KEY(id)
);

CREATE TABLE IF NOT EXISTS users_to_groups (
    id BIGINT AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    PRIMARY KEY(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (group_id) REFERENCES user_groups(id)
);

CREATE TABLE IF NOT EXISTS items_to_groups (
    id BIGINT AUTO_INCREMENT,
    item_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    PRIMARY KEY(id),
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (group_id) REFERENCES user_groups(id)
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT,
    total DECIMAL(15,2) NOT NULL,
    address VARCHAR(100) NOT NULL,
    status ENUM ('ORDERED', 'ASSIGNED', 'DELIVERED') DEFAULT 'ORDERED',
    PRIMARY KEY(id)
);

CREATE TABLE IF NOT EXISTS carts (
    id BIGINT AUTO_INCREMENT,
    quantity INT NOT NULL,
    item_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    order_id BIGINT,
    PRIMARY KEY(id),
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (order_id) REFERENCES orders(id)
);