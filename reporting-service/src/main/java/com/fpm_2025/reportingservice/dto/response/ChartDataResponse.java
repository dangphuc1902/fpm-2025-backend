package com.fpm_2025.reportingservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartDataResponse {
    
    private String chartType; // PIE, LINE, BAR
    private List<String> labels;
    private List<Dataset> datasets;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Dataset {
        private String label;
        private List<Object> data; // Can be Number or String
        private List<String> backgroundColor;
        private List<String> borderColor;
        private Integer borderWidth;
    }
}
