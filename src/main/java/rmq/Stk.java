package rmq;

import com.google.common.collect.Lists;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.joda.time.DateTime;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
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
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Created by mukesh.bang on 07/12/16.
 */

enum OrderType {
    MIS, NRML;
}

class Stock {
    private static final boolean US_STK = true;
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
        url = MessageFormat.format(Stk.URL, seriesName);
//        System.out.println("url:" + url);
        connection = Jsoup.connect(url);
    }

    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    private double sendGet() throws Exception {

        HttpGet request = new HttpGet(url);

        // add request headers
        request.addHeader("authorization", "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJleHAiOjE1ODEyMjExNjUsInRva2VuIjoiSFFSOVl3WGdmeG5EMlVnQUZlMlpna1dPb3pXVEk1IiwidXNlcl9pZCI6IjQxZjkzZjY5LWVkZDctNDNjYi04NGMzLTQ5YzI5MzVjNzJlOCIsImRldmljZV9oYXNoIjoiOGY1NTU0ZWQ2ZDJkOWEwZmFlNjQxYzg5OWQ0ZTkzNDUiLCJzY29wZSI6IndlYl9saW1pdGVkIiwidXNlcl9vcmlnaW4iOiJVUyIsIm9wdGlvbnMiOmZhbHNlLCJsZXZlbDJfYWNjZXNzIjpmYWxzZX0.bWxCv_YdzxMTV1HKkR5Mb2k2UF3O71jfdjHDezvjXRB72FMaSNN0eyEqDYy6ibMS-R4VJ3oXcOe9acNiCsNtyBU7_YBGKVaDHvraqODjhsoipmBEXr3TREHHCNxh7K0l3SZH3ffxsO4PL9u9s1xyUWimo1fmBcXw3AzFBxNeHfkdUi2-EGrrjzp2B9tEAVyL0ZONlqqwrsJsqHABrY9t6DCBIRMTRvXLUzT7bcVWZEOrlcdjQ2nb5FKk5uLe5nCp51TwU-WyU8L7mTl76vIReNaEZ-nxxa16CcXS9Z4apKcSjWd2ynz-Xnt8gdkiF0dmFrKTvLd7GUVagm67FNU2vQ");
        request.addHeader("authority", "api.robinhood.com");

        try (CloseableHttpResponse response = httpClient.execute(request)) {

            // Get HttpResponse Status
            System.out.println(response.getStatusLine().toString());

            HttpEntity entity = response.getEntity();
            Header headers = entity.getContentType();
//            System.out.println(headers);

            if (entity != null) {
                // return it as a String
                String result = EntityUtils.toString(entity);

                Object obj = new JSONParser().parse(result);
                String val = (String)((JSONObject)((JSONArray)((JSONObject)obj).get("results")).get(0)).get("last_trade_price");
//                String val = (String)((JSONObject)((JSONArray)((JSONObject)obj).get("data")).get(1)).get("lastSalePrice");

                System.out.println(val);
                return Double.parseDouble(val);
            }

        }

        return 0;
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
            ;
            double val = sendGet();
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
            mainPanel.add(btnPnl, BorderLayout.SOUTH);

            frame_.add(mainPanel);
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

                    if (xList.size() > 400) {
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
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println(seriesName + " -- Done...");

        }


    }

    public double getBankValue() throws Exception {

        if(US_STK)
            return sendGet();

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

public class Stk {

    //    static String URL = "https://secure.icicidirect.com/IDirectTrading/Trading/FNO/GetQuoteData.aspx?FFO_XCHNG_CD=NFO&FFO_PRDCT_TYP=F&FFO_UNDRLYNG={0}&FFO_EXPRY_DT=29-Jun-2017&FFO_EXER_TYP=E&FFO_OPT_TYP=";
    static String URL = "https://api.robinhood.com/marketdata/quotes/?instruments=https%3A%2F%2Fapi.robinhood.com%2Finstruments%2F{0}%2F";

    static String NDX_URL = "https://api.nasdaq.com/api/quote/watchlist?symbol=indu|index&symbol=ndx|index|futures";
    public static boolean pause = false;
    static FileWriter fw;

    public Stk() throws IOException {
        fw = new FileWriter(new File("banks.txt"), true);

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
        List<String> stocks2 = Lists.newArrayList("bhainf","asipai","jubfoo"
//                new Pair("intavi", 2600),
//                new Pair("sunpha", 2600),
//                new Pair("inftec", 2600),
//                new Pair("indho", 2600)
//                new Pair("EICMOT", 2600),
//                new Pair("divlab", 2600),
//                new Pair("rblban", 2600)
        );

        List<String> stocks = Lists.newArrayList(
//                new Pair("iciban", 2600),
//                new Pair("axiban", 2600),
//                new Pair("indho", 2600),
//                new Pair("kotmah", 2600),
//                new Pair("zeeent", 2600),
//                new Pair("batind", 2600),
//                "cnxban",
                "tesla"
        );

        List<String> codes = Lists.newArrayList(  "dlflim", "eicmot","drredd", "bajfi", "iciban", "axiban","nifty","cnxban","kotmah" );
//        new Stock(notifier, "axiban", 1000, "27-Jun-2019", 6500).start();

        for (String stock: stocks){
            new Stock(notifier, "50810c35-d215-4866-9758-0ada4ac79ffa", 25000, "27-Feb-2020", Integer.parseInt("100")).start();
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
                    Stk.pause = true;
                if (s.equalsIgnoreCase("resume"))
                    Stk.pause = false;

            } catch (Exception e) {
                System.out.println("INvalid input.. ");
            }


        }

    }

}
