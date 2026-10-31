package com.example.pantry.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.pantry.model.PantryItem;

@RestController
@RequestMapping("/api/pantry")
public class PantryController {

    private final List<PantryItem> inventory = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public PantryController() {
        inventory.add(new PantryItem(idGenerator.getAndIncrement(), "อกไก่สด", "🥩 เนื้อสัตว์", 120.0, LocalDate.now().plusDays(2), 165, 0, 70, 31, 3.6));
        inventory.add(new PantryItem(idGenerator.getAndIncrement(), "ผักกาดขาว", "🥦 ผักผลไม้", 35.0, LocalDate.now().plusDays(1), 12, 1, 10, 1, 0.2));
    }

    @GetMapping("/items")
    public List<PantryItem> getItems() {
        inventory.sort(Comparator.comparing(PantryItem::getExpiry));
        return inventory;
    }

    @PostMapping("/add")
    public ResponseEntity<?> addItem(@RequestBody PantryItem item) {
        if (item.getName() == null || item.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("ชื่อรายการต้องไม่เป็นค่าว่าง");
        }
        if (item.getExpiry() == null) {
            item.setExpiry(LocalDate.now().plusDays(7));
        }
        item.setId(idGenerator.getAndIncrement());
        inventory.add(item);
        return ResponseEntity.ok(item);
    }

    @PostMapping("/consume/{id}")
    public ResponseEntity<?> consumeItem(@PathVariable Long id) {
        inventory.removeIf(item -> item.getId().equals(id));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/discard/{id}")
    public ResponseEntity<?> discardItem(@PathVariable Long id) {
        inventory.removeIf(item -> item.getId().equals(id));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/freeze/{id}")
    public ResponseEntity<?> freezeItem(@PathVariable Long id) {
        for (PantryItem item : inventory) {
            if (item.getId().equals(id)) {
                if (!item.getName().contains("(แช่แข็ง)")) {
                    item.setName(item.getName() + " (แช่แข็ง)");
                    item.setExpiry(item.getExpiry().plusDays(30));
                }
                break;
            }
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/intake")
    public Map<String, Double> getIntake() {
        Map<String, Double> intake = new HashMap<>();
        intake.put("sugar", 0.0);
        intake.put("sodium", 0.0);
        intake.put("protein", 40.0);
        intake.put("fat", 27.0);
        return intake;
    }

    @GetMapping("/waste-stats")
    public Map<String, Double> getWasteStats() {
        Map<String, Double> stats = new LinkedHashMap<>();
        stats.put("ผักผลไม้", 35.0);
        stats.put("เนื้อสัตว์", 120.0);
        stats.put("ของหวาน/ขนม", 0.0);
        return stats;
    }

    @PostMapping("/reset-intake")
    public ResponseEntity<?> resetIntake() {
        return ResponseEntity.ok().build();
    }
}