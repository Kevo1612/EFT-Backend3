package cl.bancoxyz.clientes.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.bancoxyz.clientes.domain.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByUsername(String username);

    boolean existsByRut(String rut);

    boolean existsByUsername(String username);
}
