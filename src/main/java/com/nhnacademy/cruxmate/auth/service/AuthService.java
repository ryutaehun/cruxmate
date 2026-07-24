package com.nhnacademy.cruxmate.auth.service;

import com.nhnacademy.cruxmate.auth.dto.AuthenticatedMember;
import com.nhnacademy.cruxmate.auth.dto.LoginResponse;
import com.nhnacademy.cruxmate.common.security.CustomUserPrincipal;
import com.nhnacademy.cruxmate.common.security.jwt.JwtProperties;
import com.nhnacademy.cruxmate.common.security.jwt.JwtTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;

    public AuthenticatedMember authenticate(
            String email,
            String password
    ){
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, password)
        );

        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();

        return new AuthenticatedMember(
                principal.memberId(),
                principal.email(),
                principal.role()
        );
    }

    public LoginResponse login(
            String email, String password
    ){
        AuthenticatedMember member = authenticate(email, password);

        String accessToken = jwtTokenService.issueAccessToken(member);

        return new LoginResponse(accessToken, "Bearer", jwtProperties.accessTokenExpiration().toSeconds());
    }
}
