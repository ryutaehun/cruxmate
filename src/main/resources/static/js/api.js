const TOKEN_KEY = "cruxmate.accessToken";

export class ApiError extends Error {
    constructor(message, status, code, errors = {}) {
        super(message);
        this.name = "ApiError";
        this.status = status;
        this.code = code;
        this.errors = errors;
    }
}

export function getAccessToken() {
    return sessionStorage.getItem(TOKEN_KEY);
}

export function saveAccessToken(accessToken) {
    sessionStorage.setItem(TOKEN_KEY, accessToken);
}

export function clearAccessToken() {
    sessionStorage.removeItem(TOKEN_KEY);
}

export function requireAuthentication() {
    if (!getAccessToken()) {
        window.location.replace("/login.html");
        return false;
    }
    return true;
}

export function logout() {
    clearAccessToken();
    window.location.replace("/login.html");
}

export function bindLogoutButtons() {
    document.querySelectorAll("[data-logout]").forEach((button) => {
        button.addEventListener("click", logout);
    });
}

export async function apiFetch(path, options = {}) {
    const {
        authenticated = true,
        headers: customHeaders = {},
        ...fetchOptions
    } = options;

    const headers = new Headers(customHeaders);
    if (fetchOptions.body !== undefined && !headers.has("Content-Type")) {
        headers.set("Content-Type", "application/json");
    }

    if (authenticated) {
        const accessToken = getAccessToken();
        if (accessToken) {
            headers.set("Authorization", `Bearer ${accessToken}`);
        }
    }

    let response;
    try {
        response = await fetch(path, {...fetchOptions, headers});
    } catch {
        throw new ApiError("서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.", 0, "NETWORK_ERROR");
    }

    if (response.status === 401 && authenticated) {
        clearAccessToken();
        window.location.replace("/login.html");
        throw new ApiError("로그인이 만료되었습니다.", 401, "AUTHENTICATION_REQUIRED");
    }

    const body = await readResponseBody(response);
    if (!response.ok) {
        const message = buildErrorMessage(body, response.status);
        throw new ApiError(message, response.status, body?.code, body?.errors);
    }

    return body;
}

async function readResponseBody(response) {
    if (response.status === 204) {
        return null;
    }

    const contentType = response.headers.get("content-type") || "";
    if (!contentType.includes("application/json")) {
        return null;
    }

    try {
        return await response.json();
    } catch {
        return null;
    }
}

function buildErrorMessage(body, status) {
    if (body?.errors && typeof body.errors === "object") {
        const fieldMessages = Object.values(body.errors).filter(Boolean);
        if (fieldMessages.length > 0) {
            return fieldMessages.join(" ");
        }
    }

    return body?.message || `요청을 처리하지 못했습니다. (${status})`;
}

export function setMessage(element, message, isError = false) {
    element.textContent = message;
    element.classList.toggle("error", isError);
    element.hidden = false;
}

export function clearMessage(element) {
    element.textContent = "";
    element.classList.remove("error");
    element.hidden = true;
}

export function formatDateTime(value) {
    if (!value) {
        return "-";
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return value;
    }

    return new Intl.DateTimeFormat("ko-KR", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
        hour12: false
    }).format(date);
}
