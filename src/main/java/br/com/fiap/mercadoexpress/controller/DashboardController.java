
package br.com.fiap.mercadoexpress.controller;

import br.com.fiap.mercadoexpress.dto.response.DashboardProfitResponse;
import br.com.fiap.mercadoexpress.dto.response.DashboardSummaryResponse;
import br.com.fiap.mercadoexpress.service.DashboardService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import br.com.fiap.mercadoexpress.dto.response.DashboardChartsResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardSummaryResponse getSummary() {
        return dashboardService.getSummary();
    }

    @GetMapping("/charts")
    public DashboardChartsResponse getCharts() {
        return dashboardService.getCharts();
    }

    @GetMapping("/profit")
    public DashboardProfitResponse getProfitReport() {
        return dashboardService.getProfitReport();
    }
}
