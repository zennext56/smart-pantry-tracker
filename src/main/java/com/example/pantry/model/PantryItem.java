package com.example.pantry.model;

import java.util.UUID;

public class PantryItem {
    private String id;
    private String barcode;
    private String name;
    private String category;
    private double price;
    private String expiryDate;
    private int calories;
    private double sugar;
    private double sodium;
    private double protein;
    private double fat;
    private int quantity;
    private boolean frozen;

    public PantryItem() {
        this.id = UUID.randomUUID().toString();
        this.quantity = 1;
        this.frozen = false;
    }

    public PantryItem(String barcode, String name, String category, double price, String expiryDate, 
                      int calories, double sugar, double sodium, double protein, double fat, int quantity) {
        this.id = UUID.randomUUID().toString();
        this.barcode = barcode;
        this.name = name;
        this.category = category;
        this.price = price;
        this.expiryDate = expiryDate;
        this.calories = calories;
        this.sugar = sugar;
        this.sodium = sodium;
        this.protein = protein;
        this.fat = fat;
        this.quantity = quantity > 0 ? quantity : 1;
        this.frozen = false;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public int getCalories() { return calories; }
    public void setCalories(int calories) { this.calories = calories; }

    public double getSugar() { return sugar; }
    public void setSugar(double sugar) { this.sugar = sugar; }

    public double getSodium() { return sodium; }
    public void setSodium(double sodium) { this.sodium = sodium; }

    public double getProtein() { return protein; }
    public void setProtein(double protein) { this.protein = protein; }

    public double getFat() { return fat; }
    public void setFat(double fat) { this.fat = fat; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public boolean isFrozen() { return frozen; }
    public void setFrozen(boolean frozen) { this.frozen = frozen; }
}