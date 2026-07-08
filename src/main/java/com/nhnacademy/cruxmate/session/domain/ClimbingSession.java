package com.nhnacademy.cruxmate.session.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "climbing_session")
public class ClimbingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String title;

    @Column(length = 100, nullable = false)
    private String location;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "reservation_open_at", nullable = false)
    private LocalDateTime reservationOpenAt;

    @Column(name = "reservation_close_at", nullable = false)
    private LocalDateTime reservationCloseAt;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "reserved_count", nullable = false)
    private int reservedCount;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClimbingSessionLevel level;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClimbingSessionStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    private void onCreate(){
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    private void onUpdate(){
        this.updatedAt = LocalDateTime.now();
    }

    public static ClimbingSession create(String title, String location, LocalDateTime startAt, LocalDateTime endAt,
                                  LocalDateTime reservationOpenAt, LocalDateTime reservationCloseAt, int capacity, ClimbingSessionLevel level){
        return new ClimbingSession(title, location, startAt, endAt, reservationOpenAt, reservationCloseAt, capacity, level);
    }

    private ClimbingSession(String title, String location, LocalDateTime startAt, LocalDateTime endAt,
                            LocalDateTime reservationOpenAt, LocalDateTime reservationCloseAt, int capacity, ClimbingSessionLevel level) {
        validateSchedule(startAt,endAt,reservationOpenAt,reservationCloseAt);
        validateBasicInfo(title, location, capacity, level);

        this.title = title;
        this.location = location;
        this.startAt = startAt;
        this.endAt = endAt;
        this.reservationOpenAt = reservationOpenAt;
        this.reservationCloseAt = reservationCloseAt;
        this.capacity = capacity;
        this.reservedCount = 0;
        this.level = level;
        this.status = ClimbingSessionStatus.SCHEDULED;
    }

    private static void validateSchedule(
            LocalDateTime startAt,
            LocalDateTime endAt,
            LocalDateTime reservationOpenAt,
            LocalDateTime reservationCloseAt
    ) {
        if (startAt == null
                || endAt == null
                || reservationOpenAt == null
                || reservationCloseAt == null) {
            throw new IllegalArgumentException("세션 시간은 모두 필수입니다.");
        }

        boolean valid =
                reservationOpenAt.isBefore(reservationCloseAt)
                        && !reservationCloseAt.isAfter(startAt)
                        && startAt.isBefore(endAt);

        if (!valid) {
            throw new IllegalArgumentException(
                    "예약 시작 < 예약 마감 <= 세션 시작 < 세션 종료 순서여야 합니다."
            );
        }
    }

    private static void validateBasicInfo(
            String title,
            String location,
            int capacity,
            ClimbingSessionLevel level
    ) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("세션 제목은 비어 있을 수 없습니다.");
        }

        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("세션 장소는 비어 있을 수 없습니다.");
        }

        if (capacity <= 0) {
            throw new IllegalArgumentException("세션 정원은 1명 이상이어야 합니다.");
        }

        if (level == null) {
            throw new IllegalArgumentException("세션 난이도는 필수입니다.");
        }
    }

    public void reserve(int participantCount, LocalDateTime now){
        if(participantCount < 1 || participantCount > 4){
            throw new IllegalArgumentException("참여 인원은 1명 이상 4명 이하여야 합니다.");
        }
        if(this.status != ClimbingSessionStatus.SCHEDULED){
            throw new IllegalArgumentException("취소되거나 마감된 세션입니다");
        }
        if(now.isBefore(reservationOpenAt) || !now.isBefore(reservationCloseAt)){
            throw new IllegalArgumentException("예약 가능 기간이 아닙니다");
        }
        if(reservedCount + participantCount > capacity){
            throw new IllegalArgumentException("예약 가능 인원이 초과되었습니다");
        }
        reservedCount += participantCount;
    }
}
