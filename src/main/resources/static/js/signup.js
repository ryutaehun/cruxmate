import {apiFetch, clearMessage, setMessage} from "./api.js";

const form = document.querySelector("#signup-form");
const emailInput = document.querySelector("#email");
const passwordInput = document.querySelector("#password");
const passwordConfirmInput = document.querySelector("#password-confirm");
const submitButton = document.querySelector("#signup-button");
const messageElement = document.querySelector("#signup-message");

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    clearMessage(messageElement);

    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }

    if (passwordInput.value !== passwordConfirmInput.value) {
        setMessage(messageElement, "비밀번호가 서로 일치하지 않습니다.", true);
        passwordConfirmInput.focus();
        return;
    }

    submitButton.disabled = true;
    submitButton.textContent = "계정 만드는 중...";

    try {
        await apiFetch("/api/members", {
            method: "POST",
            authenticated: false,
            body: JSON.stringify({
                email: emailInput.value.trim(),
                password: passwordInput.value
            })
        });

        window.location.replace("/login.html?registered=true");
    } catch (error) {
        setMessage(messageElement, error.message || "계정을 만들지 못했습니다. 다시 시도해 주세요.", true);
    } finally {
        submitButton.disabled = false;
        submitButton.textContent = "계정 만들기";
    }
});
