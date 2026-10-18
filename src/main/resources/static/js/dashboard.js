
const token = sessionStorage.getItem("accessToken");

const logoutButton = document.getElementById("logoutButton");

logoutButton.addEventListener("click", () => {
    sessionStorage.removeItem("accessToken");
    window.location.href = "/login.html";
});

const currencyFormatter = new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL"
});

function showError(message) {
    const element = document.getElementById("errorMessage");

    element.textContent = message;
    element.hidden = false;
}

async function fetchDashboardData(endpoint) {
    const response = await fetch(endpoint, {
        headers: {
            Authorization: `Bearer ${token}`
        }
    });

    if (response.status === 401) {
        sessionStorage.removeItem("accessToken");
        window.location.href = "/login.html";
        throw new Error("Authentication required");
    }

    if (response.status === 403) {
        throw new Error(
            "Acesso negado. Apenas administradores podem visualizar o dashboard."
        );
    }

    if (!response.ok) {
        throw new Error("Erro ao carregar os dados do dashboard.");
    }

    return response.json();
}

function renderSummary(data) {
    document.getElementById("totalRevenue").textContent =
        currencyFormatter.format(data.totalRevenue);

    document.getElementById("totalOrders").textContent =
        data.totalOrders;

    document.getElementById("paidOrders").textContent =
        data.paidOrders;

    document.getElementById("availableStock").textContent =
        data.availableStock;
}

function renderSalesChart(sales) {
    new Chart(document.getElementById("salesChart"), {
        type: "bar",
        data: {
            labels: sales.map(item => {
                const [year, month, day] = item.date.split("-");
                return `${day}/${month}/${year}`;
            }),
            datasets: [{
                label: "Faturamento (R$)",
                data: sales.map(item => item.revenue),
                backgroundColor: "#2563eb",
                borderRadius: 6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false
        }
    });
}

function renderOrdersChart(summary) {
    new Chart(document.getElementById("ordersChart"), {
        type: "doughnut",
        data: {
            labels: ["Pagos", "Pendentes", "Rejeitados"],
            datasets: [{
                data: [
                    summary.paidOrders,
                    summary.pendingOrders,
                    summary.failedOrders
                ],
                backgroundColor: [
                    "#16a34a",
                    "#f59e0b",
                    "#dc2626"
                ]
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false
        }
    });
}

function renderStockChart(stock) {
    new Chart(document.getElementById("stockChart"), {
        type: "bar",
        data: {
            labels: stock.map(item => item.productName),
            datasets: [{
                label: "Unidades disponíveis",
                data: stock.map(item => item.availableQuantity),
                backgroundColor: "#0891b2",
                borderRadius: 6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            indexAxis: "y"
        }
    });
}

async function loadDashboard() {
    if (!token) {
        window.location.href = "/login.html";
        return;
    }

    try {
        const [summary, charts] = await Promise.all([
            fetchDashboardData("/api/admin/dashboard"),
            fetchDashboardData("/api/admin/dashboard/charts")
        ]);

        renderSummary(summary);

        document.getElementById("dashboardContent").hidden = false;

        if (typeof Chart !== "undefined") {
            renderSalesChart(charts.salesByDay);
            renderOrdersChart(summary);
            renderStockChart(charts.stockByProduct);
        } else {
            showError(
                "Os indicadores foram carregados, mas a biblioteca de gráficos não está disponível."
            );
        }

    } catch (error) {
        if (error.message !== "Authentication required") {
            showError(error.message);
        }
        console.error(error);
    }
}

loadDashboard();
