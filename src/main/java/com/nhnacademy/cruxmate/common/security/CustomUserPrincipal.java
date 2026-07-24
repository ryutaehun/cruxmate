package com.nhnacademy.cruxmate.common.security;

import com.nhnacademy.cruxmate.member.domain.Member;
import com.nhnacademy.cruxmate.member.domain.MemberRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record CustomUserPrincipal (
        Long memberId,
        String email,
        String passwordHash,
        MemberRole role
) implements UserDetails {

    public static CustomUserPrincipal from(Member member){
        return new CustomUserPrincipal(
                member.getId(),
                member.getEmail(),
                member.getPasswordHash(),
                member.getRole()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + role.name())
        );
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

}
