package com.nhnacademy.cruxmate.session.dto;

import com.nhnacademy.cruxmate.session.domain.ClimbingSessionLevel;

import java.time.LocalDateTime;

public record ClimbingSessionCreateRequest (String title, String location, LocalDateTime startAt, LocalDateTime endAt,
                                            LocalDateTime reservationOpenAt, LocalDateTime reservationCloseAt,
                                            int capacity, ClimbingSessionLevel level){
}
