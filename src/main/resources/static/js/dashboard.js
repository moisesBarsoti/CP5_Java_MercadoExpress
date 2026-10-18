
(() => {
    "use strict";

    const token = sessionStorage.getItem("accessToken");

    const logoutButton = document.getElementById("logoutButton");
    const dashboardContent = document.getElementById("dashboardContent");
    const errorMessage = document.getElementById("errorMessage");

    const currencyFormatter = new Intl.NumberFormat("pt-BR", {
        style: "currency",
        currency: "BRL"
    });

    logoutButton.addEventListener("click", () => {
        sessionStorage.removeItem("accessToken");
        window.location.href = "/login.html";
    });

    function showError(message) {
        errorMessage.textContent = message;
        errorMessage.hidden = false;
    }

    function formatDate(date) {
        const [year, month, day] = date.split("-");
        return `${day}/${month}/${year}`;
    }

    async function fetchDashboardData(endpoint) {
        const response = await fetch(endpoint, {
            method: "GET",
            headers: {
                Accept: "application/json",
                Authorization: `Bearer ${token}`
            },
            cache: "no-store"
        });

        if (response.status === 401) {
            sessionStorage.removeItem("accessToken");

            throw new Error(
                "Sua sessão expirou. Faça login novamente."
            );
        }

        if (response.status === 403) {
            throw new Error(
                "Acesso negado. Apenas administradores podem " +
                "visualizar o dashboard."
            );
        }

        if (!response.ok) {
            throw new Error(
                `Erro ao carregar os dados. HTTP ${response.status}`
            );
        }

        return response.json();
    }

    function renderSummary(summary) {
        document.getElementById("totalRevenue").textContent =
            currencyFormatter.format(summary.totalRevenue);

        document.getElementById("totalOrders").textContent =
            summary.totalOrders;

        document.getElementById("paidOrders").textContent =
            summary.paidOrders;

        document.getElementById("availableStock").textContent =
            summary.availableStock;
    }

    function renderProfitSummary(profit) {
        document.getElementById("recordedGrossProfit").textContent =
            currencyFormatter.format(profit.recordedGrossProfit);

        document.getElementById("profitCoverage").textContent =
            `${profit.fullyCostedOrders} pedido(s) com custo completo`;

        const warning = document.getElementById("profitWarning");

        if (profit.ordersMissingCost > 0) {
            warning.textContent =
                `${profit.ordersMissingCost} pedido(s) pago(s) ` +
                "não possuem custo histórico completo. " +
                "O lucro apresentado considera apenas os itens " +
                "com custo conhecido e não representa o lucro " +
                "total de todas as vendas.";

            warning.hidden = false;
        } else {
            warning.textContent = "";
            warning.hidden = true;
        }
    }

    function renderSalesChart(sales) {
        new Chart(document.getElementById("salesChart"), {
            type: "bar",
            data: {
                labels: sales.map(item => formatDate(item.date)),
                datasets: [{
                    label: "Faturamento (R$)",
                    data: sales.map(item => Number(item.revenue)),
                    backgroundColor: "#2563eb",
                    borderRadius: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: value =>
                                currencyFormatter.format(value)
                        }
                    }
                },
                plugins: {
                    legend: {
                        display: true
                    }
                }
            }
        });
    }

    function renderOrdersChart(summary) {
        new Chart(document.getElementById("ordersChart"), {
            type: "doughnut",
            data: {
                labels: [
                    "Pagos",
                    "Pendentes",
                    "Rejeitados"
                ],
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
                    ],
                    borderWidth: 2
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: "top"
                    }
                }
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
                    data: stock.map(item =>
                        Number(item.availableQuantity)
                    ),
                    backgroundColor: "#0891b2",
                    borderRadius: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                indexAxis: "y",
                scales: {
                    x: {
                        beginAtZero: true,
                        ticks: {
                            precision: 0
                        }
                    }
                }
            }
        });
    }

    function renderProfitChart(profitByDay) {
        const values = profitByDay.map(item => {
            const recordedProfit = Number(
                item.recordedGrossProfit
            );

            const hasMissingCost =
                Number(item.ordersMissingCost) > 0;

            const hasCompleteOrders =
                Number(item.fullyCostedOrders) > 0;

            if (
                !hasCompleteOrders &&
                hasMissingCost &&
                recordedProfit === 0
            ) {
                return null;
            }

            return recordedProfit;
        });

        new Chart(document.getElementById("profitChart"), {
            type: "bar",
            data: {
                labels: profitByDay.map(item =>
                    formatDate(item.date)
                ),
                datasets: [{
                    label: "Lucro bruto registrado (R$)",
                    data: values,
                    backgroundColor: "#16a34a",
                    borderRadius: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        ticks: {
                            callback: value =>
                                currencyFormatter.format(value)
                        }
                    }
                },
                plugins: {
                    legend: {
                        display: true
                    },
                    tooltip: {
                        callbacks: {
                            afterBody: context => {
                                if (context.length === 0) {
                                    return "";
                                }

                                const index = context[0].dataIndex;
                                const day = profitByDay[index];

                                if (day.ordersMissingCost > 0) {
                                    return (
                                        `${day.ordersMissingCost} pedido(s) ` +
                                        "sem custo histórico completo"
                                    );
                                }

                                return "";
                            }
                        }
                    }
                }
            }
        });
    }

    async function loadDashboard() {
        if (!token) {
            window.location.href = "/login.html";
            return;
        }

        errorMessage.hidden = true;
        dashboardContent.hidden = true;

        try {
            const [summary, charts, profit] =
                await Promise.all([
                    fetchDashboardData("/api/admin/dashboard"),
                    fetchDashboardData("/api/admin/dashboard/charts"),
                    fetchDashboardData("/api/admin/dashboard/profit")
                ]);

            renderSummary(summary);
            renderProfitSummary(profit);

            dashboardContent.hidden = false;

            if (typeof Chart === "undefined") {
                showError(
                    "Os indicadores foram carregados, mas " +
                    "a biblioteca de gráficos não está disponível."
                );

                return;
            }

            renderSalesChart(charts.salesByDay);
            renderOrdersChart(summary);
            renderStockChart(charts.stockByProduct);
            renderProfitChart(profit.profitByDay);

        } catch (error) {
            console.error(
                "Erro ao carregar dashboard:",
                error
            );

            showError(error.message);
        }
    }

    loadDashboard();
})();
