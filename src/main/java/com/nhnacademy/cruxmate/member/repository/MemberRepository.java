package com.nhnacademy.cruxmate.member.repository;

import com.nhnacademy.cruxmate.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByEmail(String email);
}
