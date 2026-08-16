// =====================================================
// DTO LOGIN REQUEST
// =====================================================
package jaabriu.jaabriu_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @Email @NotBlank String email,
    @NotBlank String senha
) {}