package jaabriu.jaabriu_backend.controller;

import jakarta.validation.Valid;
import jaabriu.jaabriu_backend.dto.ChamadoFiltroRequest;
import jaabriu.jaabriu_backend.dto.ChamadoRequest;
import jaabriu.jaabriu_backend.dto.ChamadoResponse;
import jaabriu.jaabriu_backend.dto.EditarChamadoRequest;
import jaabriu.jaabriu_backend.dto.FecharChamadoRequest;
import jaabriu.jaabriu_backend.dto.HistoricoResponse;
import jaabriu.jaabriu_backend.security.CustomUserDetails;
import jaabriu.jaabriu_backend.service.ChamadoService;
import jaabriu.jaabriu_backend.service.HistoricoService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chamados")
public class ChamadoController {

    private final ChamadoService chamadoService;
    private final HistoricoService historicoService;

    public ChamadoController(
            ChamadoService chamadoService,
            HistoricoService historicoService
    ) {
        this.chamadoService = chamadoService;
        this.historicoService = historicoService;
    }

    // Qualquer perfil autenticado pode abrir um chamado.
    // Prioridade/status são resolvidos no service conforme o perfil.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChamadoResponse criar(
            @Valid @RequestBody ChamadoRequest request,
            @AuthenticationPrincipal CustomUserDetails usuarioLogado
    ) {
        return chamadoService.criar(usuarioLogado.getId(), request);
    }

    // Usuário comum só vê os próprios chamados; técnico/admin veem todos
    // (regra aplicada dentro do service).
    @GetMapping
    public List<ChamadoResponse> listarTodos(
            @AuthenticationPrincipal CustomUserDetails usuarioLogado
    ) {
        return chamadoService.listarTodos(
                usuarioLogado.getId(),
                usuarioLogado.getPerfil()
        );
    }

    @GetMapping("/{id}/historico")
    public List<HistoricoResponse> listarHistorico(@PathVariable Long id) {
        return historicoService.listarPorChamado(id);
    }

    @PostMapping("/filtros")
    public List<ChamadoResponse> filtrar(
            @RequestBody ChamadoFiltroRequest filtro,
            @AuthenticationPrincipal CustomUserDetails usuarioLogado
    ) {
        return chamadoService.listarPorFiltro(
                filtro,
                usuarioLogado.getId(),
                usuarioLogado.getPerfil()
        );
    }

    // Só técnico/admin podem mover o status do chamado (resolver, reabrir etc.)
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('TECNICO','ADMIN')")
    public ChamadoResponse atualizarStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @AuthenticationPrincipal CustomUserDetails usuarioLogado
    ) {
        return chamadoService.atualizarStatus(id, status, usuarioLogado.getId());
    }

    // Só técnico/admin podem fechar um chamado
    @PutMapping("/{id}/fechar")
    @PreAuthorize("hasAnyRole('TECNICO','ADMIN')")
    public ResponseEntity<ChamadoResponse> fecharChamado(
            @PathVariable Long id,
            @Valid @RequestBody FecharChamadoRequest request,
            @AuthenticationPrincipal CustomUserDetails usuarioLogado
    ) {
        return ResponseEntity.ok(
                chamadoService.fecharChamado(id, request, usuarioLogado.getId())
        );
    }

    // Só técnico/admin podem editar título/descrição
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TECNICO','ADMIN')")
    public ChamadoResponse editar(
            @PathVariable Long id,
            @Valid @RequestBody EditarChamadoRequest request,
            @AuthenticationPrincipal CustomUserDetails usuarioLogado
    ) {
        return chamadoService.editar(id, request, usuarioLogado.getId());
    }

    // Só técnico/admin podem excluir
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TECNICO','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        chamadoService.excluir(id);
    }
}
