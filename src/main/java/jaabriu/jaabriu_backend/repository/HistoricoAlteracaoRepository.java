package jaabriu.jaabriu_backend.repository;

import jaabriu.jaabriu_backend.entity.HistoricoAlteracao;
import jaabriu.jaabriu_backend.entity.Chamado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoAlteracaoRepository extends JpaRepository<HistoricoAlteracao, Long> {

    // Buscar histórico de um chamado (ordenado do mais recente)
    List<HistoricoAlteracao> findByChamadoOrderByCreatedAtDesc(Chamado chamado);
}