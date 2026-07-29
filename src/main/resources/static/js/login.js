import {
    apiFetch,
    clearAccessToken,
    clearMessage,
    saveAccessToken,
    setMessage
} from "./api.js";

const form = document.querySelector("#login-form");
const emailInput = document.querySelector("#email");
const passwordInput = document.querySelector("#password");
const submitButton = document.querySelector("#login-button");
const messageElement = document.querySelector("#login-message");

clearAccessToken();

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    clearMessage(messageElement);
    submitButton.disabled = true;
    submitButton.textContent = "로그인 중...";

    try {
        const response = await apiFetch("/api/auth/login", {
            method: "POST",
            authenticated: false,
            body: JSON.stringify({
                email: emailInput.value.trim(),
                password: passwordInput.value
            })
        });

        saveAccessToken(response.accessToken);
        window.location.replace("/sessions.html");
    } catch (error) {
        setMessage(messageElement, error.message, true);
    } finally {
        submitButton.disabled = false;
        submitButton.textContent = "로그인";
    }
});
