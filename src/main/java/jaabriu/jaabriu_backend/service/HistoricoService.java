package jaabriu.jaabriu_backend.service;
import jaabriu.jaabriu_backend.entity.TipoAlteracao;

import jaabriu.jaabriu_backend.dto.HistoricoResponse;
import jaabriu.jaabriu_backend.entity.*;
import jaabriu.jaabriu_backend.exception.ResourceNotFoundException;
import jaabriu.jaabriu_backend.repository.ChamadoRepository;
import jaabriu.jaabriu_backend.repository.HistoricoAlteracaoRepository;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.List;

@Service
public class HistoricoService {

    private final HistoricoAlteracaoRepository historicoRepository;
    private final ChamadoRepository chamadoRepository;

    public HistoricoService(
            HistoricoAlteracaoRepository historicoRepository,
            ChamadoRepository chamadoRepository
    ) {
        this.historicoRepository = historicoRepository;
        this.chamadoRepository = chamadoRepository;
    }

    // 🔥 MÉTODO PRINCIPAL CORRIGIDO
    public void registrar(Chamado chamado,
                          Usuario usuario,
                          String descricao,
                          TipoAlteracao tipo) {

        HistoricoAlteracao historico = new HistoricoAlteracao();

        historico.setChamado(chamado);
        historico.setUsuario(usuario);
        historico.setDescricao(descricao);
        historico.setTipoAlteracao(tipo); // ✅ ESSENCIAL (corrige seu erro)
        historico.setCreatedAt(LocalDateTime.now());

        historicoRepository.save(historico);
    }

    // 🔥 OPCIONAL (atalho pra não quebrar código antigo)
    public void registrar(Chamado chamado,
                          Usuario usuario,
                          String descricao) {

        registrar(chamado, usuario, descricao, TipoAlteracao.OUTRO);
    }

    public List<HistoricoResponse> listarPorChamado(Long chamadoId) {

        Chamado chamado = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Chamado não encontrado"));

        return historicoRepository.findByChamadoOrderByCreatedAtDesc(chamado)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private HistoricoResponse mapToResponse(HistoricoAlteracao historico) {
        return HistoricoResponse.builder()
                .id(historico.getId())
                .descricao(historico.getDescricao())
                .usuarioId(
                        historico.getUsuario() != null
                                ? historico.getUsuario().getId()
                                : null
                )
                .usuarioNome(
                        historico.getUsuario() != null
                                ? historico.getUsuario().getNome()
                                : null
                )
                .chamadoId(
                        historico.getChamado() != null
                                ? historico.getChamado().getId()
                                : null
                )
                .createdAt(historico.getCreatedAt())
                .build();
    }
}