package com.interexport.guarantees.entity.enums;

/**
 * File types supported for F12 Data Migration
 * Defines the format of import files from legacy systems
 */
public enum ImportFileType {
    CSV("Comma Separated Values", ".csv", "text/csv"),
    XML("eXtensible Markup Language", ".xml", "application/xml"),
    JSON("JavaScript Object Notation", ".json", "application/json"),
    EXCEL("Microsoft Excel", ".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    FIXED_WIDTH("Fixed Width Text", ".txt", "text/plain"),
    DOKA_LEGACY("Doka Legacy Format", ".dka", "application/octet-stream");

    private final String description;
    private final String extension;
    private final String mimeType;

    ImportFileType(String description, String extension, String mimeType) {
        this.description = description;
        this.extension = extension;
        this.mimeType = mimeType;
    }

    public String getDescription() {
        return description;
    }

    public String getExtension() {
        return extension;
    }

    public String getMimeType() {
        return mimeType;
    }
}




