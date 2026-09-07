package jaabriu.jaabriu_backend.service;

import jaabriu.jaabriu_backend.dto.SlaConfiguracaoRequest;
import jaabriu.jaabriu_backend.dto.SlaConfiguracaoResponse;
import jaabriu.jaabriu_backend.entity.Prioridade;
import jaabriu.jaabriu_backend.entity.SlaConfiguracao;
import jaabriu.jaabriu_backend.repository.SlaConfiguracaoRepository;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class SlaConfiguracaoService {

    private final SlaConfiguracaoRepository slaConfiguracaoRepository;

    // Padrões pedidos: P1 Crítico=URGENTE, P2 Alto=ALTA, P3 Médio=MEDIA, P4 Baixo=BAIXA
    private static final Map<Prioridade, int[]> PADROES = new EnumMap<>(Prioridade.class);

    static {
        // { tempoRespostaMinutos, tempoResolucaoMinutos }
        PADROES.put(Prioridade.URGENTE, new int[]{15, 4 * 60});
        PADROES.put(Prioridade.ALTA, new int[]{60, 8 * 60});
        PADROES.put(Prioridade.MEDIA, new int[]{4 * 60, 24 * 60});
        PADROES.put(Prioridade.BAIXA, new int[]{24 * 60, 72 * 60});
    }

    public SlaConfiguracaoService(SlaConfiguracaoRepository slaConfiguracaoRepository) {
        this.slaConfiguracaoRepository = slaConfiguracaoRepository;
    }

    /**
     * Garante que existe uma configuração de SLA salva para a prioridade.
     * Se não existir ainda no banco, cria com o valor padrão na hora
     * (lazy init) — assim não depende de nenhum script de migração.
     */
    public SlaConfiguracao obterOuCriarPadrao(Prioridade prioridade) {
        return slaConfiguracaoRepository.findByPrioridade(prioridade)
                .orElseGet(() -> {
                    int[] padrao = PADROES.get(prioridade);
                    SlaConfiguracao nova = SlaConfiguracao.builder()
                            .prioridade(prioridade)
                            .tempoRespostaMinutos(padrao[0])
                            .tempoResolucaoMinutos(padrao[1])
                            .build();
                    return slaConfiguracaoRepository.save(nova);
                });
    }

    public int minutosResolucaoPara(Prioridade prioridade) {
        return obterOuCriarPadrao(prioridade).getTempoResolucaoMinutos();
    }

    public List<SlaConfiguracaoResponse> listarTodas() {
        return List.of(Prioridade.values()).stream()
                .map(p -> mapToResponse(obterOuCriarPadrao(p)))
                .toList();
    }

    public SlaConfiguracaoResponse atualizar(Prioridade prioridade, SlaConfiguracaoRequest request) {
        SlaConfiguracao configuracao = obterOuCriarPadrao(prioridade);
        configuracao.setTempoRespostaMinutos(request.getTempoRespostaMinutos());
        configuracao.setTempoResolucaoMinutos(request.getTempoResolucaoMinutos());
        SlaConfiguracao salva = slaConfiguracaoRepository.save(configuracao);
        return mapToResponse(salva);
    }

    private SlaConfiguracaoResponse mapToResponse(SlaConfiguracao configuracao) {
        return SlaConfiguracaoResponse.builder()
                .prioridade(configuracao.getPrioridade().name())
                .tempoRespostaMinutos(configuracao.getTempoRespostaMinutos())
                .tempoResolucaoMinutos(configuracao.getTempoResolucaoMinutos())
                .build();
    }
}
