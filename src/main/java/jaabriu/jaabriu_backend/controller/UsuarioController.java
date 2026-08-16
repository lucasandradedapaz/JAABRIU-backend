package jaabriu.jaabriu_backend.controller;

import jakarta.validation.Valid;
import jaabriu.jaabriu_backend.dto.SetorRequest;
import jaabriu.jaabriu_backend.dto.UsuarioRequest;
import jaabriu.jaabriu_backend.dto.UsuarioResponse;
import jaabriu.jaabriu_backend.security.CustomUserDetails;
import jaabriu.jaabriu_backend.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // Qualquer usuário autenticado consegue ver os próprios dados
    // (usado pelo frontend pra saber nome/perfil de quem está logado).
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> meuPerfil(
            @AuthenticationPrincipal CustomUserDetails usuarioLogado
    ) {
        return ResponseEntity.ok(usuarioService.buscarPorId(usuarioLogado.getId()));
    }

    // NOVO: usuário comum define/altera o próprio setor, sem precisar de admin
    @PutMapping("/me/setor")
    public ResponseEntity<UsuarioResponse> atualizarMeuSetor(
            @AuthenticationPrincipal CustomUserDetails usuarioLogado,
            @Valid @RequestBody SetorRequest request
    ) {
        return ResponseEntity.ok(
                usuarioService.atualizarMeuSetor(usuarioLogado.getId(), request.getSetor())
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UsuarioResponse>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    // NOVO: lista técnicos disponíveis (usado no seletor de "técnico que auxiliou")
    @GetMapping("/tecnicos")
    @PreAuthorize("hasAnyRole('TECNICO','ADMIN')")
    public ResponseEntity<List<UsuarioResponse>> listarTecnicos() {
        return ResponseEntity.ok(usuarioService.listarTecnicos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECNICO')")
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> criar(
            @Valid @RequestBody UsuarioRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(usuarioService.criar(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioRequest request
    ) {
        return ResponseEntity.ok(usuarioService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        usuarioService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    // NOVO: reativar usuário desativado
    @PutMapping("/{id}/reativar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> reativar(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.reativar(id));
    }
}