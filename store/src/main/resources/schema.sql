CREATE TABLE IF NOT EXISTS items (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      title VARCHAR(255) NOT NULL,
                      description VARCHAR(2000),
                      img_path VARCHAR(255),
                      count INT NOT NULL,
                      price DECIMAL(10,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS orders (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_items (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            order_id BIGINT NOT NULL,
                            item_id BIGINT NOT NULL,
                            count INT NOT NULL,
                            price DECIMAL(10, 2) NOT NULL,
                            CONSTRAINT fk_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
                            CONSTRAINT fk_item FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);
