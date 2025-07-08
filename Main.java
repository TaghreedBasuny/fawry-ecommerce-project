import java.util.*;
import java.time.LocalDate;

public class Main {
    public static void main(String[] args) {
        int testCase = 3; // 1: Success, 2: Insufficient Balance, 3: Expired Product, 4: Empty Cart

        Product cheese = new ExpirableProduct("Cheese", 100, 5, LocalDate.of(2025, 12, 31));
        Product biscuits = (testCase == 3)
                ? new ExpirableProduct("Biscuits", 150, 3, LocalDate.of(2023, 12, 31))
                : new ExpirableProduct("Biscuits", 150, 3, LocalDate.of(2025, 12, 31));
        Product tv = new ShippableProduct("TV", 3000, 2, 10.0);
        Product scratchCard = new NonExpirableProduct("Mobile Scratch Card", 50, 10);

        Customer customer;
        switch (testCase) {
            case 1 -> customer = new Customer("Ahmed", 4000);
            case 2 -> customer = new Customer("Ahmed", 100);
            default -> customer = new Customer("Ahmed", 4000);
        }

        Cart cart = new Cart();

        try {
            if (testCase != 4) {
                cart.add(cheese, 2);
                if (testCase != 3) cart.add(biscuits, 1);
                cart.add(tv, 1);
                cart.add(scratchCard, 1);
            }

            Checkout.checkout(customer, cart);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    interface Shippable {
        String getName();
        double getWeight();
    }

    static abstract class Product {
        protected String name;
        protected double price;
        protected int quantity;
        protected boolean requiresShipping;
        protected double weight;
        protected boolean hasExpiry;
        protected LocalDate expiryDate;

        public Product(String name, double price, int quantity) {
            this.name = name;
            this.price = price;
            this.quantity = quantity;
        }

        public abstract boolean isExpired();

        public boolean isInStock(int requestedQuantity) {
            return quantity >= requestedQuantity;
        }

        public void deductQuantity(int amount) {
            quantity -= amount;
        }

        public String getName() { return name; }
        public double getPrice() { return price; }
        public int getQuantity() { return quantity; }
        public boolean needsShipping() { return requiresShipping; }
        public double getWeight() { return weight; }
    }

    static class ExpirableProduct extends Product {
        private LocalDate expiryDate;

        public ExpirableProduct(String name, double price, int quantity, LocalDate expiryDate) {
            super(name, price, quantity);
            this.expiryDate = expiryDate;
            this.hasExpiry = true;
            this.requiresShipping = false;
        }

        @Override
        public boolean isExpired() {
            return LocalDate.now().isAfter(expiryDate);
        }
    }

    static class NonExpirableProduct extends Product {
        public NonExpirableProduct(String name, double price, int quantity) {
            super(name, price, quantity);
            this.hasExpiry = false;
            this.requiresShipping = false;
        }

        @Override
        public boolean isExpired() {
            return false;
        }
    }

    static class ShippableProduct extends NonExpirableProduct implements Shippable {
        private double weight;

        public ShippableProduct(String name, double price, int quantity, double weight) {
            super(name, price, quantity);
            this.weight = weight;
            this.requiresShipping = true;
        }

        @Override
        public double getWeight() {
            return weight;
        }
    }

    static class Customer {
        private String name;
        private double balance;

        public Customer(String name, double balance) {
            this.name = name;
            this.balance = balance;
        }

        public boolean canAfford(double amount) {
            return balance >= amount;
        }

        public void pay(double amount) {
            balance -= amount;
        }

        public double getBalance() {
            return balance;
        }
    }

    static class Cart {
        private Map<Product, Integer> items = new HashMap<>();

        public void add(Product product, int quantity) throws Exception {
            if (!product.isInStock(quantity)) {
                throw new Exception("Not enough stock for " + product.getName());
            }
            if (product.isExpired()) {
                throw new Exception(product.getName() + " is expired.");
            }
            items.put(product, quantity);
        }

        public List<Shippable> getShippableItems() {
            List<Shippable> shippables = new ArrayList<>();
            for (Product p : items.keySet()) {
                if (p instanceof Shippable) {
                    int quantity = items.get(p);
                    for (int i = 0; i < quantity; i++) {
                        shippables.add((Shippable) p);
                    }
                }
            }
            return shippables;
        }

        public double getTotalPrice() {
            double total = 0;
            for (Map.Entry<Product, Integer> entry : items.entrySet()) {
                total += entry.getKey().getPrice() * entry.getValue();
            }
            return total;
        }

        public boolean isEmpty() {
            return items.isEmpty();
        }

        public Map<Product, Integer> getItems() {
            return items;
        }

        public void clear() {
            items.clear();
        }
    }

    static class ShippingService {
        public static double calculateShippingFee(List<Shippable> products) {
            System.out.println("** Shipment notice **");
            Map<String, Integer> productCounts = new HashMap<>();
            Map<String, Double> weights = new HashMap<>();

            double totalWeight = 0;
            for (Shippable product : products) {
                productCounts.put(product.getName(), productCounts.getOrDefault(product.getName(), 0) + 1);
                weights.put(product.getName(), product.getWeight());
                totalWeight += product.getWeight();
            }

            for (String name : productCounts.keySet()) {
                int count = productCounts.get(name);
                double weightInGrams = weights.get(name) * 1000 * count;
                System.out.println(count + "x " + name + " " + (int) weightInGrams + "g");
            }

            System.out.printf("Total package weight %.2fkg%n", totalWeight);
            return 30;
        }
    }

    static class Checkout {
        public static void checkout(Customer customer, Cart cart) {
            try {
                if (cart.isEmpty()) {
                    throw new Exception("Cart is empty!");
                }

                double subtotal = cart.getTotalPrice();
                double shippingFee = 0;

                List<Shippable> shippableProducts = cart.getShippableItems();
                if (!shippableProducts.isEmpty()) {
                    shippingFee = ShippingService.calculateShippingFee(shippableProducts);
                }

                double total = subtotal + shippingFee;

                if (!customer.canAfford(total)) {
                    throw new Exception("Insufficient balance!");
                }

                customer.pay(total);

                for (Map.Entry<Product, Integer> entry : cart.getItems().entrySet()) {
                    entry.getKey().deductQuantity(entry.getValue());
                }

                System.out.println("** Checkout receipt **");
                for (Map.Entry<Product, Integer> entry : cart.getItems().entrySet()) {
                    System.out.println(entry.getValue() + "x " + entry.getKey().getName() + " " + (entry.getKey().getPrice() * entry.getValue()));
                }
                System.out.println("----------------------");
                System.out.println("Subtotal " + subtotal);
                System.out.println("Shipping " + shippingFee);
                System.out.println("Amount " + total);
                System.out.printf("Remaining Balance: %.2f%n", customer.getBalance());

                cart.clear();

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
}
