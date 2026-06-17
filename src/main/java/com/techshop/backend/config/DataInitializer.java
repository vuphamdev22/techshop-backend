package com.techshop.backend.config;

import com.techshop.backend.entity.Category;
import com.techshop.backend.entity.Product;
import com.techshop.backend.entity.ProductImage;
import com.techshop.backend.entity.ProductSpec;
import com.techshop.backend.entity.ChatFaq;
import com.techshop.backend.entity.Brand;
import com.techshop.backend.repository.CategoryRepository;
import com.techshop.backend.repository.ProductRepository;
import com.techshop.backend.repository.ChatFaqRepository;
import com.techshop.backend.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ChatFaqRepository chatFaqRepository;
    private final BrandRepository brandRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Only seed if database is empty
        if (categoryRepository.count() == 0) {
            seedCategories();
        }
        
        // Seed chat FAQs
        if (chatFaqRepository.count() == 0) {
            seedFaqs();
        }
        
        // Ensure all categories (seeded or existing) have valid icons populated
        updateExistingCategoryIcons();

        // Seed pristine tech products (including budget ones) if missing
        seedProducts();

        // Ensure specifications are populated
        seedSpecsForExistingProducts();
    }

    private void seedCategories() {
        String[] categoryNames = {"Laptops", "Smartphones", "Audio", "Wearables", "Monitors", "Gaming", "Accessories"};
        String[] categoryIcons = {"Laptop", "Smartphone", "Headphones", "Watch", "Monitor", "Gamepad2", "Keyboard"};
        
        for (int i = 0; i < categoryNames.length; i++) {
            Category category = new Category();
            category.setName(categoryNames[i]);
            category.setDescription("Shop " + categoryNames[i]);
            category.setIcon(categoryIcons[i]);
            categoryRepository.save(category);
        }
    }

    private void updateExistingCategoryIcons() {
        categoryRepository.findAll().forEach(category -> {
            if (category.getIcon() == null || category.getIcon().isEmpty()) {
                String name = category.getName();
                String icon = getIconForCategory(name);
                if (icon != null) {
                    category.setIcon(icon);
                    categoryRepository.save(category);
                }
            }
        });
    }

    private String getIconForCategory(String name) {
        if (name == null) return null;
        switch (name.toLowerCase()) {
            case "laptops": return "Laptop";
            case "smartphones":
            case "phones": return "Smartphone";
            case "audio": return "Headphones";
            case "wearables": return "Watch";
            case "monitors": return "Monitor";
            case "gaming": return "Gamepad2";
            case "accessories": return "Keyboard";
            case "desktop pcs": return "Laptop";
            case "components": return "Cpu";
            case "peripherals": return "Keyboard";
            default: return "Laptop";
        }
    }

    private void seedProducts() {
        Category laptopsCategory = categoryRepository.findByName("Laptops").orElse(null);
        Category phonesCategory = categoryRepository.findByName("Smartphones").orElse(null);
        Category audioCategory = categoryRepository.findByName("Audio").orElse(null);
        Category wearablesCategory = categoryRepository.findByName("Wearables").orElse(null);
        Category monitorsCategory = categoryRepository.findByName("Monitors").orElse(null);
        Category gamingCategory = categoryRepository.findByName("Gaming").orElse(null);
        Category accessoriesCategory = categoryRepository.findByName("Accessories").orElse(null);

        // 💻 Laptops (12)
        createProductIfNotExists("MacBook Pro 16\" M3 Max", 3499.99, 3799.99, 15, "Apple M3 Max chip, 48GB Unified Memory, 1TB SSD, 16-inch Liquid Retina XDR Display.", 
                "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=600&auto=format&fit=crop&q=60", "sale", laptopsCategory);
        createProductIfNotExists("Dell XPS 15 9530", 1899.99, 2099.99, 12, "Intel Core i9-13900H, 32GB DDR5, 1TB SSD, NVIDIA GeForce RTX 4060, 15.6\" OLED Touchscreen.", 
                "https://images.unsplash.com/photo-1593642632823-8f785ba67e45?w=600&auto=format&fit=crop&q=60", "hot", laptopsCategory);
        createProductIfNotExists("ASUS ROG Zephyrus G14", 1599.99, 1799.99, 8, "AMD Ryzen 9 7940HS, 16GB DDR5, 1TB PCIe 4.0 SSD, NVIDIA RTX 4070, 14\" 165Hz QHD Display.", 
                "https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=600&auto=format&fit=crop&q=60", "new", laptopsCategory);
        createProductIfNotExists("ASUS Vivobook 15", 649.99, 699.99, 25, "Intel Core i5-1335U, 16GB RAM, 512GB SSD, Intel Iris Xe Graphics, 15.6\" FHD Display. Máy tính xách tay học tập, văn phòng giá tốt, hỗ trợ trả góp 0%.", 
                "https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=600&auto=format&fit=crop&q=60", "sale", laptopsCategory);
        createProductIfNotExists("HP Pavilion 15", 749.99, 799.99, 20, "AMD Ryzen 7 7730U, 16GB RAM, 512GB SSD, AMD Radeon Graphics, 15.6\" FHD IPS. Hoàn hảo cho sinh viên học tập và văn phòng.", 
                "https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=600&auto=format&fit=crop&q=60", "new", laptopsCategory);
        createProductIfNotExists("Lenovo ThinkPad X1 Carbon Gen 11", 1999.99, 2199.99, 10, "Intel Core i7-1355U, 32GB LPDDR5, 1TB SSD, 14\" WUXGA IPS, Intel Iris Xe, Windows 11 Pro. Premium business laptop.",
                "https://images.unsplash.com/photo-1593642632823-8f785ba67e45?w=600&auto=format&fit=crop&q=60", null, laptopsCategory);
        createProductIfNotExists("Acer Swift Go 14", 799.99, 849.99, 15, "Intel Core i5-13500H, 16GB LPDDR5, 512GB SSD, 14\" 2.8K 90Hz OLED, Intel Iris Xe, Windows 11 Home. Lightweight and sleek.",
                "https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=600&auto=format&fit=crop&q=60", "hot", laptopsCategory);
        createProductIfNotExists("ASUS Zenbook 14 OLED", 999.99, 1099.99, 12, "Intel Core i7-1360P, 16GB RAM, 1TB SSD, 14\" 2.8K 90Hz OLED Touchscreen, Intel Iris Xe, Windows 11 Home.",
                "https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=600&auto=format&fit=crop&q=60", null, laptopsCategory);
        createProductIfNotExists("MSI Katana 15", 1199.99, 1299.99, 8, "Intel Core i7-13620H, 16GB DDR5, 1TB SSD, NVIDIA GeForce RTX 4060 (8GB), 15.6\" 144Hz FHD Display, Windows 11 Home. Powerful gaming.",
                "https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=600&auto=format&fit=crop&q=60", "hot", laptopsCategory);
        createProductIfNotExists("Gigabyte AORUS 15", 1499.99, 1649.99, 6, "Intel Core i7-13700H, 16GB DDR5, 1TB SSD, NVIDIA GeForce RTX 4070 (8GB), 15.6\" 165Hz QHD Display, Windows 11 Home.",
                "https://images.unsplash.com/photo-1593642632823-8f785ba67e45?w=600&auto=format&fit=crop&q=60", "new", laptopsCategory);
        createProductIfNotExists("MacBook Air 13-inch M3", 1099.99, 1199.99, 20, "Apple M3 chip with 8-core CPU and 10-core GPU, 8GB Unified Memory, 256GB SSD, 13.6\" Liquid Retina Display. Silent, fanless design.",
                "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=600&auto=format&fit=crop&q=60", "sale", laptopsCategory);
        createProductIfNotExists("Dell Inspiron 15", 599.99, 649.99, 25, "Intel Core i5-1235U, 8GB DDR4, 512GB SSD, Intel UHD Graphics, 15.6\" FHD 120Hz Display, Windows 11 Home. Budget-friendly daily driver.",
                "https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=600&auto=format&fit=crop&q=60", null, laptopsCategory);

        // 📱 Smartphones (11)
        createProductIfNotExists("iPhone 15 Pro Max", 1199.99, 1299.99, 25, "Titanium design, A17 Pro chip, 5x Telephoto camera, 256GB Storage, Super Retina XDR.", 
                "https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?w=600&auto=format&fit=crop&q=60", "hot", phonesCategory);
        createProductIfNotExists("Samsung Galaxy S24 Ultra", 1299.99, 1399.99, 20, "Snapdragon 8 Gen 3, 200MP Camera, S Pen included, 512GB Storage, 6.8\" QHD+ Dynamic AMOLED.", 
                "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=600&auto=format&fit=crop&q=60", "sale", phonesCategory);
        createProductIfNotExists("Google Pixel 8 Pro", 999.99, 1099.99, 18, "Google Tensor G3, Pro triple camera system, Magic Eraser, 128GB Storage, Bay Blue.", 
                "https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=600&auto=format&fit=crop&q=60", "new", phonesCategory);
        createProductIfNotExists("OnePlus 12", 799.99, 899.99, 15, "Snapdragon 8 Gen 3, 16GB RAM, 100W SUPERVOOC charging, 4th Gen Hasselblad Camera.", 
                "https://images.unsplash.com/photo-1565849906461-0ee1ebe3500e?w=600&auto=format&fit=crop&q=60", null, phonesCategory);
        createProductIfNotExists("iPhone 15", 799.99, 849.99, 25, "A16 Bionic chip, Dynamic Island, 48MP main camera, 128GB Storage, USB-C connector.",
                "https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?w=600&auto=format&fit=crop&q=60", "sale", phonesCategory);
        createProductIfNotExists("Samsung Galaxy S24", 799.99, 849.99, 22, "Exynos 2400 / Snapdragon 8 Gen 3, 8GB RAM, 256GB Storage, 6.2\" FHD+ Dynamic AMOLED 2X, Galaxy AI features.",
                "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=600&auto=format&fit=crop&q=60", null, phonesCategory);
        createProductIfNotExists("Xiaomi 14 Ultra", 1099.99, 1199.99, 10, "Snapdragon 8 Gen 3, 16GB RAM, 512GB Storage, Leica 1-inch main sensor quad-camera, 6.73\" 120Hz AMOLED.",
                "https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=600&auto=format&fit=crop&q=60", "hot", phonesCategory);
        createProductIfNotExists("Redmi Note 13 Pro", 299.99, 329.99, 35, "MediaTek Helio G99-Ultra, 8GB RAM, 256GB Storage, 200MP Camera with OIS, 6.67\" 120Hz AMOLED. Excellent value.",
                "https://images.unsplash.com/photo-1565849906461-0ee1ebe3500e?w=600&auto=format&fit=crop&q=60", "sale", phonesCategory);
        createProductIfNotExists("POCO F6 Pro", 499.99, 549.99, 18, "Snapdragon 8 Gen 2, 12GB RAM, 512GB Storage, 120W HyperCharge, 6.67\" WQHD+ 120Hz Flow AMOLED.",
                "https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=600&auto=format&fit=crop&q=60", "new", phonesCategory);
        createProductIfNotExists("Sony Xperia 1 VI", 1399.99, 1499.99, 8, "Snapdragon 8 Gen 3, 12GB RAM, 256GB Storage, 85-170mm optical telephoto zoom camera, 6.5\" 120Hz OLED.",
                "https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?w=600&auto=format&fit=crop&q=60", null, phonesCategory);
        createProductIfNotExists("Samsung Galaxy A55", 449.99, 489.99, 30, "Exynos 1480, 8GB RAM, 128GB Storage, 50MP main camera, IP67 dust and water resistance, premium glass design.",
                "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=600&auto=format&fit=crop&q=60", null, phonesCategory);

        // 🎧 Audio (9)
        createProductIfNotExists("Sony WH-1000XM5", 399.99, 449.99, 30, "Industry-leading Noise Cancellation, 30-hour Battery, High-Resolution Wireless Audio, Black.", 
                "https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=600&auto=format&fit=crop&q=60", "hot", audioCategory);
        createProductIfNotExists("Apple AirPods Pro (2nd Gen)", 249.99, 279.99, 40, "Active Noise Cancellation, Adaptive Audio, USB-C Charging Case, Personalized Spatial Audio.", 
                "https://images.unsplash.com/photo-1600294037681-c80b4cb5b434?w=600&auto=format&fit=crop&q=60", "sale", audioCategory);
        createProductIfNotExists("Bose QuietComfort Ultra", 429.99, 479.99, 22, "World-class noise cancellation, Immersive Audio, CustomTune technology, Luxe White.", 
                "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600&auto=format&fit=crop&q=60", "new", audioCategory);
        createProductIfNotExists("Sony WF-1000XM5", 299.99, 329.99, 40, "The Best Truly Wireless Noise Cancelling Earbuds, high-performance dual processors, 8-hour battery, water-resistant.",
                "https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=600&auto=format&fit=crop&q=60", null, audioCategory);
        createProductIfNotExists("Sennheiser Momentum True Wireless 4", 299.99, 329.99, 20, "Audiophile-grade sound quality, Adaptive ANC, Bluetooth 5.4, Auracast support, 30 hours playtime with case.",
                "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600&auto=format&fit=crop&q=60", "new", audioCategory);
        createProductIfNotExists("JBL Flip 6", 129.99, 139.99, 50, "IP67 Waterproof and Dustproof Portable Bluetooth Speaker, 2-way speaker system, 12 hours playtime.",
                "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600&auto=format&fit=crop&q=60", null, audioCategory);
        createProductIfNotExists("Marshall Emberton II", 169.99, 179.99, 25, "Signature Marshall sound, True Stereophonic multi-directional sound, IP67 waterproof, 30+ hours playtime.",
                "https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=600&auto=format&fit=crop&q=60", "hot", audioCategory);
        createProductIfNotExists("Apple AirPods Max", 549.99, 599.99, 15, "Over-ear headphones, Apple-designed dynamic driver, Active Noise Cancellation, Transparency mode, Spatial Audio.",
                "https://images.unsplash.com/photo-1600294037681-c80b4cb5b434?w=600&auto=format&fit=crop&q=60", "sale", audioCategory);
        createProductIfNotExists("Bose QuietComfort Headphones", 349.99, 379.99, 30, "Legendary noise cancellation, adjustable EQ, high-fidelity audio, up to 24 hours battery life.",
                "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600&auto=format&fit=crop&q=60", null, audioCategory);

        // ⌚ Wearables (8)
        createProductIfNotExists("Apple Watch Ultra 2", 799.99, 849.99, 15, "Titanium case, dual-frequency GPS, 36-hour battery life, Ocean Band, rugged outdoor sports watch.", 
                "https://images.unsplash.com/photo-1434494878577-86c23bcb06b9?w=600&auto=format&fit=crop&q=60", "hot", wearablesCategory);
        createProductIfNotExists("Samsung Galaxy Watch 6 Classic", 399.99, 449.99, 25, "Rotating bezel, body composition analysis, advanced sleep coaching, LTE connectivity.", 
                "https://images.unsplash.com/photo-1579586337278-3befd40fd17a?w=600&auto=format&fit=crop&q=60", "new", wearablesCategory);
        createProductIfNotExists("Garmin Fenix 7X Pro", 899.99, 949.99, 10, "Multisport GPS watch, Solar charging, built-in LED flashlight, preloaded topo maps.", 
                "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=60", "sale", wearablesCategory);
        createProductIfNotExists("Apple Watch Series 9", 399.99, 429.99, 30, "S9 SiP chip, double tap gesture, brighter Always-On Retina display, temperature sensing, ECG app.",
                "https://images.unsplash.com/photo-1434494878577-86c23bcb06b9?w=600&auto=format&fit=crop&q=60", "sale", wearablesCategory);
        createProductIfNotExists("Samsung Galaxy Watch 6", 299.99, 329.99, 25, "Exynos W930, 1.4\" Super AMOLED, body composition, sleep coaching, LTE connectivity available.",
                "https://images.unsplash.com/photo-1579586337278-3befd40fd17a?w=600&auto=format&fit=crop&q=60", null, wearablesCategory);
        createProductIfNotExists("Garmin Venu 3", 449.99, 499.99, 15, "AMOLED touchscreen GPS smartwatch, advanced health and fitness tracking, voice calls and texting from wrist.",
                "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=60", "new", wearablesCategory);
        createProductIfNotExists("Fitbit Charge 6", 159.99, 179.99, 40, "Premium health and fitness tracker, built-in GPS, heart rate on gym equipment, Google Maps & Wallet.",
                "https://images.unsplash.com/photo-1434494878577-86c23bcb06b9?w=600&auto=format&fit=crop&q=60", null, wearablesCategory);
        createProductIfNotExists("Amazfit GTR 4", 199.99, 219.99, 30, "Dual-band circular-polarized GPS antenna, 150+ sports modes, 14-day battery life, classic round design.",
                "https://images.unsplash.com/photo-1579586337278-3befd40fd17a?w=600&auto=format&fit=crop&q=60", null, wearablesCategory);

        // 🖥️ Monitors (6)
        createProductIfNotExists("Samsung Odyssey G9 OLED", 1799.99, 1999.99, 5, "49\" Curved Dual QHD Gaming Monitor, 240Hz refresh rate, 0.03ms response time, Neo Quantum Processor.", 
                "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=600&auto=format&fit=crop&q=60", "hot", monitorsCategory);
        createProductIfNotExists("LG UltraFine 27\" 5K", 1299.99, 1399.99, 8, "27-inch 5K IPS Display, Thunderbolt 3, 500 nits brightness, P3 wide color gamut, ideal for creators.", 
                "https://images.unsplash.com/photo-1547119957-637f8679db1e?w=600&auto=format&fit=crop&q=60", "sale", monitorsCategory);
        createProductIfNotExists("Dell UltraSharp 27 4K", 549.99, 599.99, 15, "27-inch 4K USB-C Hub Monitor (U2723QE), IPS Black technology, 2000:1 contrast ratio, 98% DCI-P3.",
                "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=600&auto=format&fit=crop&q=60", "sale", monitorsCategory);
        createProductIfNotExists("ASUS ROG Swift PG27AQDM", 899.99, 999.99, 10, "27-inch QHD (2560 x 1440) OLED Gaming Monitor, 240Hz refresh rate, 0.03ms response time, custom heatsink.",
                "https://images.unsplash.com/photo-1547119957-637f8679db1e?w=600&auto=format&fit=crop&q=60", "hot", monitorsCategory);
        createProductIfNotExists("Gigabyte M27Q", 299.99, 329.99, 25, "27-inch 170Hz QHD (2560 x 1440) KVM Gaming Monitor, 0.5ms response time, Super Speed IPS display.",
                "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=600&auto=format&fit=crop&q=60", null, monitorsCategory);
        createProductIfNotExists("LG DualUp 28MQ780", 599.99, 649.99, 12, "28-inch SDQHD (2560 x 2880) Nano IPS Display with Ergo Stand, unique 16:18 aspect ratio.",
                "https://images.unsplash.com/photo-1547119957-637f8679db1e?w=600&auto=format&fit=crop&q=60", null, monitorsCategory);

        // 🎮 Gaming (7)
        createProductIfNotExists("PlayStation 5 Slim", 499.99, 549.99, 30, "Ultra-high speed SSD, Ray tracing, 4K-TV gaming, HDR technology, includes Astro's Playroom.", 
                "https://images.unsplash.com/photo-1606813907291-d86efa9b94db?w=600&auto=format&fit=crop&q=60", "hot", gamingCategory);
        createProductIfNotExists("Nintendo Switch OLED", 349.99, 379.99, 45, "7-inch OLED screen, wide adjustable stand, wired LAN port, 64GB internal storage, Neon Blue/Red.", 
                "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop&q=60", "sale", gamingCategory);
        createProductIfNotExists("Steam Deck OLED 512GB", 549.99, 599.99, 15, "7.4\" HDR OLED Display, 90Hz refresh rate, 6nm AMD APU, premium carrying case.", 
                "https://images.unsplash.com/photo-1605901309584-818e25960a8f?w=600&auto=format&fit=crop&q=60", "new", gamingCategory);
        createProductIfNotExists("Xbox Series X", 499.99, 549.99, 20, "12 teraflops processing power, true 4K gaming, up to 120fps, custom 1TB SSD, Xbox Velocity Architecture.",
                "https://images.unsplash.com/photo-1606813907291-d86efa9b94db?w=600&auto=format&fit=crop&q=60", "hot", gamingCategory);
        createProductIfNotExists("ASUS ROG Ally X", 799.99, 849.99, 12, "Windows 11 gaming handheld, AMD Ryzen Z1 Extreme, 24GB LPDDR5X, 1TB SSD, 80Wh battery, 7\" 120Hz FHD screen.",
                "https://images.unsplash.com/photo-1605901309584-818e25960a8f?w=600&auto=format&fit=crop&q=60", "new", gamingCategory);
        createProductIfNotExists("Sony PlayStation VR2", 549.99, 599.99, 10, "Virtual reality headset for PS5, 4K HDR display, 110-degree field of view, eye tracking, headset feedback.",
                "https://images.unsplash.com/photo-1606813907291-d86efa9b94db?w=600&auto=format&fit=crop&q=60", null, gamingCategory);
        createProductIfNotExists("Nintendo Switch Lite", 199.99, 219.99, 30, "Dedicated handheld play console, compatible with all physical and digital Nintendo Switch games.",
                "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop&q=60", null, gamingCategory);

        // ⌨️ Accessories (7)
        createProductIfNotExists("Logitech MX Master 3S", 99.99, 109.99, 50, "Ergonomic wireless mouse, 8K DPI tracking, ultra-quiet clicks, MagSpeed electromagnetic scrolling.", 
                "https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?w=600&auto=format&fit=crop&q=60", "hot", accessoriesCategory);
        createProductIfNotExists("Keychron Q1 Pro Wireless", 199.99, 219.99, 25, "QMK/VIA custom mechanical keyboard, full aluminum body, hot-swappable switches, double-gasket design.", 
                "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=600&auto=format&fit=crop&q=60", "new", accessoriesCategory);
        createProductIfNotExists("Logitech MX Keys S", 119.99, 129.99, 40, "Advanced wireless illuminated keyboard, low-profile keys, smart backlighting, multi-device connection.",
                "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=600&auto=format&fit=crop&q=60", null, accessoriesCategory);
        createProductIfNotExists("Razer DeathAdder V3 Pro", 149.99, 159.99, 35, "Ultra-lightweight wireless gaming mouse (63g), Focus Pro 30K optical sensor, Optical Mouse Switches Gen-3.",
                "https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?w=600&auto=format&fit=crop&q=60", "hot", accessoriesCategory);
        createProductIfNotExists("SteelSeries Apex Pro TKL", 189.99, 199.99, 20, "World's fastest keyboard, OmniPoint 2.0 adjustable hypermagnetic switches, OLED smart display, premium aluminum.",
                "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=600&auto=format&fit=crop&q=60", "new", accessoriesCategory);
        createProductIfNotExists("Anker 737 Power Bank", 149.99, 159.99, 50, "PowerCore 24K, 140W ultra-powerful two-way charging, smart digital display, 24,000mAh capacity.",
                "https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?w=600&auto=format&fit=crop&q=60", null, accessoriesCategory);
        createProductIfNotExists("Elgato Stream Deck MK.2", 149.99, 159.99, 25, "Studio controller with 15 customizable LCD keys, drag-and-drop actions, interchangeable faceplates.",
                "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=600&auto=format&fit=crop&q=60", null, accessoriesCategory);
    }

    private void createProductIfNotExists(String name, Double price, Double originalPrice, Integer stock, 
                                          String description, String imageUrl, String badge, Category category) {
        boolean exists = productRepository.findAll().stream()
                .anyMatch(p -> p.getName().equalsIgnoreCase(name));
        if (exists) {
            return;
        }
        createProduct(name, price, originalPrice, stock, description, imageUrl, badge, category);
    }

    private Brand getOrCreateBrand(String name) {
        return brandRepository.findByName(name).orElseGet(() -> {
            Brand brand = new Brand();
            brand.setName(name);
            brand.setDescription("Chính hãng " + name);
            return brandRepository.save(brand);
        });
    }

    private String detectBrandName(String productName) {
        String lower = productName.toLowerCase();
        if (lower.contains("macbook") || lower.contains("iphone") || lower.contains("ipad") || lower.contains("airpods") || lower.contains("apple watch") || lower.contains("apple")) {
            return "Apple";
        }
        if (lower.contains("dell") || lower.contains("ultrasharp")) {
            return "Dell";
        }
        if (lower.contains("asus") || lower.contains("rog") || lower.contains("zenbook") || lower.contains("vivobook")) {
            return "ASUS";
        }
        if (lower.contains("hp") || lower.contains("pavilion")) {
            return "HP";
        }
        if (lower.contains("samsung") || lower.contains("galaxy") || lower.contains("odyssey")) {
            return "Samsung";
        }
        if (lower.contains("google") || lower.contains("pixel")) {
            return "Google";
        }
        if (lower.contains("oneplus")) {
            return "OnePlus";
        }
        if (lower.contains("sony") || lower.contains("xperia") || lower.contains("playstation") || lower.contains("ps5")) {
            return "Sony";
        }
        if (lower.contains("bose")) {
            return "Bose";
        }
        if (lower.contains("garmin")) {
            return "Garmin";
        }
        if (lower.contains("nintendo")) {
            return "Nintendo";
        }
        if (lower.contains("logitech")) {
            return "Logitech";
        }
        if (lower.contains("keychron")) {
            return "Keychron";
        }
        if (lower.contains("lenovo") || lower.contains("thinkpad")) {
            return "Lenovo";
        }
        if (lower.contains("acer") || lower.contains("swift")) {
            return "Acer";
        }
        if (lower.contains("msi") || lower.contains("katana")) {
            return "MSI";
        }
        if (lower.contains("gigabyte") || lower.contains("aorus") || lower.contains("m27q")) {
            return "Gigabyte";
        }
        if (lower.contains("xiaomi") || lower.contains("redmi") || lower.contains("poco")) {
            return "Xiaomi";
        }
        if (lower.contains("sennheiser")) {
            return "Sennheiser";
        }
        if (lower.contains("jbl")) {
            return "JBL";
        }
        if (lower.contains("marshall") || lower.contains("emberton")) {
            return "Marshall";
        }
        if (lower.contains("fitbit")) {
            return "Fitbit";
        }
        if (lower.contains("amazfit")) {
            return "Amazfit";
        }
        if (lower.contains("lg") || lower.contains("dualup")) {
            return "LG";
        }
        if (lower.contains("steam deck")) {
            return "Valve";
        }
        if (lower.contains("xbox")) {
            return "Microsoft";
        }
        if (lower.contains("razer")) {
            return "Razer";
        }
        if (lower.contains("steelseries")) {
            return "SteelSeries";
        }
        if (lower.contains("anker")) {
            return "Anker";
        }
        if (lower.contains("elgato")) {
            return "Corsair";
        }
        return "Chính hãng";
    }

    private void createProduct(String name, Double price, Double originalPrice, Integer stock, 
                               String description, String imageUrl, String badge, Category category) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(price);
        product.setOriginalPrice(originalPrice);
        product.setStock(stock);
        product.setDescription(description);
        product.setCategory(category);
        product.setRating(4.5);
        product.setReviews((int) (Math.random() * 500) + 50);
        product.setBadge(badge);
        product.setLastRestocked(LocalDate.now());

        String brandName = detectBrandName(name);
        if (brandName != null) {
            product.setBrand(getOrCreateBrand(brandName));
        }
        
        // Create product image
        if (imageUrl != null) {
            ProductImage productImage = new ProductImage();
            productImage.setImageUrl(imageUrl);
            productImage.setProduct(product);
            
            List<ProductImage> images = new ArrayList<>();
            images.add(productImage);
            product.setImages(images);
        }
        
        productRepository.save(product);
    }

    private void seedFaqs() {
        createFaq("Phí giao hàng là bao nhiêu? Có miễn phí không?",
                "🚚 **Chính sách giao hàng của VoltTech:**\n\n- **Miễn phí giao hàng (Free Shipping)** cho mọi đơn hàng từ **$99 trở lên**.\n- Với đơn hàng dưới $99, phí giao hàng toàn quốc cố định là **$9.99**.\n- **Thời gian giao nhận**: Từ 1-2 ngày làm việc (các tỉnh thành lớn thường nhận hàng sau 24h).\n- Ngay khi gửi hàng, hệ thống sẽ tự động cập nhật mã vận đơn (Tracking number) trong phần 'My Orders' để bạn tiện theo dõi hành trình!",
                "ship, phí, vận chuyển, giao hàng, bao lâu, delivery, shipping, cod, vận đơn",
                "shipping");

        createFaq("Chính sách đổi trả hàng như thế nào?",
                "🛡️ **Chính sách đổi trả & bảo hành của VoltTech:**\n\n- **Đổi mới 1-đổi-1 trong vòng 30 ngày đầu tiên** nếu sản phẩm phát sinh lỗi kỹ thuật từ nhà sản xuất (miễn phí phí vận chuyển thu hồi).\n- **Điều kiện**: Sản phẩm còn nguyên hộp, đầy đủ phụ kiện và không trầy xước.\n- **Chính sách bảo hành**: Mọi thiết bị công nghệ chính hãng mua tại VoltTech đều được bảo hành từ **12 đến 24 tháng** theo tiêu chuẩn của hãng.",
                "đổi, trả, đổi trả, hoàn tiền, lỗi, hỏng, bảo hành, hoàn trả, bảo dưỡng",
                "policies");

        createFaq("Có những hình thức thanh toán nào?",
                "💳 **Các hình thức thanh toán linh hoạt tại VoltTech:**\n\n1. **Thanh toán khi nhận hàng (COD)**: Xem hàng trước khi trả tiền bằng tiền mặt.\n2. **Chuyển khoản Ngân hàng (MB Bank)**: Quét mã QR thanh toán nhanh tự động hoàn toàn.\n3. **Trả góp 0% lãi suất**: Hỗ trợ trả góp qua thẻ tín dụng liên kết hơn 25 ngân hàng hoặc qua công ty tài chính với thủ tục duyệt nhanh chóng trong 15 phút!",
                "thanh toán, trả góp, momo, vnpay, chuyển khoản, ngân hàng, cod, tiền mặt",
                "payment");

        createFaq("Hiện tại có voucher giảm giá nào không?",
                "🎁 **Ưu đãi ngập tràn tại VoltTech:**\n\n- Nhập mã **VOLT10** tại màn hình giỏ hàng để được **giảm ngay 10%** cho đơn hàng đầu tiên của bạn!\n- Ngoài ra, chúng tôi thường xuyên tổ chức Flash Sale vào thứ 6 hàng tuần với mức giảm giá lên đến 30% cho các sản phẩm hot.\n- Để áp dụng: Hãy copy mã **VOLT10**, đi đến Giỏ hàng, nhập vào ô 'Promo code' và nhấn áp dụng.",
                "voucher, giảm giá, khuyến mãi, discount, code, coupon, sale, mã giảm giá",
                "promotions");

        createFaq("Tôi muốn đổi mật khẩu hoặc thông tin cá nhân thì làm thế nào?",
                "🔐 **Quản lý tài khoản VoltTech cá nhân:**\n\n- **Đổi thông tin / Avatar**: Bạn hãy click vào avatar góc trên bên phải ➡️ Chọn **'My Profile'** (hoặc truy cập trực tiếp [Hồ sơ cá nhân](/profile)).\n- **Đổi mật khẩu**: Chọn **'Change Password'** (hoặc truy cập [Đổi mật khẩu](/profile/password)) để cập nhật mật khẩu mạnh mới để bảo mật tài khoản.\n- **Quên mật khẩu**: Liên hệ hotline CSKH 1900 8198 để được hỗ trợ reset nhanh qua Email đã đăng ký.",
                "mật khẩu, tài khoản, quên mật khẩu, đổi mật khẩu, email, số điện thoại, đăng ký, đăng nhập",
                "account");
    }

    private void createFaq(String question, String answer, String keyword, String category) {
        ChatFaq faq = new ChatFaq();
        faq.setQuestion(question);
        faq.setAnswer(answer);
        faq.setKeyword(keyword);
        faq.setCategory(category);
        chatFaqRepository.save(faq);
    }

    private void seedSpecsForExistingProducts() {
        List<Product> products = productRepository.findAll();
        for (Product product : products) {
            if (product.getSpecs() == null || product.getSpecs().isEmpty()) {
                List<ProductSpec> specs = new ArrayList<>();
                String name = product.getName().toLowerCase();
                
                if (product.getBrand() != null) {
                    addSpec(specs, product, "Brand", product.getBrand().getName());
                }

                // Dynamically extract and assign specs based on keywords in name and description
                if (name.contains("macbook pro 16")) {
                    addSpec(specs, product, "CPU", "Apple M3 Max (16-core)");
                    addSpec(specs, product, "RAM", "48GB Unified Memory");
                    addSpec(specs, product, "Storage", "1TB SSD");
                    addSpec(specs, product, "Screen", "16.2\" Liquid Retina XDR (120Hz)");
                    addSpec(specs, product, "Battery", "Up to 22 hours");
                    addSpec(specs, product, "OS", "macOS Sonoma");
                    addSpec(specs, product, "GPU", "40-core GPU");
                } else if (name.contains("dell xps 15")) {
                    addSpec(specs, product, "CPU", "Intel Core i9-13900H");
                    addSpec(specs, product, "RAM", "32GB DDR5 (Upgradeable)");
                    addSpec(specs, product, "Storage", "1TB NVMe SSD");
                    addSpec(specs, product, "GPU", "NVIDIA GeForce RTX 4060 (8GB GDDR6)");
                    addSpec(specs, product, "Screen", "15.6\" 3.5K OLED Touch");
                    addSpec(specs, product, "Battery", "86Whr (8-10 hours)");
                    addSpec(specs, product, "OS", "Windows 11 Home");
                } else if (name.contains("asus rog zephyrus")) {
                    addSpec(specs, product, "CPU", "AMD Ryzen 9 7940HS");
                    addSpec(specs, product, "RAM", "16GB DDR5");
                    addSpec(specs, product, "Storage", "1TB PCIe 4.0 SSD");
                    addSpec(specs, product, "GPU", "NVIDIA GeForce RTX 4070 (8GB GDDR6)");
                    addSpec(specs, product, "Screen", "14\" QHD+ (165Hz)");
                    addSpec(specs, product, "Battery", "76Whr (Up to 7 hours)");
                    addSpec(specs, product, "OS", "Windows 11 Home");
                } else if (name.contains("asus vivobook 15")) {
                    addSpec(specs, product, "CPU", "Intel Core i5-1335U");
                    addSpec(specs, product, "RAM", "16GB RAM");
                    addSpec(specs, product, "Storage", "512GB SSD");
                    addSpec(specs, product, "GPU", "Intel Iris Xe Graphics");
                    addSpec(specs, product, "Screen", "15.6\" FHD Display");
                    addSpec(specs, product, "Battery", "42Whr (Up to 6 hours)");
                    addSpec(specs, product, "OS", "Windows 11 Home");
                } else if (name.contains("hp pavilion 15")) {
                    addSpec(specs, product, "CPU", "AMD Ryzen 7 7730U");
                    addSpec(specs, product, "RAM", "16GB RAM");
                    addSpec(specs, product, "Storage", "512GB SSD");
                    addSpec(specs, product, "GPU", "AMD Radeon Graphics");
                    addSpec(specs, product, "Screen", "15.6\" FHD IPS");
                    addSpec(specs, product, "Battery", "41Whr (Up to 6 hours)");
                    addSpec(specs, product, "OS", "Windows 11 Home");
                } else if (name.contains("lenovo thinkpad x1")) {
                    addSpec(specs, product, "CPU", "Intel Core i7-1355U");
                    addSpec(specs, product, "RAM", "32GB LPDDR5");
                    addSpec(specs, product, "Storage", "1TB NVMe SSD");
                    addSpec(specs, product, "Screen", "14\" WUXGA IPS");
                    addSpec(specs, product, "OS", "Windows 11 Pro");
                } else if (name.contains("acer swift go")) {
                    addSpec(specs, product, "CPU", "Intel Core i5-13500H");
                    addSpec(specs, product, "RAM", "16GB LPDDR5");
                    addSpec(specs, product, "Storage", "512GB SSD");
                    addSpec(specs, product, "Screen", "14\" 2.8K 90Hz OLED");
                    addSpec(specs, product, "OS", "Windows 11 Home");
                } else if (name.contains("asus zenbook 14")) {
                    addSpec(specs, product, "CPU", "Intel Core i7-1360P");
                    addSpec(specs, product, "RAM", "16GB LPDDR5");
                    addSpec(specs, product, "Storage", "1TB SSD");
                    addSpec(specs, product, "Screen", "14\" 2.8K OLED Touch");
                    addSpec(specs, product, "OS", "Windows 11 Home");
                } else if (name.contains("msi katana")) {
                    addSpec(specs, product, "CPU", "Intel Core i7-13620H");
                    addSpec(specs, product, "RAM", "16GB DDR5");
                    addSpec(specs, product, "Storage", "1TB NVMe SSD");
                    addSpec(specs, product, "GPU", "NVIDIA RTX 4060 (8GB)");
                    addSpec(specs, product, "Screen", "15.6\" 144Hz FHD");
                    addSpec(specs, product, "OS", "Windows 11 Home");
                } else if (name.contains("aorus 15")) {
                    addSpec(specs, product, "CPU", "Intel Core i7-13700H");
                    addSpec(specs, product, "RAM", "16GB DDR5");
                    addSpec(specs, product, "Storage", "1TB SSD");
                    addSpec(specs, product, "GPU", "NVIDIA RTX 4070 (8GB)");
                    addSpec(specs, product, "Screen", "15.6\" 165Hz QHD");
                    addSpec(specs, product, "OS", "Windows 11 Home");
                } else if (name.contains("macbook air 13")) {
                    addSpec(specs, product, "CPU", "Apple M3 (8-core)");
                    addSpec(specs, product, "RAM", "8GB Unified Memory");
                    addSpec(specs, product, "Storage", "256GB SSD");
                    addSpec(specs, product, "Screen", "13.6\" Liquid Retina");
                    addSpec(specs, product, "OS", "macOS Sonoma");
                } else if (name.contains("dell inspiron")) {
                    addSpec(specs, product, "CPU", "Intel Core i5-1235U");
                    addSpec(specs, product, "RAM", "8GB DDR4");
                    addSpec(specs, product, "Storage", "512GB SSD");
                    addSpec(specs, product, "Screen", "15.6\" FHD 120Hz");
                    addSpec(specs, product, "OS", "Windows 11 Home");
                } else if (name.contains("iphone 15 pro max")) {
                    addSpec(specs, product, "CPU", "Apple A17 Pro");
                    addSpec(specs, product, "RAM", "8GB");
                    addSpec(specs, product, "Storage", "256GB");
                    addSpec(specs, product, "Screen", "6.7\" Super Retina XDR (120Hz)");
                    addSpec(specs, product, "Camera", "48MP + 12MP + 12MP (5x Zoom)");
                    addSpec(specs, product, "Charging", "20W Fast Charging");
                    addSpec(specs, product, "Waterproof", "IP68");
                } else if (name.contains("samsung galaxy s24 ultra")) {
                    addSpec(specs, product, "CPU", "Snapdragon 8 Gen 3 for Galaxy");
                    addSpec(specs, product, "RAM", "12GB");
                    addSpec(specs, product, "Storage", "512GB");
                    addSpec(specs, product, "Screen", "6.8\" Dynamic AMOLED 2X QHD+");
                    addSpec(specs, product, "Camera", "200MP + 50MP + 12MP + 10MP");
                    addSpec(specs, product, "Charging", "45W Fast Charging");
                    addSpec(specs, product, "Waterproof", "IP68");
                    addSpec(specs, product, "Stylus", "S Pen Integrated");
                } else if (name.contains("google pixel 8 pro")) {
                    addSpec(specs, product, "CPU", "Google Tensor G3");
                    addSpec(specs, product, "RAM", "12GB");
                    addSpec(specs, product, "Storage", "128GB");
                    addSpec(specs, product, "Screen", "6.7\" Super Actua Display (120Hz)");
                    addSpec(specs, product, "Camera", "50MP + 48MP + 48MP");
                    addSpec(specs, product, "Waterproof", "IP68");
                } else if (name.contains("oneplus 12")) {
                    addSpec(specs, product, "CPU", "Snapdragon 8 Gen 3");
                    addSpec(specs, product, "RAM", "16GB");
                    addSpec(specs, product, "Storage", "512GB");
                    addSpec(specs, product, "Screen", "6.82\" 2K Oriental AMOLED");
                    addSpec(specs, product, "Charging", "100W SUPERVOOC");
                    addSpec(specs, product, "Waterproof", "IP65");
                } else if (name.contains("iphone 15")) {
                    addSpec(specs, product, "CPU", "Apple A16 Bionic");
                    addSpec(specs, product, "RAM", "6GB");
                    addSpec(specs, product, "Storage", "128GB");
                    addSpec(specs, product, "Screen", "6.1\" Super Retina XDR");
                    addSpec(specs, product, "Camera", "48MP + 12MP");
                    addSpec(specs, product, "Waterproof", "IP68");
                } else if (name.contains("samsung galaxy s24")) {
                    addSpec(specs, product, "CPU", "Snapdragon 8 Gen 3");
                    addSpec(specs, product, "RAM", "8GB");
                    addSpec(specs, product, "Storage", "256GB");
                    addSpec(specs, product, "Screen", "6.2\" FHD+ Dynamic AMOLED 2X");
                    addSpec(specs, product, "Waterproof", "IP68");
                } else if (name.contains("xiaomi 14 ultra")) {
                    addSpec(specs, product, "CPU", "Snapdragon 8 Gen 3");
                    addSpec(specs, product, "RAM", "16GB");
                    addSpec(specs, product, "Storage", "512GB");
                    addSpec(specs, product, "Screen", "6.73\" 120Hz AMOLED");
                    addSpec(specs, product, "Camera", "50MP Quad-Leica Camera");
                } else if (name.contains("redmi note 13")) {
                    addSpec(specs, product, "CPU", "MediaTek Helio G99-Ultra");
                    addSpec(specs, product, "RAM", "8GB");
                    addSpec(specs, product, "Storage", "256GB");
                    addSpec(specs, product, "Screen", "6.67\" 120Hz AMOLED");
                    addSpec(specs, product, "Camera", "200MP Main");
                } else if (name.contains("poco f6 pro")) {
                    addSpec(specs, product, "CPU", "Snapdragon 8 Gen 2");
                    addSpec(specs, product, "RAM", "12GB");
                    addSpec(specs, product, "Storage", "512GB");
                    addSpec(specs, product, "Screen", "6.67\" WQHD+ 120Hz AMOLED");
                    addSpec(specs, product, "Charging", "120W HyperCharge");
                } else if (name.contains("xperia 1 vi")) {
                    addSpec(specs, product, "CPU", "Snapdragon 8 Gen 3");
                    addSpec(specs, product, "RAM", "12GB");
                    addSpec(specs, product, "Storage", "256GB");
                    addSpec(specs, product, "Screen", "6.5\" 120Hz OLED");
                    addSpec(specs, product, "Waterproof", "IP68");
                } else if (name.contains("samsung galaxy a55")) {
                    addSpec(specs, product, "CPU", "Exynos 1480");
                    addSpec(specs, product, "RAM", "8GB");
                    addSpec(specs, product, "Storage", "128GB");
                    addSpec(specs, product, "Waterproof", "IP67");
                } else if (name.contains("sony wh-1000xm5")) {
                    addSpec(specs, product, "Type", "Over-ear Headphones");
                    addSpec(specs, product, "Battery", "Up to 30 hours (ANC ON)");
                    addSpec(specs, product, "Noise Cancelling", "Active Noise Cancelling");
                } else if (name.contains("airpods pro")) {
                    addSpec(specs, product, "Type", "In-ear Earbuds");
                    addSpec(specs, product, "Battery", "Up to 6 hours");
                    addSpec(specs, product, "Noise Cancelling", "Active Noise Cancelling");
                } else if (name.contains("bose quietcomfort ultra")) {
                    addSpec(specs, product, "Type", "Over-ear Headphones");
                    addSpec(specs, product, "Battery", "Up to 24 hours");
                } else if (name.contains("wf-1000xm5")) {
                    addSpec(specs, product, "Type", "In-ear Earbuds");
                    addSpec(specs, product, "Noise Cancelling", "Active Noise Cancelling");
                } else if (name.contains("sennheiser momentum")) {
                    addSpec(specs, product, "Type", "In-ear ANC Earbuds");
                    addSpec(specs, product, "Battery", "Up to 30 hours with case");
                } else if (name.contains("flip 6")) {
                    addSpec(specs, product, "Type", "Portable Bluetooth Speaker");
                    addSpec(specs, product, "Waterproof", "IP67");
                } else if (name.contains("marshall emberton")) {
                    addSpec(specs, product, "Type", "Portable Speaker");
                    addSpec(specs, product, "Battery", "30+ hours");
                } else if (name.contains("airpods max")) {
                    addSpec(specs, product, "Type", "Over-ear Premium Headphones");
                    addSpec(specs, product, "Noise Cancelling", "Active Noise Cancelling");
                } else if (name.contains("bose quietcomfort headphones")) {
                    addSpec(specs, product, "Type", "Over-ear Headphones");
                    addSpec(specs, product, "Battery", "Up to 24 hours");
                } else if (name.contains("apple watch ultra 2")) {
                    addSpec(specs, product, "Case", "49mm Titanium");
                    addSpec(specs, product, "Battery", "Up to 36 hours");
                    addSpec(specs, product, "Waterproof", "100m");
                } else if (name.contains("watch 6 classic")) {
                    addSpec(specs, product, "Case", "47mm Stainless Steel");
                    addSpec(specs, product, "Waterproof", "IP68 / 5ATM");
                } else if (name.contains("fenix 7x pro")) {
                    addSpec(specs, product, "Battery", "Up to 28 days");
                    addSpec(specs, product, "Waterproof", "10ATM");
                } else if (name.contains("apple watch series 9")) {
                    addSpec(specs, product, "Case", "41mm/45mm Aluminium");
                    addSpec(specs, product, "OS", "watchOS");
                } else if (name.contains("galaxy watch 6")) {
                    addSpec(specs, product, "Screen", "1.4\" Super AMOLED");
                    addSpec(specs, product, "OS", "Wear OS");
                } else if (name.contains("garmin venu 3")) {
                    addSpec(specs, product, "Screen", "AMOLED Touchscreen");
                } else if (name.contains("fitbit charge 6")) {
                    addSpec(specs, product, "Type", "Fitness Tracker");
                } else if (name.contains("amazfit gtr 4")) {
                    addSpec(specs, product, "Battery", "Up to 14 days");
                } else if (name.contains("odyssey g9")) {
                    addSpec(specs, product, "Screen", "49\" Dual QHD Curved OLED");
                    addSpec(specs, product, "Refresh Rate", "240Hz");
                } else if (name.contains("ultrafine")) {
                    addSpec(specs, product, "Screen", "27\" 5K IPS");
                } else if (name.contains("ultrasharp 27")) {
                    addSpec(specs, product, "Screen", "27\" 4K USB-C Hub Monitor");
                } else if (name.contains("swift pg27aqdm")) {
                    addSpec(specs, product, "Screen", "27\" QHD Gaming OLED");
                    addSpec(specs, product, "Refresh Rate", "240Hz");
                } else if (name.contains("m27q")) {
                    addSpec(specs, product, "Screen", "27\" 170Hz QHD");
                } else if (name.contains("dualup")) {
                    addSpec(specs, product, "Screen", "28\" SDQHD Nano IPS");
                } else if (name.contains("playstation 5")) {
                    addSpec(specs, product, "Storage", "1TB Custom SSD");
                } else if (name.contains("nintendo switch oled")) {
                    addSpec(specs, product, "Screen", "7-inch OLED");
                } else if (name.contains("steam deck")) {
                    addSpec(specs, product, "Screen", "7.4\" HDR OLED (90Hz)");
                } else if (name.contains("xbox series x")) {
                    addSpec(specs, product, "Storage", "1TB Custom SSD");
                    addSpec(specs, product, "Resolution", "True 4K Gaming");
                } else if (name.contains("rog ally x")) {
                    addSpec(specs, product, "RAM", "24GB LPDDR5X");
                    addSpec(specs, product, "Storage", "1TB SSD");
                } else if (name.contains("vr2")) {
                    addSpec(specs, product, "Screen", "4K HDR VR Display");
                } else if (name.contains("switch lite")) {
                    addSpec(specs, product, "Type", "Handheld Only Console");
                } else if (name.contains("mx master 3s")) {
                    addSpec(specs, product, "DPI", "8000 DPI");
                } else if (name.contains("keychron q1")) {
                    addSpec(specs, product, "Type", "Custom Mechanical Keyboard");
                } else if (name.contains("mx keys s")) {
                    addSpec(specs, product, "Type", "Wireless Illuminated Keyboard");
                } else if (name.contains("deathadder v3")) {
                    addSpec(specs, product, "Weight", "63g");
                    addSpec(specs, product, "DPI", "30000 DPI");
                } else if (name.contains("apex pro")) {
                    addSpec(specs, product, "Switches", "OmniPoint 2.0 Adjustable");
                } else if (name.contains("737 power bank")) {
                    addSpec(specs, product, "Capacity", "24,000mAh");
                    addSpec(specs, product, "Output", "140W max");
                } else if (name.contains("stream deck mk.2")) {
                    addSpec(specs, product, "Keys", "15 LCD Keys");
                }

                if (specs.isEmpty()) {
                    addSpec(specs, product, "Warranty", "12 Months");
                }
                
                if (product.getSpecs() == null) {
                    product.setSpecs(new ArrayList<>());
                } else {
                    product.getSpecs().clear();
                }
                product.getSpecs().addAll(specs);
                productRepository.save(product);
            }
        }
    }
    
    private void addSpec(List<ProductSpec> specs, Product product, String key, String value) {
        ProductSpec spec = new ProductSpec();
        spec.setSpecKey(key);
        spec.setSpecValue(value);
        spec.setProduct(product);
        specs.add(spec);
    }
}
