import {
    apiFetch,
    bindLogoutButtons,
    clearMessage,
    formatDateTime,
    requireAuthentication,
    setMessage,
    showAdminLinks
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
const loadingState = document.querySelector("#loading-state");
const errorActions = document.querySelector("#error-actions");
const retryButton = document.querySelector("#retry-button");

let currentPage = 0;
let totalPages = 0;

if (requireAuthentication()) {
    bindLogoutButtons();
    showAdminLinks();
    loadSessions(0);
}

previousButton.addEventListener("click", () => loadSessions(currentPage - 1));
nextButton.addEventListener("click", () => loadSessions(currentPage + 1));
retryButton.addEventListener("click", () => loadSessions(currentPage));

async function loadSessions(page) {
    listElement.replaceChildren();
    emptyState.hidden = true;
    errorActions.hidden = true;
    loadingState.hidden = false;
    listElement.setAttribute("aria-busy", "true");
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
        setMessage(messageElement, error.message || "세션을 불러오지 못했습니다.", true);
        errorActions.hidden = false;
        pageInfo.textContent = "-";
    } finally {
        loadingState.hidden = true;
        listElement.setAttribute("aria-busy", "false");
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
        detailLine("종료", formatDateTime(session.endAt)),
        detailLine(
            "예약 기간",
            `${formatDateTime(session.reservationOpenAt)} ~ ${formatDateTime(session.reservationCloseAt)}`
        )
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
    select.addEventListener("change", () => {
        delete form.dataset.idempotencyKey;
    });

    for (let count = 1; count <= 4; count += 1) {
        const option = document.createElement("option");
        option.value = String(count);
        option.textContent = `${count}명`;
        option.disabled = count > session.remainingCapacity;
        select.append(option);
    }

    const availability = getReservationAvailability(session);
    const button = document.createElement("button");
    button.className = "button button-primary";
    button.type = "submit";
    button.textContent = availability.label;
    button.disabled = !availability.available;
    select.disabled = !availability.available;

    field.append(label, select);
    form.append(field, button);
    form.addEventListener("submit", (event) => reserveSession(event, session, select, button));
    return form;
}

function getReservationAvailability(session) {
    if (session.remainingCapacity < 1) {
        return {available: false, label: "마감"};
    }
    if (session.status !== "SCHEDULED") {
        return {available: false, label: "예약 불가"};
    }

    const now = Date.now();
    const openAt = new Date(session.reservationOpenAt).getTime();
    const closeAt = new Date(session.reservationCloseAt).getTime();
    if (!Number.isNaN(openAt) && now < openAt) {
        return {available: false, label: "오픈 예정"};
    }
    if (!Number.isNaN(closeAt) && now >= closeAt) {
        return {available: false, label: "예약 종료"};
    }
    return {available: true, label: "예약하기"};
}

async function reserveSession(event, session, select, button) {
    event.preventDefault();
    clearMessage(messageElement);

    const form = event.currentTarget;
    button.disabled = true;
    button.textContent = "예약 중...";

    try {
        // 응답이 유실되어 같은 시도를 재요청하더라도 동일한 키를 사용한다.
        const idempotencyKey = formIdempotencyKey(form);
        const response = await apiFetch("/api/reservations", {
            method: "POST",
            headers: {"Idempotency-Key": idempotencyKey},
            body: JSON.stringify({
                sessionId: session.sessionId,
                participantCount: Number(select.value)
            })
        });

        delete form.dataset.idempotencyKey;
        await loadSessions(currentPage);
        setMessage(messageElement, `예약이 완료되었습니다. 예약 번호: ${response.reservationId}`);
    } catch (error) {
        setMessage(messageElement, error.message || "예약을 처리하지 못했습니다.", true);
        button.disabled = false;
        button.textContent = "예약하기";
    }
}

function formIdempotencyKey(form) {
    if (!form.dataset.idempotencyKey) {
        form.dataset.idempotencyKey = createUuid();
    }
    return form.dataset.idempotencyKey;
}

function createUuid() {
    if (typeof globalThis.crypto?.randomUUID === "function") {
        return globalThis.crypto.randomUUID();
    }

    const bytes = new Uint8Array(16);
    if (typeof globalThis.crypto?.getRandomValues === "function") {
        globalThis.crypto.getRandomValues(bytes);
    } else {
        for (let index = 0; index < bytes.length; index += 1) {
            bytes[index] = Math.floor(Math.random() * 256);
        }
    }

    bytes[6] = (bytes[6] & 0x0f) | 0x40;
    bytes[8] = (bytes[8] & 0x3f) | 0x80;
    const hex = Array.from(bytes, (byte) => byte.toString(16).padStart(2, "0"));
    return `${hex.slice(0, 4).join("")}-${hex.slice(4, 6).join("")}-${hex.slice(6, 8).join("")}-${hex.slice(8, 10).join("")}-${hex.slice(10).join("")}`;
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
