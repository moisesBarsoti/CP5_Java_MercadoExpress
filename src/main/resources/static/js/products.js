const productsList = document.getElementById("products-list");
const productsMessage = document.getElementById("products-message");
const actionMessage = document.getElementById("action-message");

const refreshButton = document.getElementById("refresh-products");
const productsLoginLink = document.getElementById("products-login-link");

const addProductButton = document.getElementById("add-product-button");
const adminSection = document.getElementById("admin-product-section");
const productForm = document.getElementById("product-form");
const productFormTitle = document.getElementById("product-form-title");
const cancelButton = document.getElementById("cancel-product-button");

const productNameInput = document.getElementById("product-name");
const productPriceInput = document.getElementById("product-price");
const productCostInput = document.getElementById("product-cost-price");
const productStockInput = document.getElementById("product-stock");

const currencyFormatter = new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL"
});

let editingProductId = null;

function getSession() {
    const token = sessionStorage.getItem("accessToken");

    if (!token) {
        return null;
    }

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
    } catch (error) {
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
    productsLoginLink.hidden = false;

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
            Accept: "application/json",
            Authorization: `Bearer ${session.token}`,
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

function createPurchaseForm(product) {
    const availableStock = Number(product.stockQuantity);
    const hasStock = Number.isInteger(availableStock) && availableStock > 0;

    const form = document.createElement("form");
    form.className = "purchase-form";
    form.style.display = "flex";
    form.style.flexWrap = "wrap";
    form.style.alignItems = "end";
    form.style.gap = "10px";
    form.style.marginTop = "16px";

    const field = document.createElement("div");

    const label = document.createElement("label");
    label.htmlFor = `purchase-quantity-${product.id}`;
    label.textContent = "Quantidade";
    label.style.display = "block";
    label.style.marginBottom = "6px";

    const quantityInput = document.createElement("input");
    quantityInput.id = label.htmlFor;
    quantityInput.type = "number";
    quantityInput.min = "1";
    quantityInput.max = String(availableStock);
    quantityInput.step = "1";
    quantityInput.value = "1";
    quantityInput.required = true;
    quantityInput.disabled = !hasStock;
    quantityInput.style.width = "90px";

    const buyButton = document.createElement("button");
    buyButton.type = "submit";
    buyButton.className = "button";
    buyButton.textContent = hasStock ? "Comprar" : "Indisponível";
    buyButton.disabled = !hasStock;

    field.append(label, quantityInput);
    form.append(field, buyButton);

    let submitting = false;

    form.addEventListener("submit", async (event) => {
        event.preventDefault();

        if (submitting || !hasStock) {
            return;
        }

        const quantity = Number(quantityInput.value);

        if (
            !Number.isSafeInteger(quantity) ||
            quantity < 1 ||
            quantity > availableStock
        ) {
            showActionMessage(
                "Informe uma quantidade válida e disponível em estoque.",
                "#b91c1c"
            );
            return;
        }

        submitting = true;
        buyButton.disabled = true;
        quantityInput.disabled = true;
        buyButton.textContent = "Processando...";
        showActionMessage("");

        try {
            const response = await apiRequest("/api/orders", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    items: [
                        {
                            productId: product.id,
                            quantity
                        }
                    ]
                })
            });

            const order = await response.json();
            const orderNumber = order.id != null ? ` #${order.id}` : "";

            showActionMessage(
                `Pedido${orderNumber} criado com sucesso! ` +
                "O pagamento está sendo processado. " +
                "Aguarde a confirmação do sistema."
            );

            await loadProducts();
        } catch (error) {
            console.error("Erro ao realizar compra:", error);
            showActionMessage(error.message, "#b91c1c");
        } finally {
            submitting = false;
            buyButton.disabled = !hasStock;
            quantityInput.disabled = !hasStock;
            buyButton.textContent = hasStock ? "Comprar" : "Indisponível";
        }
    });

    return form;
}

function createProductCard(product, isAdmin) {
    const card = document.createElement("article");
    card.className = "product-card";

    const name = document.createElement("h3");
    name.textContent = product.name;

    const price = document.createElement("p");
    price.textContent = currencyFormatter.format(product.price);

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
        editButton.addEventListener("click", () => openProductForm(product));

        const deleteButton = document.createElement("button");
        deleteButton.textContent = "Excluir";
        deleteButton.className = "danger-button";
        deleteButton.type = "button";
        deleteButton.addEventListener("click", () => deleteProduct(product));

        actions.append(editButton, deleteButton);
        card.appendChild(actions);
    } else {
        card.appendChild(createPurchaseForm(product));
    }

    return card;
}

async function loadProducts() {
    productsList.replaceChildren();

    const session = getSession();

    addProductButton.hidden = true;
    adminSection.hidden = true;
    productsLoginLink.hidden = true;
    editingProductId = null;

    if (!session) {
        showMessage("Faça login para visualizar os produtos.", "#b91c1c");
        productsLoginLink.hidden = false;
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

        products.forEach((product) => {
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

async function openProductForm(product = null) {
    const session = getSession();

    if (!session || session.role !== "ADMIN") {
        showActionMessage("Operação não permitida.", "#b91c1c");
        return;
    }

    editingProductId = null;
    productForm.reset();
    adminSection.hidden = true;
    showActionMessage("");

    if (product) {
        productFormTitle.textContent = "Editar produto";

        try {
            const response = await apiRequest(
                `/api/admin/products/${product.id}/cost`
            );

            const costData = await response.json();

            if (costData.costPrice == null) {
                throw new Error(
                    "Este produto não possui custo de aquisição cadastrado."
                );
            }

            productNameInput.value = product.name;
            productPriceInput.value = product.price;
            productCostInput.value = costData.costPrice;
            productStockInput.value = product.stockQuantity;
            editingProductId = product.id;
        } catch (error) {
            console.error("Erro ao consultar custo do produto:", error);
            showActionMessage(error.message, "#b91c1c");
            return;
        }
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
        name: productNameInput.value.trim(),
        price: Number(productPriceInput.value),
        costPrice: Number(productCostInput.value),
        stockQuantity: Number(productStockInput.value)
    };

    if (
        !product.name ||
        !Number.isFinite(product.price) ||
        product.price <= 0 ||
        !Number.isFinite(product.costPrice) ||
        product.costPrice < 0 ||
        !Number.isInteger(product.stockQuantity) ||
        product.stockQuantity < 0
    ) {
        showActionMessage("Verifique os dados informados.", "#b91c1c");
        return;
    }

    const isEditing = editingProductId !== null;
    const url = isEditing
        ? `/api/products/${editingProductId}`
        : "/api/products";
    const method = isEditing ? "PUT" : "POST";

    const submitButton = productForm.querySelector('button[type="submit"]');
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

    if (!confirmed) {
        return;
    }

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

addProductButton.addEventListener("click", () => openProductForm());

cancelButton.addEventListener("click", () => {
    adminSection.hidden = true;
    editingProductId = null;
    productForm.reset();
    showActionMessage("");
});

productForm.addEventListener("submit", saveProduct);
refreshButton.addEventListener("click", loadProducts);

loadProducts();