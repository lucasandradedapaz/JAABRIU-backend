package jaabriu.jaabriu_backend.controller;

import jaabriu.jaabriu_backend.dto.HistoricoResponse;
import jaabriu.jaabriu_backend.service.HistoricoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/historicos") // 🔥 corrigido
public class HistoricoController {

    private final HistoricoService historicoService;

    public HistoricoController(HistoricoService historicoService) {
        this.historicoService = historicoService;
    }

    @GetMapping("/chamado/{id}")
    public List<HistoricoResponse> listarPorChamado(@PathVariable Long id) {
        return historicoService.listarPorChamado(id);
    }
}