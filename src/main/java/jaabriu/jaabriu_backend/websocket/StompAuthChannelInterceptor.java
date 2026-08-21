package jaabriu.jaabriu_backend.websocket;

import jaabriu.jaabriu_backend.security.CustomUserDetails;
import jaabriu.jaabriu_backend.security.CustomUserDetailsService;
import jaabriu.jaabriu_backend.security.JwtService;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Autentica a conexão WebSocket/STOMP usando o mesmo JWT já usado na API REST.
 *
 * O client manda o token no header STOMP "Authorization: Bearer xxx" durante
 * o CONNECT. Aqui validamos exatamente como o JwtAuthenticationFilter faz
 * pra requisições HTTP normais, e associamos o usuário autenticado à sessão
 * WebSocket (accessor.setUser).
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public StompAuthChannelInterceptor(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String jwt = authHeader.substring(7);
                    String email = jwtService.extractUsername(jwt);

                    CustomUserDetails userDetails =
                            (CustomUserDetails) userDetailsService.loadUserByUsername(email);

                    if (jwtService.isTokenValid(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );
                        accessor.setUser(authToken);
                    }
                } catch (Exception e) {
                    System.out.println("Erro ao autenticar WebSocket: " + e.getMessage());
                }
            }
        }

        return message;
    }
}
