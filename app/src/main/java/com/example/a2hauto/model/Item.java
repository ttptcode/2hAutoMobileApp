package com.example.a2hauto.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Item {
    @SerializedName("itemId")
    private String itemId;
    @SerializedName("itemTypeName")
    private String itemTypeName;
    @SerializedName("brand")
    private String brand;
    @SerializedName("model")
    private String model;
    @SerializedName("year")
    private Integer year;
    @SerializedName("condition")
    private String condition;
    @SerializedName("imageUrls")
    private List<String> imageUrls;
    @SerializedName("fuel")
    private String fuel;

    // Getters
    public String getItemId() { return itemId; }
    public String getItemTypeName() { return itemTypeName; }
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public Integer getYear() { return year; }
    public String getCondition() { return condition; }
    public List<String> getImageUrls() { return imageUrls; }
    public String getFuel() { return fuel; }

    public String getDisplayName() {
        if (brand == null && model == null) return "Chưa xác định";
        return (brand != null ? brand : "") + " " + (model != null ? model : "");
    }
}
