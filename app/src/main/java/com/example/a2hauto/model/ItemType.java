package com.example.a2hauto.model;

import com.google.gson.annotations.SerializedName;

public class ItemType {
    @SerializedName("itemTypeId")
    private String itemTypeId;
    @SerializedName("name")
    private String name;
    @SerializedName("description")
    private String description;

    public String getItemTypeId() { return itemTypeId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
}
