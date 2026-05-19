package jaabriu.jaabriu_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnexoResponse {

    private Long id;
    private String nomeArquivo;
    private String tipoArquivo;
    private Long tamanho;
    private LocalDateTime dataUpload;
    private Long chamadoId;
    private Long usuarioId;
}