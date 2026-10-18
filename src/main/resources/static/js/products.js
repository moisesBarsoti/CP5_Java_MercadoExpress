
const productsList = document.getElementById("products-list");
const productsMessage = document.getElementById("products-message");
const actionMessage = document.getElementById("action-message");

const refreshButton = document.getElementById("refresh-products");
const loginLink = document.getElementById("products-login-link");

const addProductButton = document.getElementById("add-product-button");
const adminSection = document.getElementById("admin-product-section");
const productForm = document.getElementById("product-form");
const productFormTitle = document.getElementById("product-form-title");
const cancelButton = document.getElementById("cancel-product-button");

let editingProductId = null;

function getSession() {
    const token = sessionStorage.getItem("accessToken");

    if (!token) return null;

    try {
        const encodedPayload = token.split(".")[1];

        const payload = JSON.parse(
            atob(
                encodedPayload
                    .replace(/-/g, "+")
                    .replace(/_/g, "/")
            )
        );

        if (!payload.exp || Date.now() >= payload.exp * 1000) {
            sessionStorage.removeItem("accessToken");
            return null;
        }

        return {
            token,
            role: payload.role
        };
    } catch {
        sessionStorage.removeItem("accessToken");
        return null;
    }
}

function showMessage(message, color = "#222") {
    productsMessage.textContent = message;
    productsMessage.style.color = color;
}

function showActionMessage(message, color = "#15803d") {
    actionMessage.textContent = message;
    actionMessage.style.color = color;
}

function handleUnauthorized() {
    sessionStorage.removeItem("accessToken");

    addProductButton.hidden = true;
    adminSection.hidden = true;
    loginLink.hidden = false;

    throw new Error("Sessão expirada. Faça login novamente.");
}

async function apiRequest(url, options = {}) {
    const session = getSession();

    if (!session) {
        handleUnauthorized();
    }

    const response = await fetch(url, {
        ...options,
        headers: {
            "Accept": "application/json",
            "Authorization": `Bearer ${session.token}`,
            ...(options.headers || {})
        },
        cache: "no-store"
    });

    if (response.status === 401) {
        handleUnauthorized();
    }

    if (response.status === 403) {
        throw new Error("Você não tem permissão para esta operação.");
    }

    if (!response.ok) {
        throw new Error(`Erro na operação. HTTP ${response.status}`);
    }

    return response;
}

function createProductCard(product, isAdmin) {
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

    card.append(name, price, stock);

    if (isAdmin) {
        const actions = document.createElement("div");
        actions.className = "product-actions";

        const editButton = document.createElement("button");
        editButton.textContent = "Editar";
        editButton.className = "secondary-button";
        editButton.type = "button";

        editButton.addEventListener("click", () => {
            openProductForm(product);
        });

        const deleteButton = document.createElement("button");
        deleteButton.textContent = "Excluir";
        deleteButton.className = "danger-button";
        deleteButton.type = "button";

        deleteButton.addEventListener("click", () => {
            deleteProduct(product);
        });

        actions.append(editButton, deleteButton);
        card.appendChild(actions);
    }

    return card;
}

async function loadProducts() {
    productsList.replaceChildren();

    const session = getSession();

    addProductButton.hidden = true;
    adminSection.hidden = true;
    loginLink.hidden = true;

    if (!session) {
        showMessage("Faça login para visualizar os produtos.", "#b91c1c");
        loginLink.hidden = false;
        return;
    }

    showMessage("Carregando produtos...");
    refreshButton.disabled = true;

    try {
        const response = await apiRequest("/api/products");
        const products = await response.json();

        if (!Array.isArray(products)) {
            throw new Error("A API retornou dados inesperados.");
        }

        const isAdmin = session.role === "ADMIN";

        addProductButton.hidden = !isAdmin;

        if (products.length === 0) {
            showMessage("Nenhum produto cadastrado no momento.");
            return;
        }

        const fragment = document.createDocumentFragment();

        products.forEach(product => {
            fragment.appendChild(createProductCard(product, isAdmin));
        });

        productsList.appendChild(fragment);

        showMessage(
            `${products.length} produto(s) encontrado(s).`,
            "#15803d"
        );

    } catch (error) {
        console.error("Erro ao carregar produtos:", error);
        showMessage(error.message, "#b91c1c");
    } finally {
        refreshButton.disabled = false;
    }
}

function openProductForm(product = null) {
    editingProductId = product ? product.id : null;

    productForm.reset();
    showActionMessage("");

    if (product) {
        productFormTitle.textContent = "Editar produto";

        document.getElementById("product-name").value = product.name;
        document.getElementById("product-price").value = product.price;
        document.getElementById("product-stock").value = product.stockQuantity;
    } else {
        productFormTitle.textContent = "Cadastrar produto";
    }

    adminSection.hidden = false;
    adminSection.scrollIntoView({ behavior: "smooth" });
}

async function saveProduct(event) {
    event.preventDefault();

    const session = getSession();

    if (!session || session.role !== "ADMIN") {
        showActionMessage("Operação não permitida.", "#b91c1c");
        return;
    }

    const product = {
        name: document.getElementById("product-name").value.trim(),
        price: Number(document.getElementById("product-price").value),
        stockQuantity: Number(document.getElementById("product-stock").value)
    };

    const isEditing = editingProductId !== null;

    const url = isEditing
        ? `/api/products/${editingProductId}`
        : "/api/products";

    const method = isEditing ? "PUT" : "POST";

    const submitButton = productForm.querySelector(
        'button[type="submit"]'
    );

    submitButton.disabled = true;

    try {
        await apiRequest(url, {
            method,
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(product)
        });

        adminSection.hidden = true;
        editingProductId = null;
        productForm.reset();

        await loadProducts();

        showActionMessage(
            isEditing
                ? "Produto atualizado com sucesso!"
                : "Produto cadastrado com sucesso!"
        );

    } catch (error) {
        console.error("Erro ao salvar produto:", error);
        showActionMessage(error.message, "#b91c1c");
    } finally {
        submitButton.disabled = false;
    }
}

async function deleteProduct(product) {
    const session = getSession();

    if (!session || session.role !== "ADMIN") {
        showActionMessage("Operação não permitida.", "#b91c1c");
        return;
    }

    const confirmed = window.confirm(
        `Deseja realmente excluir "${product.name}"?`
    );

    if (!confirmed) return;

    try {
        await apiRequest(`/api/products/${product.id}`, {
            method: "DELETE"
        });

        await loadProducts();

        showActionMessage("Produto excluído com sucesso!");

    } catch (error) {
        console.error("Erro ao excluir produto:", error);
        showActionMessage(error.message, "#b91c1c");
    }
}

addProductButton.addEventListener("click", () => {
    openProductForm();
});

cancelButton.addEventListener("click", () => {
    adminSection.hidden = true;
    editingProductId = null;
    productForm.reset();
});

productForm.addEventListener("submit", saveProduct);
refreshButton.addEventListener("click", loadProducts);

loadProducts();
