package jaabriu.jaabriu_backend.repository;

import jaabriu.jaabriu_backend.entity.Comentario;
import jaabriu.jaabriu_backend.entity.Chamado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {

    // Listar comentários de um chamado (mais recentes primeiro)
    List<Comentario> findByChamadoOrderByCreatedAtDesc(Chamado chamado);
}