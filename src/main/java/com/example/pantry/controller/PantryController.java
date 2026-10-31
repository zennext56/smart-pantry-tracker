package com.example.pantry.controller;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    private final ConcurrentHashMap<String, PantryItem> pantryStorage = new ConcurrentHashMap<>();
    private double totalWasteValue = 0.0;

    @GetMapping
    public List<PantryItem> getAllItems() {
        return pantryStorage.values().stream()
                .sorted(Comparator.comparing(PantryItem::getExpiryDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    // รองรับทั้ง /api/pantry และ /api/pantry/add ป้องกันข้อผิดพลาด Method Not Allowed 405
    @PostMapping({"", "/add"})
    public PantryItem addItem(@RequestBody PantryItem item) {
        if (item.getId() == null || item.getId().isEmpty()) {
            item.setId(UUID.randomUUID().toString());
        }
        if (item.getQuantity() <= 0) {
            item.setQuantity(1);
        }
        pantryStorage.put(item.getId(), item);
        return item;
    }

    // 🍽️ กินวัตถุดิบ (ลดจำนวนลง 1)
    @PostMapping("/{id}/eat")
    public Map<String, Object> eatItem(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        PantryItem item = pantryStorage.get(id);
        if (item != null) {
            item.setQuantity(item.getQuantity() - 1);
            if (item.getQuantity() <= 0) {
                pantryStorage.remove(id);
                response.put("removed", true);
            } else {
                response.put("removed", false);
            }
            response.put("consumedItem", item);
        }
        return response;
    }

    // ❄️ แช่แข็งวัตถุดิบ (เพิ่มวันหมดอายุ 30 วัน)
    @PostMapping("/{id}/freeze")
    public PantryItem freezeItem(@PathVariable String id) {
        PantryItem item = pantryStorage.get(id);
        if (item != null) {
            boolean currentStatus = item.isFrozen();
            item.setFrozen(!currentStatus);
            if (!currentStatus && item.getExpiryDate() != null && !item.getExpiryDate().isEmpty()) {
                try {
                    LocalDate date = LocalDate.parse(item.getExpiryDate());
                    item.setExpiryDate(date.plusDays(30).toString());
                } catch (Exception ignored) {}
            }
        }
        return item;
    }

    // 🗑️ ทิ้งวัตถุดิบ (คำนวณมูลค่าขยะอาหาร)
    @PostMapping("/{id}/discard")
    public Map<String, Object> discardItem(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        PantryItem item = pantryStorage.remove(id);
        if (item != null) {
            double itemValue = item.getPrice() * item.getQuantity();
            totalWasteValue += itemValue;
            response.put("discardedValue", itemValue);
        }
        response.put("totalWasteValue", totalWasteValue);
        return response;
    }

    @DeleteMapping("/{id}")
    public void deleteItem(@PathVariable String id) {
        pantryStorage.remove(id);
    }

    @GetMapping("/waste")
    public Map<String, Double> getWasteSummary() {
        return Collections.singletonMap("totalWasteValue", totalWasteValue);
    }

    // 🎲 จำลองข้อมูลวัตถุดิบ
    @PostMapping("/seed")
    public List<PantryItem> seedMockData() {
        LocalDate now = LocalDate.now();
        List<PantryItem> mockList = Arrays.asList(
            new PantryItem("8850100121011", "มันฝรั่งทอดกรอบ", "ของหวาน/ขนม", 30.0, now.plusDays(10).toString(), 150, 1.0, 350.0, 2.0, 10.0, 1),
            new PantryItem("8850123456789", "นมสดพาสเจอร์ไรส์ 200ml", "เครื่องดื่ม", 26.0, now.plusDays(2).toString(), 120, 10.0, 80.0, 7.0, 6.0, 2)
        );
        for (PantryItem item : mockList) {
            pantryStorage.put(item.getId(), item);
        }
        return getAllItems();
    }
}