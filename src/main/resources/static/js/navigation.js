
const accessToken = sessionStorage.getItem("accessToken");

const dashboardLink = document.getElementById("dashboardLink");
const loginLink = document.getElementById("loginLink");
const registerLink = document.getElementById("registerLink");
const logoutButton = document.getElementById("logoutButton");

function getTokenPayload(token) {
    try {
        if (!token) return null;

        const payload = token.split(".")[1];

        if (!payload) return null;

        const decoded = atob(
            payload.replace(/-/g, "+").replace(/_/g, "/")
        );

        return JSON.parse(decoded);

    } catch (error) {
        return null;
    }
}

function updateNavigation() {
    const payload = getTokenPayload(accessToken);

    const isAuthenticated =
        payload !== null &&
        typeof payload.exp === "number" &&
        payload.exp * 1000 > Date.now();

    if (!isAuthenticated) {
        sessionStorage.removeItem("accessToken");
        return;
    }

    loginLink.hidden = true;
    registerLink.hidden = true;
    logoutButton.hidden = false;

    if (payload.role === "ADMIN") {
        dashboardLink.hidden = false;
    }
}

logoutButton.addEventListener("click", () => {
    sessionStorage.removeItem("accessToken");
    window.location.href = "/index.html";
});

updateNavigation();
