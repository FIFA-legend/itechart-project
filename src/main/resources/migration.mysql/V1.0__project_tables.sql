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

CREATE TABLE IF NOT EXISTS items (
    id BIGINT AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    amount INT,
    price DECIMAL(15,2) NOT NULL,
    status ENUM ('IN_PROCESSING', 'AVAILABLE', 'NOT_AVAILABLE') DEFAULT 'IN_PROCESSING',
    category_id BIGINT,
    supplier_id BIGINT,
    PRIMARY KEY(id),
    FOREIGN KEY (category_id) REFERENCES categories(id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT,
    total DECIMAL(15,2) NOT NULL,
    status ENUM ('ORDERED', 'ASSIGNED', 'DELIVERED') DEFAULT 'ORDERED',
    user_id BIGINT,
    PRIMARY KEY(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS users_categories (
    user_id BIGINT,
    category_id BIGINT,
    PRIMARY KEY(user_id, category_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
	FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE IF NOT EXISTS users_suppliers (
    user_id BIGINT,
    supplier_id BIGINT,
    PRIMARY KEY(user_id, supplier_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
);

CREATE TABLE IF NOT EXISTS users_items (
    user_id BIGINT,
    item_id BIGINT,
    PRIMARY KEY(user_id, item_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (item_id) REFERENCES items(id)
);

CREATE TABLE IF NOT EXISTS orders_items (
    order_id BIGINT,
    item_id BIGINT,
    quantity INT,
    PRIMARY KEY(order_id, item_id),
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (item_id) REFERENCES items(id)
);