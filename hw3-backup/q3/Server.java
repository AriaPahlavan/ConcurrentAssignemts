import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.*;

public class Server {
    final static String UDP = "U", TCP = "T";
    static Map<String, Integer> inventory = new HashMap<>();
    static List<User> users = new ArrayList<>();
    static Map<User.Order, User> allOrders = new HashMap<>();
    static int orderNum = 1;
    final static String DEFAULT_MODE = UDP;
    static String curMode = DEFAULT_MODE;

    public static void main(String[] args) throws IOException {
        DatagramPacket reqPacket, respPacket;
        int tcpPort, udpPort;
        int length = 1000;
        byte[] buffer = new byte[length];


        if (args.length != 3) {
            System.out.println("ERROR: Provide 3 arguments");
            System.out.println("\t(1) <tcpPort>: the port number for TCP connection");
            System.out.println("\t(2) <udpPort>: the port number for UDP connection");
            System.out.println("\t(3) <file>: the file of inventory");

            System.exit(-1);
        }
        tcpPort = Integer.parseInt(args[0]);
        udpPort = Integer.parseInt(args[1]);
        String fileName = args[2];
        DatagramSocket uSocket = new DatagramSocket(udpPort);

        File f = new File(fileName);
        Scanner s = null;


        // parse the inventory file
        try {
            s = new Scanner(f);
            while (s.hasNext())
                inventory.put(s.next(), s.nextInt());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        // TODO: handle request from clients
        while (true) {
            String req = "";
            switch (curMode) {
                case UDP:
                    reqPacket = new DatagramPacket(buffer, length);
                    uSocket.receive(reqPacket);
                    req = new String(reqPacket.getData(), 0, reqPacket.getLength());
                    String resp = processReq(req, reqPacket);
                    DatagramPacket responsePacket =
                            new DatagramPacket(
                                    resp.getBytes(),
                                    resp.length(),
                                    reqPacket.getAddress(),
                                    reqPacket.getPort()
                            );
                    uSocket.send(responsePacket);
                    break;
                case TCP:

                    break;

            }
        }


    }

    private static String processReq(String req, DatagramPacket reqPacket) throws IOException {
        String[] tokens = req.split(" ");
        String resp = "";
        String name;
        switch (tokens[0]) {
            case "setmode":
                curMode = tokens[1];
                System.out.println("Server: change mode to " + curMode);
                break;
            case "purchase":
                name = tokens[1];
                String product = tokens[2];
                int qty = new Integer(tokens[3]);
                resp = tryPurchase(name, product, qty);
                System.out.println(resp);
                break;
            case "cancel":
                int curOrderID = new Integer(tokens[1]);
                User.Order curOrder = new User.Order(curOrderID);
                User orderOwner = allOrders.get(curOrder);
                if (orderOwner != null) {
                    curOrder = orderOwner.getOrder(curOrderID);
                    int orderQty = curOrder.qty;
                    String orderProduct = curOrder.product;
                    inventory.put(orderProduct, inventory.get(orderProduct) + orderQty);
                    allOrders.remove(curOrder);
                    orderOwner.removeOrder(curOrder);
                    resp = curOrderID + " is canceled";

                } else {
                    resp = curOrderID + " not found, no such order";
                }
                System.out.println(resp);
                break;
            case "search":
                name = tokens[1];
                int userIndex = users.indexOf(new User(name));
                if (userIndex != -1) {
                    User user = users.get(userIndex);
                    StringBuilder sb = new StringBuilder();

                    if (user.orders == null || user.orders.size() == 0) {
                        resp = "No order found for " + name;

                    } else {
                        for (User.Order o : user.orders)
                            sb.append(o.orderID).append(", ").append(o.product).append(", ").append(o.qty).append("\n");

                        resp = sb.toString();
                    }

                } else {
                    resp = "No user found with name: " + name;
                }
                break;
            case "list":
                break;
        }

        return resp;
    }

    private static String tryPurchase(String name, String product, int qty) {
        String result = "";

        Integer qtyAvail = inventory.get(product);
        if (qtyAvail == null) {
            result = "Not Available - We do not sell this product";

        } else if (qtyAvail < qty) {
            result = "Not Available - Not enough items";

        } else {
            //TODO lock this whole block
            int id = orderNum;
            orderNum++;
            User user = getUser(name);
            User.Order newOrder = user.createOrder(product, id, qty);
            allOrders.put(newOrder, user);
            inventory.put(product, inventory.get(product) - qty);
            users.add(user);
            result = "Order has been placed, " + id + " " + name + " " + product + " " + qty;
        }

        return result;
    }

    private static User getUser(String name) {
        User result = new User(name);

        if (users.contains(result)) {
            result = users.get(users.indexOf(result));
        }

        return result;
    }


    static class User {
        String name;
        List<Order> orders = new ArrayList<>();

        User(String name) {
            this.name = name;
        }


        Order createOrder(String product, int id, int qty) {
            Order newOrder = new Order(product, id, qty);
            orders.add(newOrder);
            return newOrder;
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

        public Order getOrder(int curOrderID) {
            User.Order curOrder = new User.Order(curOrderID);

            for (Order order : orders) {
                if (order.equals(curOrder))
                    return order;
            }
            return null;
        }

        public void removeOrder(Order curOrder) {
            orders.remove(curOrder);
        }

        static class Order {
            String product;
            int orderID;
            int qty;

            public Order(int orderID) {
                this.orderID = orderID;
            }

            public Order(String product, int orderID, int qty) {
                this.product = product;
                this.orderID = orderID;
                this.qty = qty;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Order)) return false;

                Order order = (Order) o;

                return orderID == order.orderID;
            }

            @Override
            public int hashCode() {
                return orderID;
            }
        }
    }
}
