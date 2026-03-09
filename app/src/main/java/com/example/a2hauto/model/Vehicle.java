package com.example.a2hauto.model;

import com.google.gson.annotations.SerializedName;

public class Vehicle {
    @SerializedName("id")
    private int id;
    @SerializedName("name")
    private String name;
    @SerializedName("description")
    private String description;
    @SerializedName("price")
    private double price;
    @SerializedName("imageUrl")
    private String imageUrl;
    @SerializedName("categoryId")
    private int categoryId;

    public Vehicle(int id, String name, String description, double price, String imageUrl, int categoryId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.categoryId = categoryId;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }
    public int getCategoryId() { return categoryId; }
}
