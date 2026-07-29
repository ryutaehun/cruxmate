import {
    apiFetch,
    bindLogoutButtons,
    clearMessage,
    formatDateTime,
    requireAuthentication,
    setMessage
} from "./api.js";

const PAGE_SIZE = 20;
const levelLabels = {
    BEGINNER: "초급",
    INTERMEDIATE: "중급",
    ADVANCED: "고급",
    ALL_LEVELS: "모든 난이도"
};

const listElement = document.querySelector("#session-list");
const emptyState = document.querySelector("#empty-state");
const messageElement = document.querySelector("#page-message");
const previousButton = document.querySelector("#previous-page");
const nextButton = document.querySelector("#next-page");
const pageInfo = document.querySelector("#page-info");

let currentPage = 0;
let totalPages = 0;

if (requireAuthentication()) {
    bindLogoutButtons();
    loadSessions(0);
}

previousButton.addEventListener("click", () => loadSessions(currentPage - 1));
nextButton.addEventListener("click", () => loadSessions(currentPage + 1));

async function loadSessions(page) {
    listElement.replaceChildren();
    emptyState.hidden = true;
    clearMessage(messageElement);
    setPaginationDisabled(true);

    try {
        const response = await apiFetch(`/api/sessions?page=${page}&size=${PAGE_SIZE}`, {
            authenticated: false
        });

        currentPage = response.page;
        totalPages = response.totalPages;
        response.content.forEach((session) => listElement.append(createSessionCard(session)));
        emptyState.hidden = response.content.length !== 0;
        updatePagination();
    } catch (error) {
        setMessage(messageElement, error.message, true);
        pageInfo.textContent = "-";
    }
}

function createSessionCard(session) {
    const article = document.createElement("article");
    article.className = "session-card";

    const top = document.createElement("div");
    top.className = "card-top";

    const titleWrap = document.createElement("div");
    const title = document.createElement("h2");
    title.textContent = session.title;
    const location = document.createElement("p");
    location.className = "muted";
    location.textContent = session.location;
    titleWrap.append(title, location);

    const level = document.createElement("span");
    level.className = "badge";
    level.textContent = levelLabels[session.level] || session.level;
    top.append(titleWrap, level);

    const details = document.createElement("div");
    details.className = "details";
    details.append(
        detailLine("시작", formatDateTime(session.startAt)),
        detailLine("종료", formatDateTime(session.endAt))
    );

    const capacity = document.createElement("div");
    capacity.className = "capacity-row";
    const capacityLabel = document.createElement("span");
    capacityLabel.className = "muted";
    capacityLabel.textContent = "예약 현황";
    const capacityValue = document.createElement("span");
    capacityValue.className = "capacity-value";
    capacityValue.textContent = `${session.reservedCount} / ${session.capacity}명`;
    capacity.append(capacityLabel, capacityValue);

    const form = createReservationForm(session);
    article.append(top, details, capacity, form);
    return article;
}

function detailLine(label, value) {
    const line = document.createElement("p");
    const labelNode = document.createElement("strong");
    labelNode.textContent = `${label} · `;
    const valueNode = document.createTextNode(value);
    line.append(labelNode, valueNode);
    return line;
}

function createReservationForm(session) {
    const form = document.createElement("form");
    form.className = "reservation-form";

    const field = document.createElement("div");
    field.className = "field";
    const id = `participants-${session.sessionId}`;
    const label = document.createElement("label");
    label.htmlFor = id;
    label.textContent = "참여 인원";
    const select = document.createElement("select");
    select.id = id;
    select.name = "participantCount";

    for (let count = 1; count <= 4; count += 1) {
        const option = document.createElement("option");
        option.value = String(count);
        option.textContent = `${count}명`;
        option.disabled = count > session.remainingCapacity;
        select.append(option);
    }

    const button = document.createElement("button");
    button.className = "button button-primary";
    button.type = "submit";
    button.textContent = session.remainingCapacity > 0 ? "예약하기" : "마감";
    button.disabled = session.remainingCapacity < 1 || session.status !== "SCHEDULED";

    field.append(label, select);
    form.append(field, button);
    form.addEventListener("submit", (event) => reserveSession(event, session, select, button));
    return form;
}

async function reserveSession(event, session, select, button) {
    event.preventDefault();
    clearMessage(messageElement);

    // 이 키는 아래의 단일 예약 시도가 끝날 때까지 재사용된다.
    const idempotencyKey = crypto.randomUUID();
    button.disabled = true;
    button.textContent = "예약 중...";

    try {
        const response = await apiFetch("/api/reservations", {
            method: "POST",
            headers: {"Idempotency-Key": idempotencyKey},
            body: JSON.stringify({
                sessionId: session.sessionId,
                participantCount: Number(select.value)
            })
        });

        await loadSessions(currentPage);
        setMessage(messageElement, `예약이 완료되었습니다. 예약 번호: ${response.reservationId}`);
    } catch (error) {
        setMessage(messageElement, error.message, true);
        button.disabled = false;
        button.textContent = "예약하기";
    }
}

function updatePagination() {
    pageInfo.textContent = totalPages === 0
        ? "0 / 0"
        : `${currentPage + 1} / ${totalPages}`;
    previousButton.disabled = currentPage <= 0;
    nextButton.disabled = totalPages === 0 || currentPage >= totalPages - 1;
}

function setPaginationDisabled(disabled) {
    previousButton.disabled = disabled;
    nextButton.disabled = disabled;
}
