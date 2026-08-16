package jaabriu.jaabriu_backend.config;

import jaabriu.jaabriu_backend.entity.Usuario;
import jaabriu.jaabriu_backend.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Cria um usuário ADMIN padrão automaticamente na primeira vez que o
 * backend sobe, caso a tabela de usuários esteja vazia.
 *
 * Isso resolve o problema de "ovo e galinha": como criar usuários agora
 * exige estar logado como ADMIN, sem isso ninguém conseguiria criar o
 * primeiro administrador do sistema.
 *
 * Credenciais padrão (troque a senha assim que logar pela primeira vez):
 *   email: admin@jaabriu.com
 *   senha: admin123
 */
@Component
public class AdminInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            return;
        }

        Usuario admin = Usuario.builder()
                .nome("Administrador")
                .email("admin@jaabriu.com")
                .senha(passwordEncoder.encode("admin123"))
                .perfil(Usuario.Perfil.ADMIN)
                .ativo(true)
                .build();

        usuarioRepository.save(admin);

        System.out.println("==================================================");
        System.out.println(" Usuário ADMIN padrão criado (banco estava vazio)");
        System.out.println(" Email: admin@jaabriu.com");
        System.out.println(" Senha: admin123");
        System.out.println(" -> Troque essa senha assim que fizer login!");
        System.out.println("==================================================");
    }
}