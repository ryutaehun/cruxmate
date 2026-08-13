import {
    apiFetch,
    bindLogoutButtons,
    clearMessage,
    requireAdmin,
    setMessage
} from "./api.js";

const form = document.querySelector("#session-create-form");
const submitButton = document.querySelector("#create-button");
const messageElement = document.querySelector("#create-message");
const titleInput = document.querySelector("#title");

if (requireAdmin()) {
    bindLogoutButtons();
    setDefaultSchedule();
    form.addEventListener("submit", createSession);
    form.addEventListener("reset", () => {
        clearMessage(messageElement);
        window.setTimeout(setDefaultSchedule, 0);
    });
}

async function createSession(event) {
    event.preventDefault();
    clearMessage(messageElement);

    const data = new FormData(form);
    const validationMessage = validateSchedule(data);
    if (validationMessage) {
        setMessage(messageElement, validationMessage, true);
        return;
    }

    submitButton.disabled = true;
    submitButton.textContent = "생성 중...";

    try {
        const response = await apiFetch("/api/sessions", {
            method: "POST",
            body: JSON.stringify({
                title: data.get("title").trim(),
                location: data.get("location").trim(),
                startAt: data.get("startAt"),
                endAt: data.get("endAt"),
                reservationOpenAt: data.get("reservationOpenAt"),
                reservationCloseAt: data.get("reservationCloseAt"),
                capacity: Number(data.get("capacity")),
                level: data.get("level")
            })
        });

        form.reset();
        setMessage(messageElement, `세션이 생성되었습니다. 세션 번호: ${response.sessionId}`);
        titleInput.focus();
    } catch (error) {
        setMessage(messageElement, error.message || "세션을 생성하지 못했습니다.", true);
    } finally {
        submitButton.disabled = false;
        submitButton.textContent = "세션 만들기";
    }
}

function validateSchedule(data) {
    const reservationOpenAt = new Date(data.get("reservationOpenAt"));
    const reservationCloseAt = new Date(data.get("reservationCloseAt"));
    const startAt = new Date(data.get("startAt"));
    const endAt = new Date(data.get("endAt"));

    if (!(reservationOpenAt < reservationCloseAt
        && reservationCloseAt <= startAt
        && startAt < endAt)) {
        return "예약 시작 < 예약 마감 ≤ 세션 시작 < 세션 종료 순서로 입력해 주세요.";
    }
    return null;
}

function setDefaultSchedule() {
    const now = new Date();
    now.setSeconds(0, 0);

    const startAt = new Date(now);
    startAt.setDate(startAt.getDate() + 7);
    startAt.setHours(19, 0, 0, 0);

    const endAt = new Date(startAt);
    endAt.setHours(endAt.getHours() + 2);

    const reservationCloseAt = new Date(startAt);
    reservationCloseAt.setDate(reservationCloseAt.getDate() - 1);

    setInputValue("reservation-open-at", now);
    setInputValue("reservation-close-at", reservationCloseAt);
    setInputValue("start-at", startAt);
    setInputValue("end-at", endAt);
}

function setInputValue(id, date) {
    const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
    document.querySelector(`#${id}`).value = localDate.toISOString().slice(0, 16);
}
