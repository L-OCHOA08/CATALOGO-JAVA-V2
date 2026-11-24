INSERT INTO categoria (nombre, slug) VALUES
  ('Remeras', 'remeras'),
  ('Pantalones', 'pantalones');

INSERT INTO producto (nombre, descripcion, precio, activo, categoria_id) VALUES
  ('REMER4 HUG0 B055', 'TALLE S-M-L-XL-XXL, Negro, Algodón 100%.', 12500.00, TRUE,
     (SELECT id FROM categoria WHERE slug = 'remeras')),
  ('REMER4 NK', 'TALLE S-M-L-XL-XXL, BLANCA Y NEGRA.', 15900.00, TRUE,
     (SELECT id FROM categoria WHERE slug = 'remeras')),
  ('P4NT4L0N NK R0MPEVIENT0 EXCLUSIV0', 'TALLE S-M-L-XL-XXL, B0LSILLO CON CIERRE.', 8900.00, TRUE,
     (SELECT id FROM categoria WHERE slug = 'pantalones'));