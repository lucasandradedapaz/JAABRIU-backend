package jaabriu.jaabriu_backend.controller;

import jakarta.validation.Valid;
import jaabriu.jaabriu_backend.dto.AdicionarSolucaoRequest;
import jaabriu.jaabriu_backend.dto.AtribuirSetorRequest;
import jaabriu.jaabriu_backend.dto.AtribuirTecnicoRequest;
import jaabriu.jaabriu_backend.dto.ChamadoFiltroRequest;
import jaabriu.jaabriu_backend.dto.ChamadoRequest;
import jaabriu.jaabriu_backend.dto.ChamadoResponse;
import jaabriu.jaabriu_backend.dto.DefinirPrioridadeRequest;
import jaabriu.jaabriu_backend.dto.EditarChamadoRequest;
import jaabriu.jaabriu_backend.dto.HistoricoResponse;
import jaabriu.jaabriu_backend.dto.SolucaoResponse;
import jaabriu.jaabriu_backend.security.CustomUserDetails;
import jaabriu.jaabriu_backend.service.ChamadoService;
import jaabriu.jaabriu_backend.service.HistoricoService;

import org.springframework.http.HttpStatus;
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

    // ================== ATRIBUIÇÃO DE SETOR E TÉCNICOS ==================
    // Só técnico/admin atribuem setor/técnicos (item 1.1 do pedido) — a
    // regra é aplicada aqui E de novo dentro do service (defesa em
    // profundidade, não é só esconder botão no frontend).

    @PutMapping("/{id}/setor")
    @PreAuthorize("hasAnyRole('TECNICO','ADMIN')")
    public ChamadoResponse atribuirSetor(
            @PathVariable Long id,
            @Valid @RequestBody AtribuirSetorRequest request,
            @AuthenticationPrincipal CustomUserDetails usuarioLogado
    ) {
        return chamadoService.atribuirSetor(id, request, usuarioLogado.getId());
    }

    @PostMapping("/{id}/tecnicos")
    @PreAuthorize("hasAnyRole('TECNICO','ADMIN')")
    public ChamadoResponse adicionarTecnico(
            @PathVariable Long id,
            @Valid @RequestBody AtribuirTecnicoRequest request,
            @AuthenticationPrincipal CustomUserDetails usuarioLogado
    ) {
        return chamadoService.adicionarTecnico(id, request, usuarioLogado.getId());
    }

    @DeleteMapping("/{id}/tecnicos/{tecnicoId}")
    @PreAuthorize("hasAnyRole('TECNICO','ADMIN')")
    public ChamadoResponse removerTecnico(
            @PathVariable Long id,
            @PathVariable Long tecnicoId,
            @AuthenticationPrincipal CustomUserDetails usuarioLogado
    ) {
        return chamadoService.removerTecnico(id, tecnicoId, usuarioLogado.getId());
    }

    // ================== HISTÓRICO DE SOLUÇÕES ==================

    // Só técnico/admin registram solução (item 8 do pedido)
    @PostMapping("/{id}/solucoes")
    @PreAuthorize("hasAnyRole('TECNICO','ADMIN')")
    public ChamadoResponse adicionarSolucao(
            @PathVariable Long id,
            @Valid @RequestBody AdicionarSolucaoRequest request,
            @AuthenticationPrincipal CustomUserDetails usuarioLogado
    ) {
        return chamadoService.adicionarSolucao(id, request, usuarioLogado.getId());
    }

    // Qualquer perfil autenticado pode ver — usuário comum só o próprio
    // chamado (checagem de posse feita dentro do service).
    @GetMapping("/{id}/solucoes")
    public List<SolucaoResponse> listarSolucoes(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails usuarioLogado
    ) {
        return chamadoService.listarSolucoes(id, usuarioLogado.getId(), usuarioLogado.getPerfil());
    }

    // Só admin pode editar título/descrição (técnico não edita os dados
    // originais do chamado que o usuário criou)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ChamadoResponse editar(
            @PathVariable Long id,
            @Valid @RequestBody EditarChamadoRequest request,
            @AuthenticationPrincipal CustomUserDetails usuarioLogado
    ) {
        return chamadoService.editar(id, request, usuarioLogado.getId());
    }

    // Só técnico/admin definem a prioridade (chamado aberto por usuário
    // comum nasce sem prioridade, aguardando essa triagem)
    @PutMapping("/{id}/prioridade")
    @PreAuthorize("hasAnyRole('TECNICO','ADMIN')")
    public ChamadoResponse definirPrioridade(
            @PathVariable Long id,
            @Valid @RequestBody DefinirPrioridadeRequest request,
            @AuthenticationPrincipal CustomUserDetails usuarioLogado
    ) {
        return chamadoService.definirPrioridade(id, request, usuarioLogado.getId());
    }

    // Só admin pode excluir
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        chamadoService.excluir(id);
    }
}
