package jaabriu.jaabriu_backend.controller;

import jaabriu.jaabriu_backend.dto.SetorOpcaoResponse;
import jaabriu.jaabriu_backend.entity.Setor;
import jaabriu.jaabriu_backend.util.TextoUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * NOVO: autocomplete de setor (item 1.2 do pedido). Reaproveita o mesmo
 * enum Setor já usado no cadastro de usuário — evita criar uma tabela
 * duplicada só pra isso.
 */
@RestController
@RequestMapping("/api/setores")
public class SetorController {

    // Só admin/técnico atribuem setor a um chamado.
    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('TECNICO','ADMIN')")
    public List<SetorOpcaoResponse> buscar(@RequestParam(required = false, defaultValue = "") String query) {
        String termo = TextoUtils.normalizar(query);

        return Arrays.stream(Setor.values())
                .filter(setor -> termo.isBlank()
                        || TextoUtils.normalizar(setor.getDescricao()).contains(termo)
                        || TextoUtils.normalizar(setor.name()).contains(termo))
                .map(setor -> SetorOpcaoResponse.builder()
                        .valor(setor.name())
                        .label(setor.getDescricao())
                        .build())
                .toList();
    }
}
