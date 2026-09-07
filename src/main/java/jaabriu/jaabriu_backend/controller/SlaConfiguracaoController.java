package jaabriu.jaabriu_backend.controller;

import jakarta.validation.Valid;
import jaabriu.jaabriu_backend.dto.SlaConfiguracaoRequest;
import jaabriu.jaabriu_backend.dto.SlaConfiguracaoResponse;
import jaabriu.jaabriu_backend.entity.Prioridade;
import jaabriu.jaabriu_backend.service.SlaConfiguracaoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sla")
public class SlaConfiguracaoController {

    private final SlaConfiguracaoService slaConfiguracaoService;

    public SlaConfiguracaoController(SlaConfiguracaoService slaConfiguracaoService) {
        this.slaConfiguracaoService = slaConfiguracaoService;
    }

    // Qualquer usuário autenticado pode ver os prazos (precisa disso pra
    // entender o contador na tela do chamado)
    @GetMapping
    public List<SlaConfiguracaoResponse> listar() {
        return slaConfiguracaoService.listarTodas();
    }

    // Só admin edita os prazos padrão da organização
    @PutMapping("/{prioridade}")
    @PreAuthorize("hasRole('ADMIN')")
    public SlaConfiguracaoResponse atualizar(
            @PathVariable String prioridade,
            @Valid @RequestBody SlaConfiguracaoRequest request
    ) {
        return slaConfiguracaoService.atualizar(
                Prioridade.valueOf(prioridade.toUpperCase()),
                request
        );
    }
}
