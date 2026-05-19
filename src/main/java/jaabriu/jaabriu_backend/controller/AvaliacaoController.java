package jaabriu.jaabriu_backend.controller;

import jakarta.validation.Valid;
import jaabriu.jaabriu_backend.dto.AvaliacaoRequest;
import jaabriu.jaabriu_backend.dto.AvaliacaoResponse;
import jaabriu.jaabriu_backend.security.CustomUserDetails;
import jaabriu.jaabriu_backend.service.AvaliacaoService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chamados/{chamadoId}/avaliacao")
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    public AvaliacaoController(AvaliacaoService avaliacaoService) {
        this.avaliacaoService = avaliacaoService;
    }

 @PostMapping
@ResponseStatus(HttpStatus.CREATED)
public AvaliacaoResponse avaliar(
        @PathVariable Long chamadoId,
        @AuthenticationPrincipal CustomUserDetails usuarioLogado,
        @Valid @RequestBody AvaliacaoRequest request
) {
    System.out.println("====== DEBUG AVALIACAO ======");
    System.out.println("Chamado ID: " + chamadoId);
    System.out.println("Usuario logado: " + usuarioLogado);
    System.out.println("Request nota: " + request.getNota());
    System.out.println("Request comentario: " + request.getComentario());

    if (usuarioLogado == null) {
        throw new RuntimeException("USUARIO LOGADO ESTA NULL");
    }

    return avaliacaoService.avaliar(
            chamadoId,
            usuarioLogado.getId(),
            request
    );
}

    @GetMapping
    public AvaliacaoResponse buscar(
            @PathVariable Long chamadoId
    ) {
        return avaliacaoService.buscarPorChamado(chamadoId);
    }
}