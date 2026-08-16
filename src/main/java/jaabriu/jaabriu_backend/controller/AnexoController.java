package jaabriu.jaabriu_backend.controller;

import jaabriu.jaabriu_backend.dto.AnexoResponse;
import jaabriu.jaabriu_backend.entity.Anexo;
import jaabriu.jaabriu_backend.service.AnexoService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/anexos")
public class AnexoController {

    private final AnexoService anexoService;

    public AnexoController(AnexoService anexoService) {
        this.anexoService = anexoService;
    }

    @PostMapping(value = "/upload/{chamadoId}/{usuarioId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'TECNICO', 'USUARIO')")
    public ResponseEntity<AnexoResponse> upload(
            @PathVariable Long chamadoId,
            @PathVariable Long usuarioId,
            @RequestParam("arquivo") MultipartFile arquivo
    ) {
        return ResponseEntity.ok(anexoService.upload(chamadoId, usuarioId, arquivo));
    }

    @GetMapping("/chamado/{chamadoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECNICO', 'USUARIO')")
    public ResponseEntity<List<AnexoResponse>> listarPorChamado(
            @PathVariable Long chamadoId
    ) {
        return ResponseEntity.ok(anexoService.listarPorChamado(chamadoId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECNICO', 'USUARIO')")
    public ResponseEntity<Resource> download(@PathVariable Long id) {

        Anexo anexo = anexoService.buscarEntidade(id);
        Resource resource = anexoService.baixar(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(anexo.getTipoArquivo()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + anexo.getNomeArquivo() + "\""
                )
                .body(resource);
    }
}