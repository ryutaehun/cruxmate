package com.nhnacademy.cruxmate.session.repository;

import com.nhnacademy.cruxmate.session.domain.ClimbingSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClimbingSessionRepository extends JpaRepository<Long, ClimbingSession> {
}
