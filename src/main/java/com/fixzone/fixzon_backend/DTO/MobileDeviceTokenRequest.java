package com.fixzone.fixzon_backend.DTO;

public class MobileDeviceTokenRequest {
    private String token;
    private String platform;
    private String deviceName;

    // Getters and Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
}
