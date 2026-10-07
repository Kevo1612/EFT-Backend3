-- Una base de datos por microservicio (patron "database per service"):
-- cada servicio es dueno de sus datos y solo se accede a ellos a traves de su API.
CREATE DATABASE clientes_db;
CREATE DATABASE cuentas_db;
CREATE DATABASE pagos_db;
CREATE DATABASE batch_db;
