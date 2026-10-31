package com.example.pantry.model;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

public class PantryItem {
    private Long id;
    private String name;
    private String category;
    private double price;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expiry;

    private double cal;
    private double sugar;
    private double sodium;
    private double protein;
    private double fat;

    public PantryItem() {}

    public PantryItem(Long id, String name, String category, double price, LocalDate expiry, 
                      double cal, double sugar, double sodium, double protein, double fat) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.expiry = expiry;
        this.cal = cal;
        this.sugar = sugar;
        this.sodium = sodium;
        this.protein = protein;
        this.fat = fat;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public LocalDate getExpiry() { return expiry; }
    public void setExpiry(LocalDate expiry) { this.expiry = expiry; }
    public double getCal() { return cal; }
    public void setCal(double cal) { this.cal = cal; }
    public double getSugar() { return sugar; }
    public void setSugar(double sugar) { this.sugar = sugar; }
    public double getSodium() { return sodium; }
    public void setSodium(double sodium) { this.sodium = sodium; }
    public double getProtein() { return protein; }
    public void setProtein(double protein) { this.protein = protein; }
    public double getFat() { return fat; }
    public void setFat(double fat) { this.fat = fat; }
}