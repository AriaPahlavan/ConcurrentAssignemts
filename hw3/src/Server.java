import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server {

    static Map<String, Integer> Inventory = new HashMap<>();
    static List<User> users = new ArrayList<>();
    static List<Order> allOrders = new ArrayList<>();
    static int orderID = 1;
    static final String DEFAULT_MODE = "U";
    static String curMode = DEFAULT_MODE;
    public static void main (String[] args) throws IOException {

        int tcpPort;
        int udpPort;

        DatagramPacket datapacket, returnpacket;


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

        // parse the inventory file
        File f = new File(fileName);
        Scanner s = null;

        try{
            s = new Scanner(f);
            while(s.hasNext()){
                Inventory.put(s.next(),s.nextInt());
            }
        } catch(FileNotFoundException e){
            e.printStackTrace();
        }



        // TODO: handle request from clients
        DatagramSocket uSocket = new DatagramSocket(udpPort);
        ServerSocket tSocket = new ServerSocket(tcpPort);
        byte[] buf = new byte[1000];
        while(true){
            String request = "";
            String response = "";
            switch(curMode){
                case "U":
                    System.out.println("processing UDP");
                    datapacket = new DatagramPacket(buf, buf.length);
                    uSocket.receive(datapacket);
                    request = new String(datapacket.getData(), 0, datapacket.getLength());
                    response = processReq(request);
                    System.out.println(response);
                    DatagramPacket responsePacket = new DatagramPacket(
                            response.getBytes(),
                            response.length(),
                            datapacket.getAddress(),
                            datapacket.getPort());

                    uSocket.send(responsePacket);
                    break;
                case "T":
                    System.out.println("processing TCP");
                    try{
                        while(true){
                            Socket clientSocket = tSocket.accept();
                            try{
                                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                                BufferedReader in = new BufferedReader(new InputStreamReader((clientSocket.getInputStream())));
                                request = in.readLine();
                                response = processReq(request);
                                String[] responseSplit = response.split("\n");
                                for(int i=0; i<responseSplit.length; i++) {
                                    out.println(responseSplit[i]);
                                }
                            }
                            finally{
                                clientSocket.close();
                            }
                        }
                    }
                    finally{
                        tSocket.close();
                        break;
                    }

            }



        }

    }


    static String processReq(String req) throws IOException {
        String[] tokens = req.split(" ");
        String result = "result";
        switch(tokens[0]){
            case "setmode":
                curMode = tokens[1];
                result = "changed server mode to " +curMode;
                break;
            case "purchase":
                String name = tokens[1];
                String product = tokens[2];
                Integer quantity = new Integer(tokens[3]);

                result = tryPurchase(name, product, quantity);
                break;
            case "cancel":
                Integer id = new Integer(tokens[1]);
                result = cancelOrder(id);
                break;

            case "search":
                String UserName = tokens[1];
                result = findUserOrders(UserName);
                break;

            case "list":
                result = listAllProducts();
                break;
        }
        return result;
    }

    private static String listAllProducts() {
        String result = "";
        Iterator it = Inventory.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            result = result + pair.getKey() + " " + pair.getValue() + "\n";
        }

        return result;
    }

    private static String findUserOrders(String userName) {
        String result = "";
        User user = new User(userName);
        User foundUser;
        int userIndex;
        if(users.contains(user)){
            userIndex = users.indexOf(user);
            foundUser = users.get(userIndex);
            for(int i=0; i<foundUser.orders.size(); i++){
                Order order = foundUser.orders.get(i);
                Integer orderID = order.getOrderID();
                String productName = order.getProduct();
                Integer quantity = order.getQuantity();
                result = result+orderID+" "+productName+" "+quantity+"\n";
            }
        }
        else{
            result = "No order found for "+userName;
        }
        return result;

    }

    private static String cancelOrder(Integer id) {
        String result = "";
        for (int i = 0; i < allOrders.size() ; i++) {
            if(allOrders.get(i).getOrderID() == id){
                //TODO: lock?
                String name = allOrders.get(i).getName();
                String product = allOrders.get(i).getProduct();
                int quantity = allOrders.get(i).getQuantity();
                //TODO: Lock
                User user = getUser(name);
                user.removeOrder(id);
                Inventory.put(product, Inventory.get(product)+quantity);
                allOrders.remove(i);
                result = "Order "+id+" is canceled";
                return result;
            }
        }

        result = id+" not found, no such order";
        return result;
    }

    private static String tryPurchase(String name, String product, Integer quantity) {
        String result = "";
        Integer qtyAvail = Inventory.get(product);
        if(qtyAvail == null){
            result = "Not Available - We do not sell this product";
        }
        else if(qtyAvail < quantity){
            result = "Not Available - Not enough items";
        }
        else{
            //TODO: lock
            int id = orderID;
            orderID++;
            User user = getUser(name);
            user.createOrder(id, product, quantity);
            Order newOrder = new Order(name, id, product, quantity);
            users.add(user);
            allOrders.add(newOrder);
            Inventory.put(product, Inventory.get(product)-quantity);

            result = "Your order has been placed, " +id+" "+name+" "+product+" "+quantity;
        }

        return result;
    }

    private static User getUser(String name) {
        User user = new User(name);
        int userIndex;
        if(users.contains(user)){
            userIndex = users.indexOf(user);
            user = users.get(userIndex);
        }
        return user;
    }


    static class User{
        String name = "";
        List<Order> orders = new ArrayList<>();

        User(String name) {
            this.name = name;
        }

        void createOrder(int id, String product, int q){
            Order order = new Order(name, id, product, q);
            orders.add(order);
        }

        void removeOrder(int id){
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

    static class Order{
        String name;
        int orderID;
        String product;
        int quantity;

        Order(String name, int id, String product, int q){
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

}
