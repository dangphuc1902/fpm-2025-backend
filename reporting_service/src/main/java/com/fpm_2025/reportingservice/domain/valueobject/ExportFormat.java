package com.fpm_2025.reportingservice.domain.valueobject;

public enum ExportFormat {
    PDF("application/pdf", ".pdf"),
    EXCEL("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx");

    private final String contentType;
    private final String extension;

    ExportFormat(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String getContentType() {
        return contentType;
    }

    public String getExtension() {
        return extension;
    }
}