package jaabriu.jaabriu_backend.repository;

import jaabriu.jaabriu_backend.entity.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    List<Notificacao> findTop30ByDestinatarioIdOrderByCreatedAtDesc(Long destinatarioId);

    Long countByDestinatarioIdAndLidaFalse(Long destinatarioId);

    List<Notificacao> findByDestinatarioIdAndLidaFalse(Long destinatarioId);
}
