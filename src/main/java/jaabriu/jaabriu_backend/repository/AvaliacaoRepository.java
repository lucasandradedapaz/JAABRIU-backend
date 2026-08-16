package jaabriu.jaabriu_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jaabriu.jaabriu_backend.entity.Avaliacao;

@Repository
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    Optional<Avaliacao> findByChamadoId(Long chamadoId);

    @Query("SELECT AVG(a.nota) FROM Avaliacao a")
    Double calcularMediaNotas();
}