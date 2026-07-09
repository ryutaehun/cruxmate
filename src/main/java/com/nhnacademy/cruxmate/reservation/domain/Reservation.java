package com.nhnacademy.cruxmate.reservation.domain;

import com.nhnacademy.cruxmate.member.domain.Member;
import com.nhnacademy.cruxmate.session.domain.ClimbingSession;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "reservation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "participant_count", nullable = false)
    private int participantCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ClimbingSession session;

    @PrePersist
    private void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    private Reservation(
            Member member,
            ClimbingSession session,
            int participantCount
    ) {
        validateBasicInfo(member, session);
        validateParticipantCount(participantCount);

        this.member = member;
        this.session = session;
        this.participantCount = participantCount;
        this.status = ReservationStatus.CONFIRMED;
    }

    public static Reservation create(
            Member member,
            ClimbingSession session,
            int participantCount
    ) {
        return new Reservation(member, session, participantCount);
    }

    private static void validateParticipantCount(int participantCount) {
        if (participantCount < 1 || participantCount > 4) {
            throw new IllegalArgumentException(
                    "참여 인원은 1명 이상 4명 이하여야 합니다."
            );
        }
    }
    private static void validateBasicInfo(Member member, ClimbingSession session){
        if (member == null) {
            throw new IllegalArgumentException("예약 회원은 필수입니다.");
        }
        if (session == null) {
            throw new IllegalArgumentException("예약 세션은 필수입니다.");
        }
    }

    public void cancel(LocalDateTime now){
        if(this.status == ReservationStatus.CANCELED){
            throw new IllegalStateException("이미 취소된 예약입니다.");
        }
        this.status = ReservationStatus.CANCELED;
        this.canceledAt = now;
    }
}