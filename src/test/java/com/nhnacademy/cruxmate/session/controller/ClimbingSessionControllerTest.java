package com.nhnacademy.cruxmate.session.controller;

import com.nhnacademy.cruxmate.common.exception.BusinessException;
import com.nhnacademy.cruxmate.common.exception.ErrorCode;
import com.nhnacademy.cruxmate.session.service.ClimbingSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClimbingSessionController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ClimbingSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClimbingSessionService climbingSessionService;

    @Test
    void 세션_생성_요청값이_유효하지_않으면_400을_반환한다() throws Exception {
        String request = """
                {
                  "title": "",
                  "location": "",
                  "capacity": 0
                }
                """;

        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COM001"))
                .andExpect(jsonPath("$.message")
                        .value("요청 값이 올바르지 않습니다."))
                .andExpect(jsonPath("$.errors.title")
                        .value("세션 제목은 필수입니다."))
                .andExpect(jsonPath("$.errors.location")
                        .value("세션 장소는 필수입니다."))
                .andExpect(jsonPath("$.errors.capacity")
                        .value("정원은 1명 이상이어야 합니다."))
                .andExpect(jsonPath("$.errors.startAt")
                        .value("세션 시작 시간은 필수입니다."));
    }

    @Test
    void 존재하지_않는_세션을_조회하면_404와_에러코드를_반환한다() throws Exception{
        Long sessionId = 0L;

        when(climbingSessionService.getSession(sessionId))
                .thenThrow(new BusinessException(ErrorCode.CLIMBING_SESSION_NOT_FOUND));

        mockMvc.perform(get("/api/sessions/{sessionId}", sessionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SES001"))
                .andExpect(jsonPath("$.message")
                        .value("클라이밍 세션을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.errors").isEmpty());

        verify(climbingSessionService).getSession(sessionId);
    }
}
