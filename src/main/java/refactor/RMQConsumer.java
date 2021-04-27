package refactor;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeoutException;

public class RMQConsumer {

    static Map<String, Stock> stocks = new HashMap<>();
    static Queue<StockMeta> queue = new ArrayBlockingQueue<StockMeta>(100);


    public static void main(String[] args) throws IOException, TimeoutException {

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

    private static void processQueue(String message)  {
        StockMeta data = null;
        try {
            data = new ObjectMapper().readValue(message, StockMeta.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return;
        }

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
        stock.updateChart(val, data.prevClose, data.percentChange);
    }

}

@JsonIgnoreProperties
class StockMeta {
    String stockName;
    String instrument;
    String token;
    double value;
    double prevClose;
    double percentChange;

    public StockMeta() {
    }

    public String getStockName(){
        return stockName;
    }

    public String getInstrument(){return instrument; }
    public double getValue(){return value;}
    public double getPrevClose() { return prevClose;}
    public double getPercentChange(){ return  percentChange;}

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