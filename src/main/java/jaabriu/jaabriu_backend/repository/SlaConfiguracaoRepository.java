package jaabriu.jaabriu_backend.repository;

import jaabriu.jaabriu_backend.entity.Prioridade;
import jaabriu.jaabriu_backend.entity.SlaConfiguracao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SlaConfiguracaoRepository extends JpaRepository<SlaConfiguracao, Long> {
    Optional<SlaConfiguracao> findByPrioridade(Prioridade prioridade);
}
