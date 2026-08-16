package jaabriu.jaabriu_backend.service;

import jaabriu.jaabriu_backend.dto.AvaliacaoRequest;
import jaabriu.jaabriu_backend.dto.AvaliacaoResponse;
import jaabriu.jaabriu_backend.entity.Avaliacao;
import jaabriu.jaabriu_backend.entity.Chamado;
import jaabriu.jaabriu_backend.entity.Status;
import jaabriu.jaabriu_backend.entity.Usuario;
import jaabriu.jaabriu_backend.exception.BusinessException;
import jaabriu.jaabriu_backend.exception.ResourceNotFoundException;
import jaabriu.jaabriu_backend.repository.AvaliacaoRepository;
import jaabriu.jaabriu_backend.repository.ChamadoRepository;
import jaabriu.jaabriu_backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final ChamadoRepository chamadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistoricoService historicoService;

    public AvaliacaoService(
            AvaliacaoRepository avaliacaoRepository,
            ChamadoRepository chamadoRepository,
            UsuarioRepository usuarioRepository,
            HistoricoService historicoService
    ) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.chamadoRepository = chamadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.historicoService = historicoService;
    }

    public AvaliacaoResponse avaliar(
            Long chamadoId,
            Long usuarioId,
            AvaliacaoRequest request
    ) {

        // Busca chamado
        Chamado chamado = chamadoRepository.findById(chamadoId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Chamado não encontrado")
                );

        if (chamado.getStatus() != Status.FECHADO) {
            throw new BusinessException(
                    "Só é possível avaliar chamados fechados."
            );
        }

        // Busca usuário
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Usuário não encontrado")
                );

        /*
         * ALTERAÇÃO PRINCIPAL:
         *
         * Antes:
         * - Se já existisse avaliação, lançava erro.
         *
         * Agora:
         * - Se já existir avaliação -> atualiza
         * - Se não existir -> cria nova
         */
        Avaliacao avaliacao = avaliacaoRepository.findByChamadoId(chamadoId)
                .orElse(new Avaliacao());

        // Atualiza ou cria dados
        avaliacao.setNota(request.getNota());
        avaliacao.setComentario(request.getComentario());
        avaliacao.setChamado(chamado);
        avaliacao.setUsuario(usuario);

        // Só define data se for novo registro
        if (avaliacao.getCreatedAt() == null) {
            avaliacao.setCreatedAt(LocalDateTime.now());
        }

        // Salva no banco
        Avaliacao avaliacaoSalva = avaliacaoRepository.save(avaliacao);

        // Registra histórico
        historicoService.registrar(
                chamado,
                usuario,
                "Chamado avaliado com nota " + request.getNota()
        );

        return mapToResponse(avaliacaoSalva);
    }

    public AvaliacaoResponse buscarPorChamado(Long chamadoId) {

        Avaliacao avaliacao = avaliacaoRepository.findByChamadoId(chamadoId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Avaliação não encontrada")
                );

        return mapToResponse(avaliacao);
    }

    private AvaliacaoResponse mapToResponse(Avaliacao avaliacao) {
        return AvaliacaoResponse.builder()
                .id(avaliacao.getId())
                .nota(avaliacao.getNota())
                .comentario(avaliacao.getComentario())
                .chamadoId(avaliacao.getChamado().getId())
                .usuarioId(avaliacao.getUsuario().getId())
                .usuarioNome(avaliacao.getUsuario().getNome())
                .createdAt(avaliacao.getCreatedAt())
                .build();
    }
}