package jaabriu.jaabriu_backend.repository;

import jaabriu.jaabriu_backend.entity.Chamado;
import jaabriu.jaabriu_backend.entity.Status;
import jaabriu.jaabriu_backend.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChamadoRepository extends JpaRepository<Chamado, Long> {

    Page<Chamado> findByUsuario(Usuario usuario, Pageable pageable);

    Page<Chamado> findByTecnico(Usuario tecnico, Pageable pageable);

    Page<Chamado> findByStatus(Status status, Pageable pageable);

    Page<Chamado> findByTecnicoAndStatus(Usuario tecnico, Status status, Pageable pageable);

    // ✅ CORRIGIDO
    Long countByStatus(Status status);

    Long countByAtrasadoTrue();
}