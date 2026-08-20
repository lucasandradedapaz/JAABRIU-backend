package jaabriu.jaabriu_backend.controller;

import jakarta.validation.Valid;
import jaabriu.jaabriu_backend.dto.ComentarioRequest;
import jaabriu.jaabriu_backend.dto.ComentarioResponse;
import jaabriu.jaabriu_backend.security.CustomUserDetails;
import jaabriu.jaabriu_backend.service.ComentarioService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chamados/{chamadoId}/comentarios")
public class ComentarioController {

    private final ComentarioService comentarioService;

    public ComentarioController(ComentarioService comentarioService) {
        this.comentarioService = comentarioService;
    }

    // Usuário comum, técnico e admin podem comentar — todo mundo que
    // participa do atendimento precisa conseguir conversar.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('USUARIO','TECNICO','ADMIN')")
    public ComentarioResponse adicionarComentario(
            @PathVariable Long chamadoId,
            @AuthenticationPrincipal CustomUserDetails usuarioLogado,
            @Valid @RequestBody ComentarioRequest request
    ) {
        return comentarioService.adicionarComentario(
                chamadoId,
                usuarioLogado.getId(),
                request
        );
    }

    @GetMapping
    public List<ComentarioResponse> listarComentarios(
            @PathVariable Long chamadoId
    ) {
        return comentarioService.listarPorChamado(chamadoId);
    }
}