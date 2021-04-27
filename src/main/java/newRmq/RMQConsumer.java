package newRmq;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.knowm.xchart.QuickChart;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.internal.chartpart.Chart;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class RMQConsumer {

    static Map<String, Stock> stocks = new HashMap<>();
    static Queue<StockMeta> queue = new ArrayBlockingQueue<StockMeta>(100);


    public static void main(String[] args) throws IOException, TimeoutException {

//        createAndShowGUI();
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        String queueName = "stocks_algo";
        channel.queueDeclare(queueName, false, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [x--] Received '" + message + "'");
            processQueue(message);

        };

        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
        });


    }
    private static void processQueue(String message) throws JsonProcessingException {
        StockMeta data = null;
        System.out.println("processing msg : " + message);
        try {
            data = new ObjectMapper().readValue(message, StockMeta.class);
            processStock(data);
        } catch (JsonProcessingException e) {

            AccountDetails accountDetails = new ObjectMapper().readValue(message, AccountDetails.class);
            System.out.println("processing account details : " + accountDetails.toString());
            processAccountDetails(accountDetails);
            e.printStackTrace();
            return;
        }
    }

    private static void processAccountDetails(AccountDetails accountDetails) {
        System.out.println(accountDetails.toString());
    }

    private static void processStock(StockMeta data)  {
        // poll the message from queue
        // read the value & stock
        String stockName = data.stockName;
        String instrument = data.instrument;
        double val = data.value;

        Stock stock;
        if (!stocks.containsKey(stockName)) {
            // initialize a new XY chart,

            stock = new Stock(stockName, instrument);
            stock.initChart(val);
            stocks.put(stockName, stock);
        }

        stock = stocks.get(stockName);
        System.out.println("updating chart..");
        stock.updateChart(val, data.prevClose, data.percentChange);
    }


    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("SimpleTableDemo");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        //Create and set up the content pane.
        SimpleTableDemo newContentPane = new SimpleTableDemo();
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    public static class SimpleTableDemo extends JPanel {
        private boolean DEBUG = true;

        public SimpleTableDemo() {
            super(new GridLayout(1, 0));

            String[] columnNames = {"Contract Type (P/C)",
                    "Name",
                    "Strike Price",
                    "Expiry",
                    "Vegetarian"};

            Object[][] data = {
                    {"Kathy", "Smith",
                            "Snowboarding", new Integer(5), new Boolean(false)},
                    {"John", "Doe",
                            "Rowing", new Integer(3), new Boolean(true)},
                    {"Sue", "Black",
                            "Knitting", new Integer(2), new Boolean(false)},
                    {"Jane", "White",
                            "Speed reading", new Integer(20), new Boolean(true)},
                    {"Joe", "Brown",
                            "Pool", new Integer(10), new Boolean(false)}
            };

            final JTable table = new JTable(data, columnNames);
            table.setPreferredScrollableViewportSize(new Dimension(500, 70));
            table.setFillsViewportHeight(true);

            if (DEBUG) {
                table.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        printDebugData(table);
                    }
                });
            }

            //Create the scroll pane and add the table to it.
            JScrollPane scrollPane = new JScrollPane(table);

            //Add the scroll pane to this panel.
            add(scrollPane);
        }
    }
    private static void printDebugData(JTable table) {
        int numRows = table.getRowCount();
        int numCols = table.getColumnCount();
        javax.swing.table.TableModel model = table.getModel();

        System.out.println("Value of data: ");
        for (int i=0; i < numRows; i++) {
            System.out.print("    row " + i + ":");
            for (int j=0; j < numCols; j++) {
                System.out.print("  " + model.getValueAt(i, j));
            }
            System.out.println();
        }
        System.out.println("--------------------------");
    }


}

@JsonIgnoreProperties

class Value {
    double Price;
    long Qnty;

    public Value() {
    }

    public Value(double price, long qnty) {
        Price = price;
        Qnty = qnty;
    }

    @Override
    public String toString() {
        return "Value{" +
                "Price=" + Price +
                ", Qnty=" + Qnty +
                '}';
    }
}

@JsonIgnoreProperties

class Position {
    String Key;
    Value Val;
    public Position(){}
    public Position(String key, Value val) {
        Key = key;
        Val = val;
    }

    @Override
    public String toString() {
        return "Position{" +
                "Key='" + Key + '\'' +
                ", Val=" + Val +
                '}';
    }
}

@JsonIgnoreProperties

class AccountDetails {
    double totalMarketVal;
    double Cost;
    double DayChangeDollar;
    Position[] positions;

    public AccountDetails() {
    }

    public AccountDetails(double totalMarketVal, double Cost, double DayChangeDollar, Position[] positions) {
        this.totalMarketVal = totalMarketVal;
        this.Cost = Cost;
        this.DayChangeDollar = DayChangeDollar;
        this.positions = positions;
    }

    @Override
    public String toString() {
        return "AccountDetails{" +
                "totalMarketVal=" + totalMarketVal +
                ", Cost=" + Cost +
                ", DayChangeDollar=" + DayChangeDollar +
                ", positions=" + Arrays.toString(positions) +
                '}';
    }
}

@JsonIgnoreProperties
class StockMeta {
    String stockName;
    String instrument;
//    String token;
    double value;
    double prevClose;
    double percentChange;

    public StockMeta() {
    }

    public StockMeta(String stockName, String instrument, double value) {
        this.stockName = stockName;
        this.instrument = instrument;
        this.value = value;
    }
}

class Stock {
    String seriesName;
    private double closePrice;
    private String title;
    private double lastVal;
    JFrame frame_;

    private int count = 0;
    List xList = new LinkedList();
    List yList = new LinkedList();
    private XChartPanel<Chart> chartPanel;

    public Stock(String title, String instrument) {
        this.seriesName = "s";
        this.title = title;
        frame_ = new JFrame(title);
    }


    void updateChart(double val, double prevClose, double percentChange) {

        XYChart chart = (XYChart) chartPanel.getChart();
        if (val == lastVal) {
            System.out.println("same value... "+ seriesName);
            return;
        }
        count++;

        if (xList.size() > 400) {
            xList.remove(0);
            yList.remove(0);
        }

        xList.add(count);
        yList.add(val);

        chart.updateXYSeries(seriesName, xList, yList, null);
        chart.setTitle(String.format("%s -- %.2f (%.2f)", title, val, percentChange));
        System.out.println("Setting title: " + chart.getTitle());
        chartPanel.revalidate();
        chartPanel.repaint();

        lastVal = val;
        if (!frame_.isVisible())
            frame_.setVisible(true);
    }


    void initChart(double val) {

        try {

            xList.add(0);
            lastVal = val;
            yList.add(val);

            // Create Chart
            final XYChart chart = QuickChart.getChart(title, "Time", "Price", seriesName, xList, yList);


            // Show it
            final SwingWrapper<XYChart> sw = new SwingWrapper<XYChart>(chart);
//            JFrame jFrame = sw.displayChart();


            frame_.setDefaultCloseOperation(1);
            chart.setWidth(500);
            chart.setHeight(280);

            XChartPanel<Chart> chartPanel = new XChartPanel(chart);
            this.chartPanel = chartPanel;

            frame_.add(chartPanel);
//            frame_.setLayout(new GridBagLayout());
            frame_.pack();

            frame_.setVisible(true);
        } catch (HeadlessException e) {
            e.printStackTrace();
        }
    }

}