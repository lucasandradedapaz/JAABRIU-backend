package jaabriu.jaabriu_backend.repository;

import jaabriu.jaabriu_backend.entity.Chamado;
import jaabriu.jaabriu_backend.entity.SolucaoChamado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolucaoChamadoRepository extends JpaRepository<SolucaoChamado, Long> {

    List<SolucaoChamado> findByChamadoOrderByNumeroAsc(Chamado chamado);

    long countByChamado(Chamado chamado);

    boolean existsByChamado(Chamado chamado);
}
