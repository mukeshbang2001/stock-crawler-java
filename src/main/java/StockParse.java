
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.knowm.xchart.QuickChart;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.internal.chartpart.Chart;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Channel;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeoutException;

/**
 * Created by mukesh.bang on 07/12/16.
 */


enum OrderType {
    MIS, NRML;
}
class Stock {
    Connection connection;
    int lotSize;
    String url;
    String seriesName;
    private int time;
    FileWriter fw;
    int x = 0;
    double prevVal, currVal, todayHigh, todayLow = 100000, prevClose;
    Notifier notifier;
    int count = 0;

    boolean first = true;
    private double scaryZone = 0.5; // percentage
    private double closePrice;
    private boolean random = false;
    boolean pause = false;
    private String title;

    public Stock(Notifier notifier, String seriesName, int time, String expiry, int lotSize) throws IOException {
        this(notifier, seriesName, time, null, "", expiry, lotSize);
        System.out.println("todayLow :" + todayLow);
    }

    public Stock(Notifier notifier, String seriesName, int time, String optType, String strikePrice, String expiry, int lotSize) throws IOException {
        this.notifier = notifier;
        fw = new FileWriter(new File(seriesName + ".txt"), true);

        this.seriesName = "s";
        this.title = seriesName + (optType!=null ? "-" + optType + "-" + strikePrice: "");
        this.time = time;
        String productType = "F";
        if(!Objects.isNull(strikePrice) && strikePrice.length()>0){
            productType = "O";
            scaryZone = 5;
        } else {
            productType = "F";
            strikePrice = "";
        }


        if(optType == null) optType = "*";
        url = MessageFormat.format(StockParse.URL, seriesName, productType, optType, strikePrice + "00", expiry);
//        System.out.println("url:" + url);
        connection = Jsoup.connect(url);
    }

    public void start() throws InterruptedException {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    stock();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
        System.out.println(seriesName + " -- Started...");
    }

    public void stock() {

        try {
            List xList = new LinkedList();
            List yList = new LinkedList();

            xList.add(0);
            double val = getBankValue();
            double lastVal = val;
            yList.add(val);

            // Create Chart
            final XYChart chart = QuickChart.getChart(title, "Price", "Time", seriesName, xList, yList);


            // Show it
            final SwingWrapper<XYChart> sw = new SwingWrapper<XYChart>(chart);
//            JFrame jFrame = sw.displayChart();
            final JFrame frame_ = new JFrame(seriesName);

            frame_.setDefaultCloseOperation(3);
            chart.setWidth(500);
            chart.setHeight(280);

            XChartPanel<Chart> chartPanel = new XChartPanel(chart);


            JButton buy, sell;
            buy = new JButton("BUY");
            sell = new JButton("SELL");

            JPanel mainPanel = new JPanel(new BorderLayout());
            JPanel topPnl = new JPanel(new BorderLayout());
            JPanel btnPnl = new JPanel(new FlowLayout(FlowLayout.CENTER));

            JToggleButton orderTypeButton = new JToggleButton("MIS");
            orderTypeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (orderTypeButton.getText().equalsIgnoreCase("MIS"))
                        orderTypeButton.setText("NRML");
                    else
                        orderTypeButton.setText("MIS");
                }
            });
            btnPnl.add(orderTypeButton);
            JToggleButton pauseButton = new JToggleButton("Pause");
            pauseButton.addActionListener(e -> {
                System.out.println("pause : " + pause);
                pause = !pause;
                if (pauseButton.getText().equalsIgnoreCase("Pause"))
                    pauseButton.setText("UnPause");
                else
                    pauseButton.setText("Pause");
            });

            JTextField qnty = new JTextField("2", 3);
            btnPnl.add(qnty);
            btnPnl.add(buy);
            btnPnl.add(sell);
            JTextField percentChange = new JTextField("txt");
            btnPnl.add(percentChange);
            btnPnl.add(pauseButton);

            buy.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String text = qnty.getText();
                    int val = Integer.parseInt(text);

                    System.out.println("BUY button pressed for : " + title + " - with qnty: " + val + ", Order Type: " + orderTypeButton.getText());
                }
            });
            sell.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String text = qnty.getText();
                    int val = Integer.parseInt(text);
                    System.out.println("SELL button pressed for : " + title + " - with qnty: " + val + ", Order Type: " + orderTypeButton.getText());
                }
            });
            btnPnl.setBorder(BorderFactory.createLineBorder(Color.BLACK));

            mainPanel.add(chartPanel, BorderLayout.NORTH);
//            mainPanel.add(btnPnl, BorderLayout.SOUTH);

            frame_.add(chartPanel);
//            frame_.setLayout(new GridBagLayout());
            frame_.pack();

            frame_.setVisible(true);


            while (true) {

                try {
                    long currentTs = System.currentTimeMillis();
                    int minuteOfDay = new DateTime().getMinuteOfDay();
//                    if (minuteOfDay > 930 || minuteOfDay < 530)
//                    if ((currentTs > 1549434540000L && currentTs < 1549436400000L) || (currentTs > 1549423800000L && currentTs < 1549424400000L))
//                        System.out.println("Stopping the notifications..... ");
                    Thread.sleep(time);
                    if (currVal > todayHigh)
                        todayHigh = currVal;

                    if (currVal < todayLow && currVal > 0)
                        todayLow = currVal;

                    System.out.println(todayHigh + " - " + todayLow + " - " + currVal);
                    if (title.contains("cnxban")){

                    }
                    prevVal = currVal;
                    val = getBankValue();
                    currVal = val;

                    double _diff = Math.abs((currVal - prevVal));
                    double perDiff = (_diff / prevVal) * 100;
                    if(seriesName.equalsIgnoreCase("nifty") && _diff > 10){
                        notifier.notify("Alert..., Sudden spikes in : " + seriesName + ", by " + _diff);
                    }
                    if(perDiff > this.scaryZone && !first && count > 2){
                        notifier.notify("Alert..., Sudden spikes in : " + seriesName + ", by " + perDiff);
                    }
                    if(first) closePrice = val;
                    first = false;

                    System.out.println(seriesName + "..." + val);

                    if (val == lastVal)
                        continue;
                    count++;

                    if (xList.size() > 600) {
                        xList.remove(0);
                        yList.remove(0);
                    }
                    xList.add(x++);
                    yList.add(val);

                    chart.updateXYSeries(seriesName, xList, yList, null);
                    chartPanel.revalidate();
                    chartPanel.repaint();
//                    sw.repaintChart();
//                    if (!set) {
//                        sw.getXChartPanel(0).setAutoscrolls(true);
//                        sw.getXChartPanel().getAutoscrolls();
//                        set = true;
//                    }
                    lastVal = val;

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println(seriesName + " -- Done...");

        }


    }

    public double getBankValue() throws IOException {

        DateTime now = DateTime.now();
        boolean dontRun = (now.getMinuteOfDay() < 545 || now.getMinuteOfDay() > 931) && !random;
        if (dontRun || pause) {
//            System.out.println("Returning the same old data");
//            return currVal;
        }
        if(random)
            return new Random().nextDouble();

        Document document = connection.get();
        Elements element = document.body().getElementById("pnlGetQuote").children().get(2).children().get(0).child(0).child(1).getElementsByTag("td");
        Element element1 = element.get(0);
        String val = element1.text();
//        LocalDateTime now = LocalDateTime.now();
//
//        fw.append(now.getHour() + ":" + now.getMinute() + ":" + now.getSecond() + " | " + seriesName + " | " + val.toString() + "\n");
//        fw.flush();

        currVal = Double.parseDouble(tmp(val.substring(0, val.indexOf('.') + 3)));
        return currVal;

    }

    private String tmp(String input) {
        return input.replaceAll(",", "");
    }

}

class Order {
    OrderType orderType;
    int qnty;


    public void placeOrder(String stockName, int lots, OrderType orderType){

    }

}

public class StockParse {

    //    static String URL = "https://secure.icicidirect.com/IDirectTrading/Trading/FNO/GetQuoteData.aspx?FFO_XCHNG_CD=NFO&FFO_PRDCT_TYP=F&FFO_UNDRLYNG={0}&FFO_EXPRY_DT=29-Jun-2017&FFO_EXER_TYP=E&FFO_OPT_TYP=";
    static String URL = "https://secure.icicidirect.com/IDirectTrading/Trading/FNO/GetQuoteData.aspx?FFO_XCHNG_CD=NFO&FFO_PRDCT_TYP={1}&FFO_UNDRLYNG={0}&FFO_EXPRY_DT={4}&FFO_OPT_TYP={2}&FFO_EXER_TYP=E&FFO_STRK_PRC={3}";

    public static boolean pause = false;
    static FileWriter fw;
    private final static String QUEUE_NAME = "stock-india";
    Channel channel;
    public StockParse() throws IOException, TimeoutException {
        fw = new FileWriter(new File("banks.txt"), true);

/*
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        com.rabbitmq.client.Connection connection = factory.newConnection();
        channel = connection.createChannel();
        try  {
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        } catch (IOException e) {
            e.printStackTrace();
        }

*/
    }
    public static enum ReviewStatus {
        PENDING(2),
        COMPLETED(1),
        DECLINED(3),
        SCHEDULED(4);

        int i;
        private ReviewStatus(int i) {
            this.i = i;
        }
    }


    static class Key {
        String a;
        int b;

        public Key(String a, int b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public String toString() {
            return "Key{" +
                    "!a='" + a + '\'' +
                    ", !b=" + b +
                    '}'; 
        }
    }
    public static void main(String[] args) throws IOException, InterruptedException {


        Notifier notifier = new Notifier();

        List<String> stocks = Lists.newArrayList(
//                "divlab"
//                "jinsp",
//                "deenit",
                "adaent",
                "nifty"
//                "cnxban"
//                "tatmot",
//                "SHRTRA"
//                "havind",

//                 "nifty","cnxban",
//                "tatmot"

//                "relind"
//                "AXIBAN",
//                "kotmah",
//                "drredd",
//                "cnxban",
//                "rblban", "bhainf"
//                "cnxban"
//                "minlim"
//                "axiban"
//                "relind", "cnxban"
//                "pvrlim",
        );

        List<String> codes = Lists.newArrayList(  "dlflim", "eicmot","drredd", "bajfi", "iciban", "axiban","nifty","cnxban","kotmah" );
//        new Stock(notifier, "axiban", 1000, "27-Jun-2019", 6500).start();

        for (String stock: stocks){
            new Stock(notifier, stock, 800, "29-apr-2021", Integer.parseInt("100")).start();
        }
        for (String code : codes) {
//            new Stock(notifier, code, 800, "28-Mar-2019").start();
        }
        List<String> codes2 = Lists.newArrayList(  "zeeent", "havind","baauto","divlab", "maruti", "hindal", "batind","aurpha","TVSMOT" );
        for (String code : codes2) {
//            new Stock(notifier, code, 1000, "28-Mar-2019").start();
        }


//        new Stock(notifier, "test", 700).start();
//        for (String strike: strikes){
//        new Stock(notifier, "cnxban", 1000, "P", "30000", "28-Mar-2019").start();
//        new Stock(notifier, "nifty", 1000, "28-Mar-2019", 75).start();
//        new Stock(notifier, "cnxban", 1000, "11-Apr-2019", 20).start();
//        new Stock(notifier, "cnxban", 1000, "P", "30400", "18-Apr-2019", 20).start();
//        new Stock(notifier, "nifty", 1000, "P", "11000", "19-Sep-2019", 20).start();
//        new Stock(notifier, "nifty", 1000, "P", "31400", "28-Mar-2019", 75).start();
//        }

        while (true) {
            InputStreamReader r = new InputStreamReader(System.in);
            BufferedReader br = new BufferedReader(r);
            System.out.println("Enter cmd: ");
            try {
                String s = br.readLine();
                System.out.println(" --------->> cmd : " + s);
                if (s.equalsIgnoreCase("pause"))
                    StockParse.pause = true;
                if (s.equalsIgnoreCase("resume"))
                    StockParse.pause = false;

            } catch (Exception e) {
                System.out.println("INvalid input.. ");
            }


        }

    }

}
