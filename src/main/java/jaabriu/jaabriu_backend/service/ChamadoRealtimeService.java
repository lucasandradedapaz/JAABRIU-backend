package jaabriu.jaabriu_backend.service;

import jaabriu.jaabriu_backend.dto.ChamadoResponse;
import jaabriu.jaabriu_backend.entity.Chamado;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Transmite eventos de chamado (criado, status alterado, prioridade
 * definida, fechado, etc.) em tempo real via WebSocket, sem precisar de F5.
 *
 * IMPORTANTE — por que mudou de convertAndSendToUser pra convertAndSend:
 * a versão anterior mandava só pra fila privada de cada técnico/admin
 * (convertAndSendToUser), que depende do Spring conseguir mapear
 * corretamente usuário -> sessão STOMP. Esse mapeamento é mais frágil e,
 * na prática, não estava entregando a lista atualizada de forma
 * confiável. Trocado por um tópico simples ("/topic/chamados/equipe"),
 * que é o mecanismo mais básico e confiável do STOMP: todo cliente que
 * assinar esse tópico recebe a mensagem, sem depender de resolução de
 * usuário/sessão.
 *
 * Segurança: só técnico/admin assinam esse tópico no frontend (usuário
 * comum nunca chama esse subscribe). Continua existindo a fila privada
 * por usuário pro dono do chamado, que é o único lugar onde o usuário
 * comum recebe atualização — nunca de chamados de outra pessoa.
 */
@Service
public class ChamadoRealtimeService {

    private final SimpMessagingTemplate messagingTemplate;

    public ChamadoRealtimeService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void transmitir(Chamado chamado, ChamadoResponse response, String tipoEvento) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tipo", tipoEvento); // "CHAMADO_CRIADO" | "CHAMADO_ATUALIZADO"
        payload.put("chamado", response);

        System.out.println(
                "[REALTIME] Transmitindo " + tipoEvento
                        + " do chamado #" + chamado.getId()
        );

        // 1) Quem estiver com a TELA DE DETALHES desse chamado aberta.
        messagingTemplate.convertAndSend(
                "/topic/chamados/" + chamado.getId() + "/eventos",
                payload
        );

        // 2) Técnico/admin, pra manter a LISTA sempre atualizada — tópico
        //    público simples, o mecanismo mais confiável do STOMP.
        messagingTemplate.convertAndSend("/topic/chamados/equipe", payload);

        // 3) O dono do chamado — só o dele, fila privada (igual às
        //    notificações), preservando a regra de que usuário comum só
        //    vê os próprios chamados.
        if (chamado.getUsuario() != null) {
            messagingTemplate.convertAndSendToUser(
                    chamado.getUsuario().getEmail(),
                    "/queue/chamados",
                    payload
            );
        }
    }
}

