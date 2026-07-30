import {
    apiFetch,
    bindLogoutButtons,
    clearMessage,
    formatDateTime,
    requireAuthentication,
    setMessage
} from "./api.js";

const PAGE_SIZE = 20;
const statusLabels = {
    CONFIRMED: "예약 확정",
    CANCELED: "취소"
};

const listElement = document.querySelector("#reservation-list");
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
    loadReservations(0);
}

previousButton.addEventListener("click", () => loadReservations(currentPage - 1));
nextButton.addEventListener("click", () => loadReservations(currentPage + 1));
retryButton.addEventListener("click", () => loadReservations(currentPage));

async function loadReservations(page) {
    listElement.replaceChildren();
    emptyState.hidden = true;
    errorActions.hidden = true;
    loadingState.hidden = false;
    listElement.setAttribute("aria-busy", "true");
    clearMessage(messageElement);
    setPaginationDisabled(true);

    try {
        const response = await apiFetch(`/api/reservations/me?page=${page}&size=${PAGE_SIZE}`);
        currentPage = response.page;
        totalPages = response.totalPages;
        response.content.forEach((reservation) => {
            listElement.append(createReservationCard(reservation));
        });
        emptyState.hidden = response.content.length !== 0;
        updatePagination();
    } catch (error) {
        setMessage(messageElement, error.message || "예약 내역을 불러오지 못했습니다.", true);
        errorActions.hidden = false;
        pageInfo.textContent = "-";
    } finally {
        loadingState.hidden = true;
        listElement.setAttribute("aria-busy", "false");
    }
}

function createReservationCard(reservation) {
    const article = document.createElement("article");
    article.className = "reservation-card";

    const head = document.createElement("div");
    head.className = "reservation-head";
    const titleWrap = document.createElement("div");
    const title = document.createElement("h2");
    title.textContent = reservation.sessionTitle;
    const location = document.createElement("p");
    location.className = "muted";
    location.textContent = reservation.sessionLocation;
    titleWrap.append(title, location);

    const status = document.createElement("span");
    const statusClass = reservation.status === "CONFIRMED" ? "badge-confirmed" : "badge-canceled";
    status.className = `badge ${statusClass}`;
    status.textContent = statusLabels[reservation.status] || reservation.status;
    head.append(titleWrap, status);

    const details = document.createElement("div");
    details.className = "details";
    details.append(
        detailLine("세션", `${formatDateTime(reservation.sessionStartAt)} ~ ${formatDateTime(reservation.sessionEndAt)}`),
        detailLine("참여 인원", `${reservation.participantCount}명`),
        detailLine("예약 시각", formatDateTime(reservation.createdAt))
    );

    const actions = document.createElement("div");
    actions.className = "reservation-actions";
    const reservationNumber = document.createElement("p");
    reservationNumber.className = "muted";
    reservationNumber.textContent = `예약 번호 #${reservation.reservationId}`;
    actions.append(reservationNumber);

    if (reservation.status === "CONFIRMED") {
        const cancelButton = document.createElement("button");
        cancelButton.className = "button button-danger";
        cancelButton.type = "button";
        cancelButton.textContent = "예약 취소";
        cancelButton.addEventListener("click", () => cancelReservation(reservation.reservationId, cancelButton));
        actions.append(cancelButton);
    }

    article.append(head, details, actions);
    return article;
}

function detailLine(label, value) {
    const line = document.createElement("p");
    const labelNode = document.createElement("strong");
    labelNode.textContent = `${label} · `;
    line.append(labelNode, document.createTextNode(value));
    return line;
}

async function cancelReservation(reservationId, button) {
    if (!window.confirm(`예약 #${reservationId}을 취소하시겠습니까?`)) {
        return;
    }

    clearMessage(messageElement);
    button.disabled = true;
    button.textContent = "취소 중...";

    try {
        const response = await apiFetch(`/api/reservations/${reservationId}/cancel`, {
            method: "PATCH"
        });
        await loadReservations(currentPage);
        setMessage(messageElement, `예약 #${response.reservationId}이 취소되었습니다.`);
    } catch (error) {
        setMessage(messageElement, error.message || "예약을 취소하지 못했습니다.", true);
        button.disabled = false;
        button.textContent = "예약 취소";
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
