package com.example.pantry.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.ResponseEntity;
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

    private final List<PantryItem> inventory = Collections.synchronizedList(new ArrayList<>());
    private final AtomicLong idGenerator = new AtomicLong(1);

    // ตัวแปรเก็บค่าโภชนาการที่กินไปแล้วประจำวัน (สะสมจริง)
    private final Map<String, Double> dailyIntake = new ConcurrentHashMap<>();
    
    // ตัวแปรเก็บสถิติขยะอาหารแยกตามหมวดหมู่ (สะสมจริง)
    private final Map<String, Double> wasteStats = new ConcurrentHashMap<>();

    public PantryController() {
        resetIntakeData();
        resetWasteData();

        // ข้อมูลเริ่มต้นสำหรับทดสอบ
        inventory.add(new PantryItem(idGenerator.getAndIncrement(), "อกไก่สด", "🥩 เนื้อสัตว์", 120.0, LocalDate.now().plusDays(2), 165, 0, 70, 31, 3.6));
        inventory.add(new PantryItem(idGenerator.getAndIncrement(), "ผักกาดขาว", "🥦 ผักผลไม้", 35.0, LocalDate.now().plusDays(1), 12, 1, 10, 1, 0.2));
    }

    private void resetIntakeData() {
        dailyIntake.put("sugar", 0.0);
        dailyIntake.put("sodium", 0.0);
        dailyIntake.put("protein", 0.0);
        dailyIntake.put("fat", 0.0);
    }

    private void resetWasteData() {
        wasteStats.put("🥦 ผักผลไม้", 0.0);
        wasteStats.put("🥩 เนื้อสัตว์", 0.0);
        wasteStats.put("🥛 นม/เนย", 0.0);
        wasteStats.put("🧁 ของหวาน/ขนม", 0.0);
        wasteStats.put("🥫 ของแห้ง", 0.0);
    }

    @GetMapping("/items")
    public List<PantryItem> getItems() {
        synchronized (inventory) {
            inventory.sort(Comparator.comparing(PantryItem::getExpiry, Comparator.nullsLast(Comparator.naturalOrder())));
            return new ArrayList<>(inventory);
        }
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

    // 🍽️ เมื่อกดกิน: นำค่าโภชนาการไปบวกสะสมใน dailyIntake แล้วจึงลบออกจากคลัง
    @PostMapping("/consume/{id}")
    public ResponseEntity<?> consumeItem(@PathVariable Long id) {
        synchronized (inventory) {
            Iterator<PantryItem> iterator = inventory.iterator();
            while (iterator.hasNext()) {
                PantryItem item = iterator.next();
                if (item.getId().equals(id)) {
                    dailyIntake.put("sugar", dailyIntake.getOrDefault("sugar", 0.0) + item.getSugar());
                    dailyIntake.put("sodium", dailyIntake.getOrDefault("sodium", 0.0) + item.getSodium());
                    dailyIntake.put("protein", dailyIntake.getOrDefault("protein", 0.0) + item.getProtein());
                    dailyIntake.put("fat", dailyIntake.getOrDefault("fat", 0.0) + item.getFat());

                    iterator.remove();
                    break;
                }
            }
        }
        return ResponseEntity.ok().build();
    }

    // 🗑️ เมื่อกดทิ้ง: นำราคาสินค้าไปบวกเข้ากราฟขยะอาหาร
    @PostMapping("/discard/{id}")
    public ResponseEntity<?> discardItem(@PathVariable Long id) {
        synchronized (inventory) {
            Iterator<PantryItem> iterator = inventory.iterator();
            while (iterator.hasNext()) {
                PantryItem item = iterator.next();
                if (item.getId().equals(id)) {
                    String category = item.getCategory() != null ? item.getCategory() : "🥫 ของแห้ง";
                    wasteStats.put(category, wasteStats.getOrDefault(category, 0.0) + item.getPrice());
                    iterator.remove();
                    break;
                }
            }
        }
        return ResponseEntity.ok().build();
    }

    // ❄️ เมื่อกดฟรีซ: ขยายวันหมดอายุ 30 วัน
    @PostMapping("/freeze/{id}")
    public ResponseEntity<?> freezeItem(@PathVariable Long id) {
        synchronized (inventory) {
            for (PantryItem item : inventory) {
                if (item.getId().equals(id)) {
                    if (!item.getName().contains("(แช่แข็ง)")) {
                        item.setName(item.getName() + " (แช่แข็ง)");
                        item.setExpiry(item.getExpiry().plusDays(30));
                    }
                    break;
                }
            }
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/intake")
    public Map<String, Double> getIntake() {
        return dailyIntake;
    }

    @GetMapping("/waste-stats")
    public Map<String, Double> getWasteStats() {
        return wasteStats;
    }

    // 🔄 ปุ่มรีเซ็ตค่าโภชนาการประจำวัน
    @PostMapping("/reset-intake")
    public ResponseEntity<?> resetIntake() {
        resetIntakeData();
        return ResponseEntity.ok().build();
    }
}