-- Datos de prueba. Los username coinciden con los usuarios creados en Keycloak (keycloak/realm-banco-xyz.json)
INSERT INTO clientes (id, rut, username, nombres, apellidos, email, telefono, direccion, segmento, activo, fecha_registro, version)
VALUES (1, '12345678-5', 'cliente1', 'Juan', 'Perez Soto', 'juan.perez@correo.cl', '+56911111111', 'Av. Providencia 1234, Santiago', 'PERSONA', true, now(), 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (id, rut, username, nombres, apellidos, email, telefono, direccion, segmento, activo, fecha_registro, version)
VALUES (2, '9876543-3', 'cliente2', 'Maria', 'Gonzalez Rojas', 'maria.gonzalez@correo.cl', '+56922222222', 'Calle Larga 55, Valparaiso', 'PREMIUM', true, now(), 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (id, rut, username, nombres, apellidos, email, telefono, direccion, segmento, activo, fecha_registro, version)
VALUES (3, '11111111-1', 'admin', 'Ana', 'Administradora', 'admin@bancoxyz.cl', '+56933333333', 'Casa Matriz', 'PERSONA', true, now(), 0)
ON CONFLICT (id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('clientes', 'id'), (SELECT MAX(id) FROM clientes));
