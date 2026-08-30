package jaabriu.jaabriu_backend.controller;

import jaabriu.jaabriu_backend.dto.NotificacaoResponse;
import jaabriu.jaabriu_backend.security.CustomUserDetails;
import jaabriu.jaabriu_backend.service.NotificacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notificacoes")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    public NotificacaoController(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @GetMapping
    public List<NotificacaoResponse> listar(
            @AuthenticationPrincipal CustomUserDetails usuarioLogado
    ) {
        return notificacaoService.listarRecentes(usuarioLogado.getId());
    }

    @GetMapping("/nao-lidas/contagem")
    public ResponseEntity<Map<String, Long>> contarNaoLidas(
            @AuthenticationPrincipal CustomUserDetails usuarioLogado
    ) {
        Long contagem = notificacaoService.contarNaoLidas(usuarioLogado.getId());
        return ResponseEntity.ok(Map.of("total", contagem));
    }

    @PutMapping("/{id}/lida")
    public ResponseEntity<Void> marcarComoLida(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails usuarioLogado
    ) {
        notificacaoService.marcarComoLida(id, usuarioLogado.getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/lidas")
    public ResponseEntity<Void> marcarTodasComoLidas(
            @AuthenticationPrincipal CustomUserDetails usuarioLogado
    ) {
        notificacaoService.marcarTodasComoLidas(usuarioLogado.getId());
        return ResponseEntity.noContent().build();
    }
}
