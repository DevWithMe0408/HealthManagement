package org.example.nutritionservice.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.nutritionservice.dto.response.CatalogStatsResponse;
import org.example.nutritionservice.service.dashboard.DashboardStatsService;
import org.example.web.dto.response.DataResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardStatsService dashboardStatsService;

    @GetMapping("/catalog-stats")
    public DataResponse<CatalogStatsResponse> getCatalogStats() {
        return DataResponse.success(dashboardStatsService.getCatalogStats());
    }
}
