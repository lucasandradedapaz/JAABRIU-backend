package jaabriu.jaabriu_backend.service;

import jaabriu.jaabriu_backend.dto.AnexoResponse;
import jaabriu.jaabriu_backend.entity.Anexo;
import jaabriu.jaabriu_backend.entity.Chamado;
import jaabriu.jaabriu_backend.entity.Usuario;
import jaabriu.jaabriu_backend.exception.ResourceNotFoundException;
import jaabriu.jaabriu_backend.repository.AnexoRepository;
import jaabriu.jaabriu_backend.repository.ChamadoRepository;
import jaabriu.jaabriu_backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AnexoService {

    private final AnexoRepository anexoRepository;
    private final ChamadoRepository chamadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final Path uploadPath;

    public AnexoService(
            AnexoRepository anexoRepository,
            ChamadoRepository chamadoRepository,
            UsuarioRepository usuarioRepository,
            @Value("${app.upload.dir:uploads}") String uploadDir
    ) {
        this.anexoRepository = anexoRepository;
        this.chamadoRepository = chamadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.uploadPath);
        } catch (Exception e) {
            throw new RuntimeException("Não foi possível criar a pasta de uploads.");
        }
    }

    public AnexoResponse upload(Long chamadoId, Long usuarioId, MultipartFile arquivo) {

        Chamado chamado = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Chamado não encontrado."));

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        String nomeOriginal = StringUtils.cleanPath(arquivo.getOriginalFilename());

        String extensao = "";

        int index = nomeOriginal.lastIndexOf('.');

        if (index > 0) {
            extensao = nomeOriginal.substring(index);
        }

        String nomeSalvo = UUID.randomUUID() + extensao;

        try {
            Path destino = this.uploadPath.resolve(nomeSalvo);

            Files.copy(
                    arquivo.getInputStream(),
                    destino,
                    StandardCopyOption.REPLACE_EXISTING
            );

            Anexo anexo = Anexo.builder()
                    .nomeArquivo(nomeOriginal)
                    .tipoArquivo(arquivo.getContentType())
                    .caminhoArquivo(destino.toString())
                    .tamanho(arquivo.getSize())
                    .dataUpload(LocalDateTime.now())
                    .chamado(chamado)
                    .usuario(usuario)
                    .build();

            Anexo salvo = anexoRepository.save(anexo);

            return AnexoResponse.builder()
                    .id(salvo.getId())
                    .nomeArquivo(salvo.getNomeArquivo())
                    .tipoArquivo(salvo.getTipoArquivo())
                    .tamanho(salvo.getTamanho())
                    .dataUpload(salvo.getDataUpload())
                    .chamadoId(chamado.getId())
                    .usuarioId(usuario.getId())
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao salvar arquivo.");
        }
    }

    public List<AnexoResponse> listarPorChamado(Long chamadoId) {
        return anexoRepository.findByChamadoId(chamadoId)
                .stream()
                .map(anexo -> AnexoResponse.builder()
                        .id(anexo.getId())
                        .nomeArquivo(anexo.getNomeArquivo())
                        .tipoArquivo(anexo.getTipoArquivo())
                        .tamanho(anexo.getTamanho())
                        .dataUpload(anexo.getDataUpload())
                        .chamadoId(anexo.getChamado().getId())
                        .usuarioId(anexo.getUsuario().getId())
                        .build())
                .toList();
    }

    public Resource baixar(Long id) {

        Anexo anexo = anexoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Anexo não encontrado."));

        try {
            Path path = Paths.get(anexo.getCaminhoArquivo()).normalize();
            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists()) {
                throw new ResourceNotFoundException("Arquivo não encontrado.");
            }

            return resource;

        } catch (MalformedURLException e) {
            throw new RuntimeException("Erro ao carregar arquivo.");
        }
    }

    public Anexo buscarEntidade(Long id) {
        return anexoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Anexo não encontrado."));
    }
}