package jaabriu.jaabriu_backend.service;

import jaabriu.jaabriu_backend.entity.Chamado;
import jaabriu.jaabriu_backend.entity.Status;
import jaabriu.jaabriu_backend.entity.Prioridade;
import jaabriu.jaabriu_backend.repository.ChamadoRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SlaService {

    private final ChamadoRepository chamadoRepository;

    public SlaService(ChamadoRepository chamadoRepository) {
        this.chamadoRepository = chamadoRepository;
    }

    @Scheduled(fixedRate = 300000)
    public void verificarSla() {

        List<Chamado> chamados = chamadoRepository.findAll();

        for (Chamado chamado : chamados) {

            if (chamado.getStatus() == null) {
                continue;
            }

            // ✅ USA ENUM DIRETO
            Status status = chamado.getStatus();

            boolean emAberto = status == Status.ABERTO
                    || status == Status.EM_ANDAMENTO;

            if (!emAberto) {
                continue;
            }

            if (chamado.getCreatedAt() == null) {
                continue;
            }

            LocalDateTime limite;

            // ✅ PRIORIDADE COMO ENUM
            Prioridade prioridade = chamado.getPrioridade();

            if (prioridade == null) {
                prioridade = Prioridade.MEDIA;
            }

            switch (prioridade) {
                case ALTA -> limite = chamado.getCreatedAt().plusHours(4);
                case MEDIA -> limite = chamado.getCreatedAt().plusHours(24);
                case BAIXA -> limite = chamado.getCreatedAt().plusHours(72);
                case URGENTE -> limite = chamado.getCreatedAt().plusHours(2);
                default -> limite = chamado.getCreatedAt().plusHours(24);
            }

            boolean atrasado = LocalDateTime.now().isAfter(limite);

            if (!Boolean.valueOf(atrasado).equals(chamado.getAtrasado())) {
                chamado.setAtrasado(atrasado);
                chamadoRepository.save(chamado);
            }
        }
    }
}