
package jaabriu.jaabriu_backend.service;

import jaabriu.jaabriu_backend.dto.UsuarioRequest;
import jaabriu.jaabriu_backend.dto.UsuarioResponse;
import jaabriu.jaabriu_backend.entity.Setor;
import jaabriu.jaabriu_backend.entity.Usuario;
import jaabriu.jaabriu_backend.exception.BusinessException;
import jaabriu.jaabriu_backend.exception.ResourceNotFoundException;
import jaabriu.jaabriu_backend.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UsuarioResponse> listarTodos() {
        return usuarioRepository.findAll()
                .stream()
                .map(this::converterParaResponse)
                .toList();
    }

    // NOVO: lista técnicos ativos, usada no seletor de "técnico que auxiliou"
    public List<UsuarioResponse> listarTecnicos() {
        return usuarioRepository.findAll()
                .stream()
                .filter(u -> u.getPerfil() == Usuario.Perfil.TECNICO && Boolean.TRUE.equals(u.getAtivo()))
                .map(this::converterParaResponse)
                .toList();
    }

    public UsuarioResponse buscarPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        return converterParaResponse(usuario);
    }

    public UsuarioResponse criar(UsuarioRequest request) {

        if (request.getSenha() == null || request.getSenha().isBlank()) {
            throw new BusinessException("Senha é obrigatória para criar um usuário.");
        }

        if (usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessException("Já existe um usuário cadastrado com esse email.");
        }

        Usuario usuario = Usuario.builder()
                .nome(request.getNome())
                .email(request.getEmail())
                .senha(passwordEncoder.encode(request.getSenha()))
                .perfil(Usuario.Perfil.valueOf(request.getPerfil().toUpperCase()))
                .setor(parseSetor(request.getSetor()))
                .ativo(true)
                .build();

        Usuario salvo = usuarioRepository.save(usuario);

        return converterParaResponse(salvo);
    }

    public UsuarioResponse atualizar(Long id, UsuarioRequest request) {

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        usuario.setNome(request.getNome());
        usuario.setEmail(request.getEmail());
        usuario.setPerfil(Usuario.Perfil.valueOf(request.getPerfil().toUpperCase()));
        usuario.setSetor(parseSetor(request.getSetor()));

        if (request.getSenha() != null && !request.getSenha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        }

        Usuario atualizado = usuarioRepository.save(usuario);

        return converterParaResponse(atualizado);
    }

    // NOVO: usuário comum define/altera o próprio setor (sem precisar de admin)
    public UsuarioResponse atualizarMeuSetor(Long usuarioId, String setor) {

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        usuario.setSetor(parseSetor(setor));

        Usuario atualizado = usuarioRepository.save(usuario);

        return converterParaResponse(atualizado);
    }

    private Setor parseSetor(String setor) {
        if (setor == null || setor.isBlank()) {
            return null;
        }
        try {
            return Setor.valueOf(setor.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Setor inválido. Use GEAS, OBRAS ou SERVICOS_PUBLICOS.");
        }
    }

    public void desativar(Long id) {

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        usuario.setAtivo(false);

        usuarioRepository.save(usuario);
    }

    // NOVO: reverte a desativação
    public UsuarioResponse reativar(Long id) {

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        usuario.setAtivo(true);

        Usuario atualizado = usuarioRepository.save(usuario);

        return converterParaResponse(atualizado);
    }

    private UsuarioResponse converterParaResponse(Usuario usuario) {
        return UsuarioResponse.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .perfil(usuario.getPerfil().name())
                .ativo(usuario.getAtivo())
                .setor(usuario.getSetor() != null ? usuario.getSetor().name() : null)
                .build();
    }
}

