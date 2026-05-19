package jaabriu.jaabriu_backend.controller;

import jakarta.validation.Valid;
import jaabriu.jaabriu_backend.dto.ChamadoFiltroRequest;
import jaabriu.jaabriu_backend.dto.ChamadoRequest;
import jaabriu.jaabriu_backend.dto.ChamadoResponse;
import jaabriu.jaabriu_backend.dto.FecharChamadoRequest;
import jaabriu.jaabriu_backend.dto.HistoricoResponse;
import jaabriu.jaabriu_backend.security.CustomUserDetails;
import jaabriu.jaabriu_backend.service.ChamadoService;
import jaabriu.jaabriu_backend.service.HistoricoService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChamadoResponse criar(@Valid @RequestBody ChamadoRequest request) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        CustomUserDetails usuarioLogado =
                (CustomUserDetails) authentication.getPrincipal();

        return chamadoService.criar(usuarioLogado.getId(), request);
    }

    @GetMapping
    public List<ChamadoResponse> listarTodos() {
        return chamadoService.listarTodos();
    }

    @GetMapping("/{id}/historico")
    public List<HistoricoResponse> listarHistorico(@PathVariable Long id) {
        return historicoService.listarPorChamado(id);
    }

    @PostMapping("/filtros")
    public List<ChamadoResponse> filtrar(
            @RequestBody ChamadoFiltroRequest filtro
    ) {
        return chamadoService.listarPorFiltro(filtro);
    }

    // 🔥 NOVO ENDPOINT
    @PutMapping("/{id}/status")
    public ChamadoResponse atualizarStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        return chamadoService.atualizarStatus(id, status);
    }


    @PutMapping("/{id}/fechar")
public ResponseEntity<ChamadoResponse> fecharChamado(
        @PathVariable Long id,
        @Valid @RequestBody FecharChamadoRequest request
) {
    return ResponseEntity.ok(chamadoService.fecharChamado(id, request));
}
}

