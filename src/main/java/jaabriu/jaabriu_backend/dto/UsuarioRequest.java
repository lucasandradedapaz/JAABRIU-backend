package jaabriu.jaabriu_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioRequest {

    @NotBlank(message = "Nome é obrigatório.")
    @Size(min = 3, max = 120, message = "Nome deve ter entre 3 e 120 caracteres.")
    private String nome;

    @NotBlank(message = "Email é obrigatório.")
    @Email(message = "Email inválido.")
    private String email;

    // Obrigatória ao criar, opcional ao editar (deixe em branco pra manter a atual).
    // A obrigatoriedade na criação é validada no UsuarioService.
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres.")
    private String senha;

    @NotBlank(message = "Perfil é obrigatório.")
    private String perfil;

    // Opcional — setor do usuário comum (GEAS, OBRAS, SERVICOS_PUBLICOS)
    private String setor;
}