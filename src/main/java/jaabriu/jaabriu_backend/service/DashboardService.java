package jaabriu.jaabriu_backend.service;

import jaabriu.jaabriu_backend.dto.DashboardResponse;
import jaabriu.jaabriu_backend.entity.Status; // 👈 IMPORTANTE
import jaabriu.jaabriu_backend.repository.AvaliacaoRepository;
import jaabriu.jaabriu_backend.repository.ChamadoRepository;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final ChamadoRepository chamadoRepository;
    private final AvaliacaoRepository avaliacaoRepository;

    public DashboardService(
            ChamadoRepository chamadoRepository,
            AvaliacaoRepository avaliacaoRepository
    ) {
        this.chamadoRepository = chamadoRepository;
        this.avaliacaoRepository = avaliacaoRepository;
    }

    public DashboardResponse obterDashboard() {

        Long totalChamados = chamadoRepository.count();

        // ✅ CORRIGIDO
        Long chamadosAbertos = chamadoRepository.countByStatus(Status.ABERTO);

        Long chamadosEmAndamento = chamadoRepository.countByStatus(Status.EM_ANDAMENTO);

        Long chamadosResolvidos = chamadoRepository.countByStatus(Status.RESOLVIDO);

        Long chamadosAtrasados = chamadoRepository.countByAtrasadoTrue();

        Double mediaAvaliacoes = avaliacaoRepository.calcularMediaNotas();

        if (mediaAvaliacoes == null) {
            mediaAvaliacoes = 0.0;
        }

        return DashboardResponse.builder()
                .totalChamados(totalChamados)
                .chamadosAbertos(chamadosAbertos)
                .chamadosEmAndamento(chamadosEmAndamento)
                .chamadosResolvidos(chamadosResolvidos)
                .chamadosAtrasados(chamadosAtrasados)
                .mediaAvaliacoes(mediaAvaliacoes)
                .build();
    }
}