package com.nhnacademy.cruxmate.idempotency.domain;

import com.nhnacademy.cruxmate.member.domain.Member;
import com.nhnacademy.cruxmate.reservation.domain.Reservation;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "reservation_idempotency",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_reservation_idempotency_member_key",
                        columnNames = {"member_id", "idempotency_key"}
                )
        }
)
public class ReservationIdempotency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "request_hash", length = 64, nullable = false)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private IdempotencyStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @OneToOne
    @JoinColumn(name = "reservation_id", unique = true)
    private Reservation reservation;

    @PrePersist
    void onCreate(){
        this.createdAt = LocalDateTime.now();
    }

    public static ReservationIdempotency create(Member member, String idempotencyKey, String requestHash) {
        ReservationIdempotency idempotency = new ReservationIdempotency();

        idempotency.member = member;
        idempotency.idempotencyKey = idempotencyKey;
        idempotency.requestHash = requestHash;
        idempotency.status = IdempotencyStatus.PROCESSING;

        return idempotency;
    }

    public void complete(Reservation reservation, LocalDateTime now){
        if(this.status != IdempotencyStatus.PROCESSING){
            throw new IllegalStateException("이미 완료된 멱등성 요청입니다.");
        }

        this.reservation = reservation;
        this.status = IdempotencyStatus.COMPLETED;
        this.completedAt = now;
    }
}
