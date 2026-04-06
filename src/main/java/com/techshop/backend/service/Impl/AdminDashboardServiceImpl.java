package com.techshop.backend.service.Impl;

import com.techshop.backend.dto.response.admin.*;
import com.techshop.backend.entity.Order;
import com.techshop.backend.entity.OrderItem;
import com.techshop.backend.entity.Payment;
import com.techshop.backend.entity.Product;
import com.techshop.backend.enums.PaymentMethod;
import com.techshop.backend.enums.PaymentStatus;
import com.techshop.backend.repository.OrderRepository;
import com.techshop.backend.repository.PaymentRepository;
import com.techshop.backend.repository.ProductRepository;
import com.techshop.backend.repository.UserRepository;
import com.techshop.backend.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    public AdminStatsResponse getStats() {
        // Total Revenue: sum of completed payments + COD orders marked as paid
        double totalRevenue = paymentRepository.findAll().stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0.0)
                .sum();

        List<Order> paidCodOrders = getPaidCodOrders();
        double codRevenue = paidCodOrders.stream()
                .mapToDouble(order -> order.getTotalPrice() != null ? order.getTotalPrice() : 0.0)
                .sum();
        totalRevenue += codRevenue;

        // Total Orders
        long totalOrders = orderRepository.count();

        // Total Users
        long totalUsers = userRepository.count();

        // Total Products
        long totalProducts = productRepository.count();

        // For simplicity, hardcode changes as in mock data
        AdminStatsResponse.StatItem revenue = new AdminStatsResponse.StatItem(totalRevenue, 12.5, "vs last month");
        AdminStatsResponse.StatItem orders = new AdminStatsResponse.StatItem(totalOrders, 8.2, "vs last month");
        AdminStatsResponse.StatItem users = new AdminStatsResponse.StatItem(totalUsers, 3.7, "vs last month");
        AdminStatsResponse.StatItem products = new AdminStatsResponse.StatItem(totalProducts, -1.2, "vs last month");

        return new AdminStatsResponse(revenue, orders, users, products);
    }

    @Override
    public RevenueDataResponse getRevenueData() {
        int targetYear = LocalDateTime.now().getYear();

        List<Order> paidCodOrders = getPaidCodOrders();

        Map<Integer, Double> revenueByMonth = new HashMap<>();
        Map<Integer, Integer> ordersByMonth = new HashMap<>();

        // Initialize months 1..12
        for (int m = 1; m <= 12; m++) {
            revenueByMonth.put(m, 0.0);
            ordersByMonth.put(m, 0);
        }

        List<Payment> payments = paymentRepository.findAll();
        payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().getYear() == targetYear)
                .collect(Collectors.groupingBy(p -> p.getCreatedAt().getMonthValue()))
                .forEach((month, monthPayments) -> {
                    double sum = monthPayments.stream().mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0.0).sum();
                    int count = monthPayments.size();
                    revenueByMonth.put(month, sum);
                    ordersByMonth.put(month, count);
                });

        paidCodOrders.stream()
                .filter(order -> order.getCreatedAt() != null && order.getCreatedAt().getYear() == targetYear)
                .forEach(order -> {
                    int month = order.getCreatedAt().getMonthValue();
                    double amount = order.getTotalPrice() != null ? order.getTotalPrice() : 0.0;
                    revenueByMonth.merge(month, amount, Double::sum);
                    ordersByMonth.put(month, ordersByMonth.getOrDefault(month, 0) + 1);
                });

        List<RevenueDataResponse.MonthlyData> data = revenueByMonth.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    String monthName = Month.of(entry.getKey()).name().substring(0, 1) + Month.of(entry.getKey()).name().substring(1).toLowerCase();
                    return new RevenueDataResponse.MonthlyData(monthName, entry.getValue(), ordersByMonth.get(entry.getKey()));
                })
                .collect(Collectors.toList());

        return new RevenueDataResponse(data);
    }

    @Override
    public TopProductResponse getTopProducts() {
        // Get all order items, group by product, sum quantity and revenue
        Map<Product, Integer> salesMap = new HashMap<>();
        Map<Product, Double> revenueMap = new HashMap<>();

        List<Order> orders = orderRepository.findAll();
        for (Order order : orders) {
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                salesMap.put(product, salesMap.getOrDefault(product, 0) + item.getQuantity());
                revenueMap.put(product, revenueMap.getOrDefault(product, 0.0) + (item.getPrice() * item.getQuantity()));
            }
        }

        List<TopProductResponse.ProductData> products = salesMap.entrySet().stream()
                .sorted(Map.Entry.<Product, Integer>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    Product p = entry.getKey();
                    return new TopProductResponse.ProductData(
                            p.getId(),
                            p.getName(),
                            entry.getValue(),
                            revenueMap.get(p),
                            p.getStock()
                    );
                })
                .collect(Collectors.toList());

        return new TopProductResponse(products);
    }

    @Override
    public AdminOrderSummaryResponse getRecentOrders() {
        // Get recent 10 orders
        List<Order> recentOrders = orderRepository.findAll().stream()
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .limit(10)
                .collect(Collectors.toList());

        List<AdminOrderSummaryResponse.OrderSummary> summaries = recentOrders.stream()
                .map(order -> {
                    String customer = order.getShippingAddress().getFirstName() + " " + order.getShippingAddress().getLastName();
                    String email = order.getUser().getEmail();
                    String product = order.getItems().stream()
                            .map(item -> item.getProduct().getName())
                            .collect(Collectors.joining(", "));
                    double amount = order.getTotalPrice();
                    String status = order.getStatus().name();
                    String date = order.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    int items = order.getItems().size();
                    return new AdminOrderSummaryResponse.OrderSummary(
                            "ORD-" + order.getId(),
                            customer,
                            email,
                            product,
                            amount,
                            status,
                            date,
                            items
                    );
                })
                .collect(Collectors.toList());

        return new AdminOrderSummaryResponse(summaries);
    }

    private List<Order> getPaidCodOrders() {
        return orderRepository.findAll().stream()
                .filter(order -> order.getPaymentMethod() == PaymentMethod.COD)
                .filter(order -> Boolean.TRUE.equals(order.getIsPaid()))
                .toList();
    }
}
