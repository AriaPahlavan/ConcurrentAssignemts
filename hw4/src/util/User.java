package util;//EIDs: sm47767, ap44342

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class User {

    private String name = "";
    private List<Order> orders = new ArrayList<>();

    public User(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public Order createOrder(int id, String product, int q){
        Order order = new Order(name, id, product, q);
        orders.add(order);

        return order;
    }

    public boolean removeOrder(int id){
        orders = orders.parallelStream()
                .filter(order -> order.getOrderID() != id)
                .collect(Collectors.toList());

        return orders.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;

        User user = (User) o;

        return name != null ? name.equals(user.name) : user.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
