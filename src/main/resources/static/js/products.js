

const productsList = document.getElementById("products-list");
const productsMessage = document.getElementById("products-message");
const refreshButton = document.getElementById("refresh-products");
const loginLink = document.getElementById("products-login-link");

function showMessage(message, color = "#222") {
    productsMessage.textContent = message;
    productsMessage.style.color = color;
}

function createProductCard(product) {
    const card = document.createElement("article");
    card.className = "product-card";

    const name = document.createElement("h3");
    name.textContent = product.name;

    const price = document.createElement("p");

    price.textContent = new Intl.NumberFormat("pt-BR", {
        style: "currency",
        currency: "BRL"
    }).format(product.price);

    const stock = document.createElement("p");

    stock.textContent = `Estoque disponível: ${product.stockQuantity}`;

    card.appendChild(name);
    card.appendChild(price);
    card.appendChild(stock);

    return card;
}

async function loadProducts() {
    const token = sessionStorage.getItem("accessToken");

    productsList.replaceChildren();
    loginLink.hidden = true;

    if (!token) {
        showMessage(
            "Você precisa fazer login para visualizar os produtos.",
            "#b91c1c"
        );

        loginLink.hidden = false;
        return;
    }

    showMessage("Carregando produtos...");

    refreshButton.disabled = true;

    try {
        const response = await fetch("/api/products", {
            method: "GET",
            headers: {
                "Accept": "application/json",
                "Authorization": `Bearer ${token}`
            },
            cache: "no-store"
        });

        if (response.status === 401) {
            sessionStorage.removeItem("accessToken");
            loginLink.hidden = false;

            throw new Error(
                "Sua sessão expirou ou o token é inválido. Faça login novamente."
            );
        }

        if (response.status === 403) {
            throw new Error(
                "Você não possui permissão para visualizar os produtos."
            );
        }

        if (!response.ok) {
            throw new Error(
                `Erro ao consultar produtos. HTTP ${response.status}`
            );
        }

        const products = await response.json();

        if (!Array.isArray(products)) {
            throw new Error(
                "A API retornou um formato de dados inesperado."
            );
        }

        if (products.length === 0) {
            showMessage("Nenhum produto cadastrado no momento.");
            return;
        }

        const fragment = document.createDocumentFragment();

        products.forEach(product => {
            fragment.appendChild(createProductCard(product));
        });

        productsList.appendChild(fragment);

        showMessage(
            `${products.length} produto(s) encontrado(s).`,
            "#15803d"
        );

    } catch (error) {
        console.error("Erro ao carregar produtos:", error);

        showMessage(
            error.message || "Não foi possível carregar os produtos.",
            "#b91c1c"
        );

    } finally {
        refreshButton.disabled = false;
    }
}

refreshButton.addEventListener("click", loadProducts);

loadProducts();
