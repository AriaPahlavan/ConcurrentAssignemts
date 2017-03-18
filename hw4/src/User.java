import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sharmistha on 2/28/2017.
 */
public class User {

    public String name = "";
    public List<Order> orders = new ArrayList<>();

    public User(String name) {
        this.name = name;
    }

    public void createOrder(int id, String product, int q){
        Order order = new Order(name, id, product, q);
        orders.add(order);
    }

    public void removeOrder(int id){
        for(Order order : orders){
            if(order.getOrderID() == id){
                orders.remove(order);
            }
        }
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
