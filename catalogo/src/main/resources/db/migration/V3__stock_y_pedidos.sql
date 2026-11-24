CREATE TABLE variante (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  producto_id BIGINT NOT NULL,
  talle VARCHAR(20) NOT NULL,
  color VARCHAR(40) NOT NULL DEFAULT '',
  stock INT NOT NULL DEFAULT 0,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT fk_variante_producto FOREIGN KEY (producto_id)
    REFERENCES producto(id) ON DELETE CASCADE,
  CONSTRAINT uq_variante UNIQUE (producto_id, talle, color),
  CONSTRAINT ck_variante_stock CHECK (stock >= 0)
);

CREATE TABLE pedido (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  fecha DATETIME NOT NULL,
  estado VARCHAR(20) NOT NULL,
  origen VARCHAR(20) NOT NULL,
  cliente_nombre VARCHAR(120) NOT NULL,
  cliente_telefono VARCHAR(40) NOT NULL,
  cliente_email VARCHAR(120),
  nota VARCHAR(500),
  total DECIMAL(12,2) NOT NULL
);

CREATE TABLE pedido_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  pedido_id BIGINT NOT NULL,
  variante_id BIGINT NOT NULL,
  producto_nombre VARCHAR(120) NOT NULL,
  talle VARCHAR(20) NOT NULL,
  color VARCHAR(40) NOT NULL,
  cantidad INT NOT NULL,
  precio_unitario DECIMAL(10,2) NOT NULL,
  CONSTRAINT fk_item_pedido FOREIGN KEY (pedido_id)
    REFERENCES pedido(id) ON DELETE CASCADE,
  CONSTRAINT fk_item_variante FOREIGN KEY (variante_id)
    REFERENCES variante(id),
  CONSTRAINT ck_item_cantidad CHECK (cantidad > 0)
);

CREATE TABLE movimiento_stock (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  variante_id BIGINT NOT NULL,
  fecha DATETIME NOT NULL,
  tipo VARCHAR(20) NOT NULL,
  cantidad INT NOT NULL,
  stock_resultante INT NOT NULL,
  motivo VARCHAR(200),
  usuario VARCHAR(120),
  pedido_id BIGINT NULL,
  CONSTRAINT fk_mov_variante FOREIGN KEY (variante_id)
    REFERENCES variante(id) ON DELETE CASCADE,
  CONSTRAINT fk_mov_pedido FOREIGN KEY (pedido_id)
    REFERENCES pedido(id) ON DELETE SET NULL
);

-- Cada producto existente recibe una variante "Único" con stock 0
INSERT INTO variante (producto_id, talle, color, stock)
SELECT id, 'Único', '', 0 FROM producto;