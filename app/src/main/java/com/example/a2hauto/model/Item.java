package com.example.a2hauto.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class Item implements Serializable {
    @SerializedName("itemId")
    private String itemId;
    @SerializedName("itemTypeId")
    private String itemTypeId;
    @SerializedName("title")
    private String title;
    @SerializedName("serialNumber")
    private String serialNumber;
    @SerializedName("itemTypeName")
    private String itemTypeName;
    @SerializedName("brand")
    private String brand;
    @SerializedName("model")
    private String model;
    @SerializedName("year")
    private Integer year;
    @SerializedName("mileage")
    private String mileage;
    @SerializedName("condition")
    private String condition;
    @SerializedName("imageUrls")
    private List<String> imageUrls;
    @SerializedName("videoUrl")
    private String videoUrl;
    @SerializedName("videoUrls")
    private List<String> videoUrls;
    @SerializedName("color")
    private String color;
    @SerializedName("seat")
    private String seat;
    @SerializedName("origin")
    private String origin;
    @SerializedName("fuel")
    private String fuel;
    @SerializedName("gearbox")
    private String gearbox;
    @SerializedName("ownerCount")
    private String ownerCount;
    @SerializedName("style")
    private String style;
    @SerializedName("licensePlate")
    private String licensePlate;

    // Getters
    public String getItemId() { return itemId; }
    public String getItemTypeId() { return itemTypeId; }
    public String getTitle() { return title; }
    public String getSerialNumber() { return serialNumber; }
    public String getItemTypeName() { return itemTypeName; }
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public Integer getYear() { return year; }
    public String getMileage() { return mileage; }
    public String getCondition() { return condition; }
    public List<String> getImageUrls() { return imageUrls; }
    public String getVideoUrl() { return videoUrl; }
    public List<String> getVideoUrls() { return videoUrls; }
    public String getColor() { return color; }
    public String getSeat() { return seat; }
    public String getOrigin() { return origin; }
    public String getFuel() { return fuel; }
    public String getGearbox() { return gearbox; }
    public String getOwnerCount() { return ownerCount; }
    public String getStyle() { return style; }
    public String getLicensePlate() { return licensePlate; }

    public String getDisplayName() {
        if (title != null && !title.isEmpty()) {
            return title;
        }
        if (brand == null && model == null) return "Chưa xác định";
        return (brand != null ? brand : "") + " " + (model != null ? model : "");
    }
}
