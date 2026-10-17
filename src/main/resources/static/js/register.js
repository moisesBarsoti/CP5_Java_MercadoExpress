
const registerForm = document.getElementById("register-form");
const registerMessage = document.getElementById("register-message");

registerForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    const name = document.getElementById("name").value.trim();
    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value;

    const submitButton = registerForm.querySelector("button");

    registerMessage.textContent = "Criando conta...";
    registerMessage.style.color = "#222";
    submitButton.disabled = true;

    try {
        const response = await fetch("/api/auth/register", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                name,
                email,
                password
            })
        });

        if (!response.ok) {
            throw new Error("Registration failed");
        }

        registerMessage.textContent =
            "Conta criada com sucesso! Faça login para continuar.";

        registerMessage.style.color = "#15803d";

        registerForm.reset();

    } catch (error) {
        registerMessage.textContent =
            "Não foi possível criar sua conta. Verifique os dados e tente novamente.";

        registerMessage.style.color = "#b91c1c";

    } finally {
        submitButton.disabled = false;
    }
});
