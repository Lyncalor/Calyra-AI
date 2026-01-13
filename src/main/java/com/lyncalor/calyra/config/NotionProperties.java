package com.lyncalor.calyra.config;

import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "notion")
public class NotionProperties {

    public enum SourcePropertyType {
        SELECT,
        RICH_TEXT
    }

    private boolean enabled = true;
    private String token;
    private String databaseId;
    private String notionVersion = "2022-06-28";
    private String apiBaseUrl = "https://api.notion.com";
    private String propertyNameTitle = "Name";
    private String propertyNameSource = "Source";
    private String propertyNameRaw = "Raw";
    private String propertyNameStart = "Start";
    private String propertyNameEnd = "End";
    private String propertyNameType = "Type";
    private String propertyNameLocation = "Location";
    private String propertyNameReminder = "Reminder";
    private SourcePropertyType propertyTypeSource = SourcePropertyType.SELECT;

    @AssertTrue(message = "notion.token and notion.database-id must be set when notion.enabled=true")
    public boolean isNotionConfigValid() {
        if (!enabled) {
            return true;
        }
        return token != null && !token.isBlank() && databaseId != null && !databaseId.isBlank();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getDatabaseId() {
        return databaseId;
    }

    public void setDatabaseId(String databaseId) {
        this.databaseId = databaseId;
    }

    public String getNotionVersion() {
        return notionVersion;
    }

    public void setNotionVersion(String notionVersion) {
        this.notionVersion = notionVersion;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getPropertyNameTitle() {
        return propertyNameTitle;
    }

    public void setPropertyNameTitle(String propertyNameTitle) {
        this.propertyNameTitle = propertyNameTitle;
    }

    public String getPropertyNameSource() {
        return propertyNameSource;
    }

    public void setPropertyNameSource(String propertyNameSource) {
        this.propertyNameSource = propertyNameSource;
    }

    public String getPropertyNameRaw() {
        return propertyNameRaw;
    }

    public void setPropertyNameRaw(String propertyNameRaw) {
        this.propertyNameRaw = propertyNameRaw;
    }

    public String getPropertyNameStart() {
        return propertyNameStart;
    }

    public void setPropertyNameStart(String propertyNameStart) {
        this.propertyNameStart = propertyNameStart;
    }

    public String getPropertyNameEnd() {
        return propertyNameEnd;
    }

    public void setPropertyNameEnd(String propertyNameEnd) {
        this.propertyNameEnd = propertyNameEnd;
    }

    public String getPropertyNameType() {
        return propertyNameType;
    }

    public void setPropertyNameType(String propertyNameType) {
        this.propertyNameType = propertyNameType;
    }

    public String getPropertyNameLocation() {
        return propertyNameLocation;
    }

    public void setPropertyNameLocation(String propertyNameLocation) {
        this.propertyNameLocation = propertyNameLocation;
    }

    public String getPropertyNameReminder() {
        return propertyNameReminder;
    }

    public void setPropertyNameReminder(String propertyNameReminder) {
        this.propertyNameReminder = propertyNameReminder;
    }

    public SourcePropertyType getPropertyTypeSource() {
        return propertyTypeSource;
    }

    public void setPropertyTypeSource(SourcePropertyType propertyTypeSource) {
        this.propertyTypeSource = propertyTypeSource;
    }
}
