package com.example.pantry.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.pantry.model.PantryItem;

@RestController
@RequestMapping("/api/pantry")
@CrossOrigin(origins = "*")
public class PantryController {

    private final List<PantryItem> inventory = new ArrayList<>();
    private final Map<String, Double> wasteStats = new HashMap<>();
    private final Map<String, Double> intake = new HashMap<>();

    public PantryController() {
        inventory.add(new PantryItem(1L, "อกไก่สด (500g)", "🥩 เนื้อสัตว์", 65, LocalDate.now().plusDays(2), 165, 0, 45, 31, 3));
        inventory.add(new PantryItem(2L, "ผักกาดขาว", "🥦 ผักผลไม้", 20, LocalDate.now().plusDays(1), 12, 1, 5, 1, 0));
        inventory.add(new PantryItem(3L, "ช็อกโกแลตบาร์", "🧁 ของหวาน/ขนม", 45, LocalDate.now().plusDays(30), 220, 25, 30, 2, 12));
        inventory.add(new PantryItem(4L, "บะหมี่กึ่งสำเร็จรูป", "🥫 ของแห้ง", 10, LocalDate.now().plusDays(60), 280, 2, 1500, 6, 12));

        wasteStats.put("🥦 ผักผลไม้", 0.0);
        wasteStats.put("🥩 เนื้อสัตว์", 0.0);
        wasteStats.put("🥛 นม/เนย", 0.0);
        wasteStats.put("🥫 ของแห้ง", 0.0);
        wasteStats.put("🧁 ของหวาน/ขนม", 0.0);

        resetIntake();
    }

    @GetMapping("/items")
    public List<PantryItem> getInventoryFEFO() {
        return inventory.stream()
                .sorted(Comparator.comparing(PantryItem::getExpiry))
                .collect(Collectors.toList());
    }

    @PostMapping("/add")
    public PantryItem addItem(@RequestBody PantryItem item) {
        item.setId(System.currentTimeMillis());
        inventory.add(item);
        return item;
    }

    @PostMapping("/consume/{id}")
    public Map<String, Double> consumeItem(@PathVariable Long id) {
        Optional<PantryItem> found = inventory.stream().filter(i -> i.getId().equals(id)).findFirst();
        if (found.isPresent()) {
            PantryItem item = found.get();
            intake.put("sugar", intake.get("sugar") + item.getSugar());
            intake.put("sodium", intake.get("sodium") + item.getSodium());
            intake.put("protein", intake.get("protein") + item.getProtein());
            intake.put("fat", intake.get("fat") + item.getFat());
            inventory.remove(item);
        }
        return intake;
    }

    @PostMapping("/discard/{id}")
    public Map<String, Double> discardItem(@PathVariable Long id) {
        Optional<PantryItem> found = inventory.stream().filter(i -> i.getId().equals(id)).findFirst();
        if (found.isPresent()) {
            PantryItem item = found.get();
            wasteStats.put(item.getCategory(), wasteStats.getOrDefault(item.getCategory(), 0.0) + item.getPrice());
            inventory.remove(item);
        }
        return wasteStats;
    }

    @PostMapping("/freeze/{id}")
    public PantryItem freezeItem(@PathVariable Long id) {
        Optional<PantryItem> found = inventory.stream().filter(i -> i.getId().equals(id)).findFirst();
        if (found.isPresent()) {
            PantryItem item = found.get();
            if (!item.getName().contains("(แช่แข็ง)")) {
                item.setName(item.getName() + " (แช่แข็ง)");
                item.setExpiry(item.getExpiry().plusDays(30));
            }
            return item;
        }
        return null;
    }

    @GetMapping("/intake")
    public Map<String, Double> getIntake() {
        return intake;
    }

    @GetMapping("/waste-stats")
    public Map<String, Double> getWasteStats() {
        return wasteStats;
    }

    @PostMapping("/reset-intake")
    public String resetIntake() {
        intake.put("sugar", 0.0);
        intake.put("sodium", 0.0);
        intake.put("protein", 0.0);
        intake.put("fat", 0.0);
        return "Reset successful";
    }
}