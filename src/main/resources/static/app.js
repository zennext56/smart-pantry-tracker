const API_BASE = "/api/pantry";

let limits = { sugar: 24, sodium: 2000, protein: 50, fat: 65 };
let wasteChart;
let currentInventory = [];
let html5QrcodeScanner = null;

// --- API Calls ---
async function fetchInventory() {
    try {
        const res = await fetch(`${API_BASE}/items`);
        if (!res.ok) throw new Error("ไม่สามารถดึงข้อมูลรายการได้");
        currentInventory = await res.json();
        renderTable(currentInventory);
    } catch (err) {
        console.error("Error fetching inventory:", err);
    }
}

async function fetchIntake() {
    try {
        const res = await fetch(`${API_BASE}/intake`);
        if (!res.ok) throw new Error("Network error");
        const intake = await res.json();
        updateNutritionUI(intake);
    } catch (err) {
        console.error("Error fetching intake:", err);
    }
}

async function fetchWasteStats() {
    try {
        const res = await fetch(`${API_BASE}/waste-stats`);
        if (!res.ok) throw new Error("Network error");
        const stats = await res.json();
        renderChart(stats);
    } catch (err) {
        console.error("Error fetching waste stats:", err);
    }
}

// --- Barcode Scanner Logic ---
async function startBarcodeScanner() {
    const scannerContainer = document.getElementById('scanner-container');
    if (scannerContainer) scannerContainer.classList.remove('d-none');
    
    if (!html5QrcodeScanner) {
        html5QrcodeScanner = new Html5Qrcode("reader");
    }

    const config = { 
        fps: 15, 
        qrbox: { width: 280, height: 120 },
        experimentalFeatures: { useBarCodeDetectorIfSupported: true }
    };

    try {
        await html5QrcodeScanner.start(
            { facingMode: "environment" }, 
            config, 
            onScanSuccess
        );
    } catch (err) {
        alert("ไม่สามารถเข้าถึงกล้องได้: " + err);
        if (scannerContainer) scannerContainer.classList.add('d-none');
    }
}

async function stopBarcodeScanner() {
    if (html5QrcodeScanner && html5QrcodeScanner.isScanning) {
        await html5QrcodeScanner.stop();
    }
    const scannerContainer = document.getElementById('scanner-container');
    if (scannerContainer) scannerContainer.classList.add('d-none');
}

async function scanBarcodeFromFile(event) {
    const file = event.target.files[0];
    if (!file) return;

    await stopBarcodeScanner();

    if ('BarcodeDetector' in window) {
        try {
            const barcodeDetector = new BarcodeDetector();
            const imageBitmap = await createImageBitmap(file);
            const barcodes = await barcodeDetector.detect(imageBitmap);
            
            if (barcodes.length > 0) {
                const decodedText = barcodes[0].rawValue;
                alert(`📌 อ่านบาร์โค้ดสำเร็จ! รหัส: ${decodedText}`);
                fetchProductByBarcode(decodedText);
                event.target.value = '';
                return;
            }
        } catch (e) {
            console.warn("Native BarcodeDetector error", e);
        }
    }

    if (!html5QrcodeScanner) {
        html5QrcodeScanner = new Html5Qrcode("reader");
    }

    try {
        const decodedText = await html5QrcodeScanner.scanFile(file, true);
        alert(`📌 อ่านบาร์โค้ดสำเร็จ! รหัส: ${decodedText}`);
        fetchProductByBarcode(decodedText);
    } catch (err) {
        const manualBarcode = prompt("❌ อ่านรูปภาพไม่สำเร็จ กรุณากรอกเลขบาร์โค้ดสินค้า (เช่น 8850100121011):");
        if (manualBarcode && manualBarcode.trim() !== "") {
            fetchProductByBarcode(manualBarcode.trim());
        }
    } finally {
        event.target.value = '';
    }
}

async function onScanSuccess(decodedText) {
    await stopBarcodeScanner();
    alert(`📌 อ่านบาร์โค้ดสำเร็จ! รหัส: ${decodedText}`);
    fetchProductByBarcode(decodedText);
}

async function fetchProductByBarcode(barcode) {
    const nameInput = document.getElementById('f_name');
    const categoryInput = document.getElementById('f_category');
    const calInput = document.getElementById('f_cal');
    const sugarInput = document.getElementById('f_sugar');
    const sodiumInput = document.getElementById('f_sodium');
    const proteinInput = document.getElementById('f_protein');
    const fatInput = document.getElementById('f_fat');
    const expiryInput = document.getElementById('f_expiry');

    try {
        const res = await fetch(`https://world.openfoodfacts.org/api/v0/product/${barcode}.json`);
        const data = await res.json();

        if (data.status === 1 && data.product) {
            const p = data.product;
            const nutriments = p.nutriments || {};

            // 1. ดึงชื่อสินค้า
            if (nameInput) nameInput.value = p.product_name_th || p.product_name || `สินค้า (${barcode})`;

            // 2. วิเคราะห์หมวดหมู่สินค้าอัตโนมัติ เพื่อประเมินวันหมดอายุที่เหมาะสม
            let category = "🥫 ของแห้ง";
            let daysToAdd = 30; // ของแห้งデフォルト 30 วัน

            const categoriesStr = (p.categories || "").toLowerCase();
            if (categoriesStr.includes("beverage") || categoriesStr.includes("milk") || categoriesStr.includes("dairy") || categoriesStr.includes("นม")) {
                category = "🥛 นม/เนย";
                daysToAdd = 7;
            } else if (categoriesStr.includes("meat") || categoriesStr.includes("chicken") || categoriesStr.includes("pork") || categoriesStr.includes("seafood") || categoriesStr.includes("เนื้อ")) {
                category = "🥩 เนื้อสัตว์";
                daysToAdd = 3;
            } else if (categoriesStr.includes("fruit") || categoriesStr.includes("vegetable") || categoriesStr.includes("ผัก")) {
                category = "🥦 ผักผลไม้";
                daysToAdd = 5;
            } else if (categoriesStr.includes("snack") || categoriesStr.includes("sweet") || categoriesStr.includes("dessert") || categoriesStr.includes("ขนม")) {
                category = "🧁 ของหวาน/ขนม";
                daysToAdd = 60;
            }

            if (categoryInput) categoryInput.value = category;

            // 3. คำนวณวันหมดอายุตามประเภทสินค้า
            const d = new Date(); 
            d.setDate(d.getDate() + daysToAdd);
            if (expiryInput) expiryInput.value = d.toISOString().split('T')[0];

            // 4. ดึงค่าโภชนาการแบบค้นหาทุกช่องข้อมูล (Serving & 100g Fallback)
            const getNutriment = (keys) => {
                for (let key of keys) {
                    if (nutriments[key] !== undefined && nutriments[key] !== null) {
                        return Math.round(parseFloat(nutriments[key]));
                    }
                }
                return 0;
            };

            if (calInput) calInput.value = getNutriment(['energy-kcal_serving', 'energy-kcal_100g', 'energy-kcal', 'energy-kcal_value']);
            if (sugarInput) sugarInput.value = getNutriment(['sugars_serving', 'sugars_100g', 'sugars', 'sugars_value']);
            if (proteinInput) proteinInput.value = getNutriment(['proteins_serving', 'proteins_100g', 'proteins', 'proteins_value']);
            if (fatInput) fatInput.value = getNutriment(['fat_serving', 'fat_100g', 'fat', 'fat_value']);
            
            // แปลงโซเดียม (กรัม ➔ มิลลิกรัม)
            let sodiumVal = nutriments['sodium_serving'] ?? nutriments['sodium_100g'] ?? nutriments['sodium'] ?? 0;
            if (sodiumVal > 0 && sodiumVal < 10) sodiumVal = sodiumVal * 1000; // หากมาเป็นกรัม ให้คูณ 1000 แปลงเป็น mg
            if (sodiumInput) sodiumInput.value = Math.round(sodiumVal);

        } else {
            // สินค้าที่ไม่มีในระบบ Open Food Facts
            if (nameInput) nameInput.value = `สินค้าบาร์โค้ด (${barcode})`;
            const d = new Date(); d.setDate(d.getDate() + 7);
            if (expiryInput) expiryInput.value = d.toISOString().split('T')[0];
        }
    } catch (err) {
        console.error("Barcode lookup error:", err);
        if (nameInput) nameInput.value = `สินค้าบาร์โค้ด (${barcode})`;
        const d = new Date(); d.setDate(d.getDate() + 7);
        if (expiryInput) expiryInput.value = d.toISOString().split('T')[0];
    }
}

// --- UI Logic ---
function updateProfileLimits() {
    const profile = document.getElementById('userProfile')?.value;
    if (profile === 'normal') limits.protein = 50;
    if (profile === 'light') limits.protein = 80;
    if (profile === 'heavy') limits.protein = 140;
    fetchIntake();
}

function updateNutritionUI(intake) {
    const updateBar = (id, current, max, type) => {
        const percent = Math.min((current / max) * 100, 100);
        const bar = document.getElementById(`${id}-bar`);
        const text = document.getElementById(`${id}-text`);
        if (text) text.innerHTML = `<b>${current.toFixed(1)}</b>/${max}${id === 'sodium' ? 'mg' : 'g'}`;
        if (bar) {
            bar.style.width = `${percent}%`;
            if (type === 'limit') {
                bar.className = `progress-bar ${percent >= 100 ? 'bg-danger' : percent >= 80 ? 'bg-warning text-dark' : 'bg-success'}`;
            } else {
                if (id === 'protein') bar.className = `progress-bar ${percent >= 100 ? 'bg-success' : 'bg-info'}`;
            }
        }
    };

    updateBar('sugar', intake.sugar || 0, limits.sugar, 'limit');
    updateBar('sodium', intake.sodium || 0, limits.sodium, 'limit');
    updateBar('protein', intake.protein || 0, limits.protein, 'goal');
    updateBar('fat', intake.fat || 0, limits.fat, 'goal');
}

function renderTable(items) {
    const tbody = document.getElementById('inventory-body');
    if (!tbody) return;
    tbody.innerHTML = '';
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    items.forEach(item => {
        const expDate = new Date(item.expiry);
        expDate.setHours(0, 0, 0, 0);
        const diffDays = Math.ceil((expDate - today) / (1000 * 60 * 60 * 24));

        let badge = '', rowClass = '';
        if (diffDays < 0) { badge = '<span class="badge bg-dark">หมดอายุ</span>'; rowClass = 'table-secondary text-muted'; }
        else if (diffDays === 0) { badge = '<span class="badge bg-danger">วันนี้!</span>'; rowClass = 'table-danger'; }
        else if (diffDays <= 2) { badge = `<span class="badge bg-warning text-dark">เหลือ ${diffDays} วัน</span>`; rowClass = 'table-warning'; }
        else { badge = `<span class="badge bg-success">เหลือ ${diffDays} วัน</span>`; }

        const isFrozen = item.name.includes('(แช่แข็ง)');

        tbody.innerHTML += `
            <tr class="${rowClass}">
                <td class="ps-3">${badge}</td>
                <td>${item.category}</td>
                <td class="fw-bold">${item.name} <br><small class="text-muted fw-normal">${item.price} ฿</small></td>
                <td>${item.expiry}</td>
                <td>
                    <span class="nutrition-badge bg-light text-dark border">🔥 ${item.cal}</span>
                    <span class="nutrition-badge bg-light text-danger border">🍬 ${item.sugar}</span>
                    <span class="nutrition-badge bg-light text-info border">💪 ${item.protein}</span>
                </td>
                <td class="text-center">
                    <div class="btn-group shadow-sm">
                        <button class="btn btn-sm btn-success" onclick="consumeFood(${item.id})" title="กิน">🍽️</button>
                        ${!isFrozen && item.category === '🥩 เนื้อสัตว์' ? `<button class="btn btn-sm btn-info text-white" onclick="freezeFood(${item.id})" title="แช่แข็ง">❄️</button>` : ''}
                        <button class="btn btn-sm btn-danger" onclick="discardFood(${item.id})" title="ทิ้ง">🗑️</button>
                    </div>
                </td>
            </tr>
        `;
    });
}

function renderChart(stats) {
    const ctx = document.getElementById('barChart');
    if (!ctx) return;
    if (wasteChart) wasteChart.destroy();
    wasteChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: Object.keys(stats),
            datasets: [{ label: 'สูญเงินไปแล้ว (บาท)', data: Object.values(stats), backgroundColor: '#ff6b6b', borderRadius: 4 }]
        },
        options: { responsive: true, maintainAspectRatio: false }
    });
}

// --- Action Functions ---
async function consumeFood(id) {
    await fetch(`${API_BASE}/consume/${id}`, { method: 'POST' });
    refreshAll();
}

async function discardFood(id) {
    await fetch(`${API_BASE}/discard/${id}`, { method: 'POST' });
    refreshAll();
}

async function freezeFood(id) {
    await fetch(`${API_BASE}/freeze/${id}`, { method: 'POST' });
    refreshAll();
}

async function resetNutrition() {
    await fetch(`${API_BASE}/reset-intake`, { method: 'POST' });
    refreshAll();
}

function generateRecipe() {
    const ingredients = currentInventory
        .filter(i => i.category !== '🧁 ของหวาน/ขนม' && i.category !== '🥫 ของแห้ง')
        .slice(0, 2);
    
    const box = document.getElementById('recipe-box');
    if (!box) return;
    if (ingredients.length >= 2) {
        box.innerHTML = `<strong>👨‍🍳 ไอเดียเมนูเคลียร์ตู้เย็น:</strong> ลองทำ <b>"ผัด${ingredients[1].name}ใส่${ingredients[0].name.replace(' (แช่แข็ง)','')}"</b> ดูไหม? รีบใช้ของใกล้เสียให้เป็นประโยชน์!`;
        box.classList.remove('d-none');
    } else {
        box.innerHTML = `<strong>✅ ตู้เย็นปลอดภัย:</strong> ตอนนี้ไม่มีของสดที่เสี่ยงหมดอายุพร้อมกันครับ!`;
        box.classList.remove('d-none');
    }
}

function mockScanBarcode() {
    const getDaysFromNow = (days) => {
        const d = new Date(); d.setDate(d.getDate() + days);
        return d.toISOString().split('T')[0];
    };
    const mocks = [
        {n: 'นมเวย์โปรตีน', c: '🥛 นม/เนย', p: 49, cal: 170, su: 0, so: 100, pr: 28, f: 2},
        {n: 'มันฝรั่งทอดกรอบ', c: '🧁 ของหวาน/ขนม', p: 30, cal: 150, su: 1, so: 350, pr: 2, f: 10}
    ];
    const r = mocks[Math.floor(Math.random() * mocks.length)];
    document.getElementById('f_name').value = r.n;
    document.getElementById('f_category').value = r.c;
    document.getElementById('f_price').value = r.p;
    document.getElementById('f_expiry').value = getDaysFromNow(10);
    document.getElementById('f_cal').value = r.cal; 
    document.getElementById('f_sugar').value = r.su;
    document.getElementById('f_sodium').value = r.so; 
    document.getElementById('f_protein').value = r.pr;
    document.getElementById('f_fat').value = r.f;
}

function refreshAll() {
    fetchInventory();
    fetchIntake();
    fetchWasteStats();
}

// --- Event Listeners & Submit Handler ---
document.addEventListener('DOMContentLoaded', () => {
    const addFoodForm = document.getElementById('addFoodForm');
    if (addFoodForm) {
        addFoodForm.addEventListener('submit', async function (e) {
            e.preventDefault();
            
            const expiryVal = document.getElementById('f_expiry').value;
            const nameVal = document.getElementById('f_name').value;

            if (!nameVal || nameVal.trim() === '') {
                alert("กรุณากรอกชื่อรายการ");
                return;
            }

            if (!expiryVal) {
                alert("กรุณาระบุวันหมดอายุ");
                return;
            }

            const payload = {
                name: nameVal,
                category: document.getElementById('f_category').value,
                price: parseFloat(document.getElementById('f_price').value) || 0,
                expiry: expiryVal,
                cal: parseFloat(document.getElementById('f_cal').value) || 0,
                sugar: parseFloat(document.getElementById('f_sugar').value) || 0,
                sodium: parseFloat(document.getElementById('f_sodium').value) || 0,
                protein: parseFloat(document.getElementById('f_protein').value) || 0,
                fat: parseFloat(document.getElementById('f_fat').value) || 0
            };

            try {
                const res = await fetch(`${API_BASE}/add`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });

                if (!res.ok) {
                    const errText = await res.text();
                    alert(`❌ เซิร์ฟเวอร์ตอบกลับข้อผิดพลาด (${res.status}): ${errText}`);
                    return;
                }

                alert("✅ บันทึกวัตถุดิบลงฐานข้อมูลสำเร็จ!");
                this.reset();
                await stopBarcodeScanner();
                
                const modalEl = document.getElementById('addFoodModal');
                if (modalEl && window.bootstrap) {
                    const modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
                    modal.hide();
                }
                
                refreshAll();
            } catch (err) {
                console.error("Error adding food:", err);
                alert("❌ เกิดข้อผิดพลาดในการเชื่อมต่อเซิร์ฟเวอร์: " + err.message);
            }
        });
    }

    refreshAll();
});