package jaabriu.jaabriu_backend.service;

import jaabriu.jaabriu_backend.dto.ChamadoFiltroRequest;
import jaabriu.jaabriu_backend.dto.ChamadoRequest;
import jaabriu.jaabriu_backend.dto.ChamadoResponse;
import jaabriu.jaabriu_backend.dto.FecharChamadoRequest;
import jaabriu.jaabriu_backend.entity.*;
import jaabriu.jaabriu_backend.exception.ResourceNotFoundException;
import jaabriu.jaabriu_backend.repository.ChamadoRepository;
import jaabriu.jaabriu_backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChamadoService {

    private final ChamadoRepository chamadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistoricoService historicoService;

    public ChamadoService(
            ChamadoRepository chamadoRepository,
            UsuarioRepository usuarioRepository,
            HistoricoService historicoService
    ) {
        this.chamadoRepository = chamadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.historicoService = historicoService;
    }

    public ChamadoResponse criar(Long usuarioId, ChamadoRequest request) {

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        Status status = Status.valueOf(request.getStatus().toUpperCase());
        Prioridade prioridade = Prioridade.valueOf(request.getPrioridade().toUpperCase());
        Categoria categoria = Categoria.valueOf(request.getCategoria().toUpperCase());

        Chamado chamado = new Chamado();
        chamado.setTitulo(request.getTitulo());
        chamado.setDescricao(request.getDescricao());
        chamado.setUsuario(usuario);
        chamado.setStatus(status);
        chamado.setPrioridade(prioridade);
        chamado.setCategoria(categoria);
        chamado.setCreatedAt(LocalDateTime.now());
        chamado.setUpdatedAt(LocalDateTime.now());
        chamado.setSlaInicio(LocalDateTime.now());

        Chamado salvo = chamadoRepository.save(chamado);

        historicoService.registrar(
                salvo,
                usuario,
                "Chamado criado",
                TipoAlteracao.OUTRO
        );

        return mapToResponse(salvo);
    }

    public List<ChamadoResponse> listarTodos() {
        return chamadoRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<ChamadoResponse> listarPorFiltro(ChamadoFiltroRequest filtro) {

        List<Chamado> chamados = chamadoRepository.findAll();

        if (filtro.getStatus() != null && !filtro.getStatus().isBlank()) {
            chamados = chamados.stream()
                    .filter(ch -> ch.getStatus() != null
                            && ch.getStatus().name().equalsIgnoreCase(filtro.getStatus()))
                    .toList();
        }

        if (filtro.getPrioridade() != null && !filtro.getPrioridade().isBlank()) {
            chamados = chamados.stream()
                    .filter(ch -> ch.getPrioridade() != null
                            && ch.getPrioridade().name().equalsIgnoreCase(filtro.getPrioridade()))
                    .toList();
        }

        if (filtro.getTecnicoId() != null) {
            chamados = chamados.stream()
                    .filter(ch -> ch.getTecnico() != null
                            && ch.getTecnico().getId().equals(filtro.getTecnicoId()))
                    .toList();
        }

        if (filtro.getUsuarioId() != null) {
            chamados = chamados.stream()
                    .filter(ch -> ch.getUsuario() != null
                            && ch.getUsuario().getId().equals(filtro.getUsuarioId()))
                    .toList();
        }

        return chamados.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ChamadoResponse atualizarStatus(Long chamadoId, String novoStatus) {

        Chamado chamado = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Chamado não encontrado"));

        Status statusAntigo = chamado.getStatus();
        Status statusNovo = Status.valueOf(novoStatus.toUpperCase());

        if (statusAntigo == statusNovo) {
            return mapToResponse(chamado);
        }

        chamado.setStatus(statusNovo);
        chamado.setUpdatedAt(LocalDateTime.now());

        Chamado atualizado = chamadoRepository.save(chamado);

        historicoService.registrar(
                atualizado,
                chamado.getUsuario(),
                "Status alterado de " + statusAntigo + " para " + statusNovo,
                TipoAlteracao.STATUS
        );

        return mapToResponse(atualizado);
    }

    // NOVO: fechar chamado com solução obrigatória
    public ChamadoResponse fecharChamado(Long chamadoId, FecharChamadoRequest request) {

        Chamado chamado = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Chamado não encontrado"));

        if (request.getDescricaoSolucao() == null || request.getDescricaoSolucao().isBlank()) {
            throw new IllegalArgumentException("Descrição da solução é obrigatória");
        }

        chamado.setDescricaoSolucao(request.getDescricaoSolucao());
        chamado.setStatus(Status.FECHADO);
        chamado.setDataFechamento(LocalDateTime.now());
        chamado.setUpdatedAt(LocalDateTime.now());

        Chamado atualizado = chamadoRepository.save(chamado);

        historicoService.registrar(
                atualizado,
                chamado.getUsuario(),
                "Chamado fechado com solução registrada",
                TipoAlteracao.STATUS
        );

        return mapToResponse(atualizado);
    }

    private ChamadoResponse mapToResponse(Chamado chamado) {
        return ChamadoResponse.builder()
                .id(chamado.getId())
                .titulo(chamado.getTitulo())
                .descricao(chamado.getDescricao())
                .descricaoSolucao(chamado.getDescricaoSolucao())
                .usuarioNome(
                        chamado.getUsuario() != null
                                ? chamado.getUsuario().getNome()
                                : null
                )
                .tecnicoNome(
                        chamado.getTecnico() != null
                                ? chamado.getTecnico().getNome()
                                : null
                )
                .status(
                        chamado.getStatus() != null
                                ? chamado.getStatus().name()
                                : null
                )
                .prioridade(
                        chamado.getPrioridade() != null
                                ? chamado.getPrioridade().name()
                                : null
                )
                .categoria(
                        chamado.getCategoria() != null
                                ? chamado.getCategoria().name()
                                : null
                )
                .slaInicio(chamado.getSlaInicio())
                .slaFim(chamado.getSlaFim())
                .dataFechamento(chamado.getDataFechamento())
                .createdAt(chamado.getCreatedAt())
                .updatedAt(chamado.getUpdatedAt())
                .build();
    }
}