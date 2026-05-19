package jaabriu.jaabriu_backend.service;

import jaabriu.jaabriu_backend.dto.ComentarioRequest;
import jaabriu.jaabriu_backend.dto.ComentarioResponse;
import jaabriu.jaabriu_backend.entity.Chamado;
import jaabriu.jaabriu_backend.entity.Comentario;
import jaabriu.jaabriu_backend.entity.Usuario;
import jaabriu.jaabriu_backend.exception.ResourceNotFoundException;
import jaabriu.jaabriu_backend.repository.ChamadoRepository;
import jaabriu.jaabriu_backend.repository.ComentarioRepository;
import jaabriu.jaabriu_backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ComentarioService {

    private final ComentarioRepository comentarioRepository;
    private final ChamadoRepository chamadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistoricoService historicoService;

    public ComentarioService(
            ComentarioRepository comentarioRepository,
            ChamadoRepository chamadoRepository,
            UsuarioRepository usuarioRepository,
            HistoricoService historicoService
    ) {
        this.comentarioRepository = comentarioRepository;
        this.chamadoRepository = chamadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.historicoService = historicoService;
    }

    public ComentarioResponse adicionarComentario(
            Long chamadoId,
            Long usuarioId,
            ComentarioRequest request
    ) {

        Chamado chamado = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Chamado não encontrado"));

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        Comentario comentario = new Comentario();
        comentario.setMensagem(request.getMensagem());
        comentario.setInterno(Boolean.TRUE.equals(request.getInterno()));
        comentario.setChamado(chamado);
        comentario.setAutor(usuario);
        comentario.setCreatedAt(LocalDateTime.now());

        Comentario salvo = comentarioRepository.save(comentario);

        historicoService.registrar(
                chamado,
                usuario,
                "Comentário adicionado ao chamado"
        );

        return mapToResponse(salvo);
    }

    public List<ComentarioResponse> listarPorChamado(Long chamadoId) {

        Chamado chamado = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Chamado não encontrado"));

        return comentarioRepository.findByChamadoOrderByCreatedAtDesc(chamado)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private ComentarioResponse mapToResponse(Comentario comentario) {
        return ComentarioResponse.builder()
                .id(comentario.getId())
                .mensagem(comentario.getMensagem())
                .interno(comentario.getInterno())
                .autorId(comentario.getAutor().getId())
                .autorNome(comentario.getAutor().getNome())
                .chamadoId(comentario.getChamado().getId())
                .createdAt(comentario.getCreatedAt())
                .build();
    }
}