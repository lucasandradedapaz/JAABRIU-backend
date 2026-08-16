package jaabriu.jaabriu_backend.service;

import jaabriu.jaabriu_backend.dto.LoginRequest;
import jaabriu.jaabriu_backend.security.CustomUserDetails;
import jaabriu.jaabriu_backend.security.CustomUserDetailsService;
import jaabriu.jaabriu_backend.security.JwtService;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       CustomUserDetailsService userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    public String authenticate(LoginRequest request) {

        // 🔐 autentica login
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.senha()
                )
        );

        // 🔥 CARREGA O USER CORRETO (CustomUserDetails)
        CustomUserDetails userDetails =
                (CustomUserDetails) userDetailsService.loadUserByUsername(request.email());

        // 🔥 GERA TOKEN COM ELE
        return jwtService.generateToken(userDetails);
    }
}