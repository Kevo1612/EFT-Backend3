-- Cuentas de prueba (clientes 1, 2 y 3 de ms-clientes)
INSERT INTO cuentas (id, numero_cuenta, cliente_id, titular_username, tipo, saldo, estado, fecha_apertura, version)
VALUES (1, 'XYZ-1000000001', 1, 'cliente1', 'AHORRO', 1500000.00, 'ACTIVA', now(), 0) ON CONFLICT (id) DO NOTHING;
INSERT INTO cuentas (id, numero_cuenta, cliente_id, titular_username, tipo, saldo, estado, fecha_apertura, version)
VALUES (2, 'XYZ-1000000002', 1, 'cliente1', 'CORRIENTE', 350000.00, 'ACTIVA', now(), 0) ON CONFLICT (id) DO NOTHING;
INSERT INTO cuentas (id, numero_cuenta, cliente_id, titular_username, tipo, saldo, estado, fecha_apertura, version)
VALUES (3, 'XYZ-1000000003', 2, 'cliente2', 'CORRIENTE', 8000000.00, 'ACTIVA', now(), 0) ON CONFLICT (id) DO NOTHING;
INSERT INTO cuentas (id, numero_cuenta, cliente_id, titular_username, tipo, saldo, estado, fecha_apertura, version)
VALUES (4, 'XYZ-1000000004', 3, 'admin', 'VISTA', 100000.00, 'ACTIVA', now(), 0) ON CONFLICT (id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('cuentas', 'id'), (SELECT MAX(id) FROM cuentas));
