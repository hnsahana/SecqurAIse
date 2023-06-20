package com.example.sequraise;

public class UploadData {
    private String captureCount, frequency, connectivity, chargingStatus, chargeLevel, location;
    private String imageUrl;

    public UploadData(String captureCount, String frequency, String connectivity, String chargingStatus, String chargeLevel, String location, String imageUrl) {
        this.captureCount = captureCount;
        this.frequency = frequency;
        this.connectivity = connectivity;
        this.chargingStatus = chargingStatus;
        this.chargeLevel = chargeLevel;
        this.location = location;
        this.imageUrl = imageUrl;
    }

    public UploadData(){

    }


    public String getCaptureCount() {
        return captureCount;
    }

    public void setCaptureCount(String captureCount) {
        this.captureCount = captureCount;
    }


    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getConnectivity() {
        return connectivity;
    }

    public void setConnectivity(String connectivity) {
        this.connectivity = connectivity;
    }

    public String getChargingStatus() {
        return chargingStatus;
    }

    public void setChargingStatus(String chargingStatus) {
        this.chargingStatus = chargingStatus;
    }

    public String getChargeLevel() {
        return chargeLevel;
    }

    public void setChargeLevel(String chargeLevel) {
        this.chargeLevel = chargeLevel;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
