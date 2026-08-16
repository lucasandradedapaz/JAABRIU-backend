package jaabriu.jaabriu_backend.repository;

import jaabriu.jaabriu_backend.entity.Anexo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnexoRepository extends JpaRepository<Anexo, Long> {

    List<Anexo> findByChamadoId(Long chamadoId);
}