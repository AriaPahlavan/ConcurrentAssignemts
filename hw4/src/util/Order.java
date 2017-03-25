package util;//EIDs: sm47767, ap44342

public class Order {
    String name;
    int orderID;
    String product;
    int quantity;

    public Order(String name, int id, String product, int q){
        this.name = name;
        this.orderID = id;
        this.product = product;
        this.quantity = q;
    }

    public String getName() {
        return name;
    }

    public int getOrderID() {
        return orderID;
    }

    public String getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }
}
