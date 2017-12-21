package org.openhab.binding.megad.i2c;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class I2C {

    private Logger logger = LoggerFactory.getLogger(I2C.class);
    String host;
    String SDA;
    String SCL;
    String password;
    int LOW = 0;
    int HIGH = 1;
    boolean isInit = false;

    public I2C(String host, String password, String port, String scl) {
        this.host = host;
        this.SDA = port;
        this.SCL = scl;
        this.password = password;

    }

    private void i2c_stop() {

        String request = "http://" + this.host + "/" + this.password + "/?" + "cmd=" + this.SDA + ":" + LOW + ";"
                + this.SCL + ":" + HIGH + ";" + this.SDA + ":" + HIGH;
        // sendCommand(request);

    }

    private void i2c_init() {
        String request = "http://" + this.host + "/" + this.password + "/?" + "cmd=" + this.SCL + ":" + HIGH + ";"
                + this.SDA + ":" + HIGH;
        // sendCommand(request);
    }

    private void i2c_send(String data) {

        String request = "http://" + this.host + "/" + this.password + "/?" + "pt=" + this.SDA + "&i2c="
                + Integer.parseInt(data, 16) + "&scl=" + this.SCL + ":1;" + this.SCL + ":0;";
        // sendCommand(request);

        // file_get_contents(MD."pt=".SDA."&i2c=".hexdec($data)."&scl=".SCL.":1;".SCL.":0;");

    }

    private void i2c_start() {

        String request = "http://" + this.host + "/" + this.password + "/?" + "cmd=" + this.SDA + ":" + LOW + ";"
                + this.SCL + ":" + LOW;
        // sendCommand(request);
    }

    private void sendCommand(String Result) {

        HttpURLConnection con;

        URL MegaURL;

        try {
            MegaURL = new URL(Result);
            con = (HttpURLConnection) MegaURL.openConnection();
            // optional default is GET
            con.setReadTimeout(500);
            con.setConnectTimeout(500);
            con.setRequestMethod("GET");

            // add request header
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            if (con.getResponseCode() == 200) {
                logger.debug("OK");
            }
            con.disconnect();
        } catch (MalformedURLException e) {
            logger.error("1" + e);
            e.printStackTrace();
        } catch (ProtocolException e) {
            logger.error("2" + e);
            e.printStackTrace();
        } catch (IOException e) {
            logger.error("Connect to megadevice " + host + " error: " + e.getLocalizedMessage());
        }

    }

    private void display_init() {

        i2c_stop();
        i2c_init();
        i2c_start();

        i2c_send("78");
        i2c_send("00");

        i2c_send("AF"); // Display ON

        i2c_send("D5"); // Display Clock ?
        i2c_send("80"); // Default 80

        i2c_send("81"); // Contrast
        i2c_send("EE");

        i2c_send("8D"); // Charge Pump (иначе не включится!)
        i2c_send("14");
        i2c_send("AF"); // Display ON

        i2c_send("A1"); // Set Segment Re-map // Default A0 слева направо или справа на лево
        i2c_send("C8"); // Set COM Output // Default C0 сверху вниз или снизу вверх

        i2c_send("A6");

        i2c_stop();

    }

    private void clear_display() {

        i2c_start();
        i2c_send("78");
        i2c_send("00");
        i2c_send("20");
        i2c_send("00");
        i2c_send("21");
        i2c_send("00");
        i2c_send("7F");
        i2c_send("22");
        i2c_send("00");
        i2c_send("07");

        i2c_stop();
        i2c_start();

        i2c_send("78");
        i2c_send("40");

        for (int i = 0; i < 1024; i++) {
            i2c_send("00");
        }

        i2c_stop();

    }

    public void prepare_display() {
        if (!isInit) {
            display_init();
            clear_display();
            write_text("Welcome! Display ready to work", "default", 0, 0);
            isInit = true;
        }
    }

    private void write_text(String string, String font, int column, int page) {

        String splitByWordsData[] = string.split(" ");

        i2c_start();
        i2c_send("78");
        i2c_send("00");
        i2c_send("20");
        i2c_send("41");
        i2c_send("21");
        i2c_send(Integer.toHexString(column));
        i2c_send("7F");
        i2c_send("22");
        i2c_send(Integer.toHexString(page));
        i2c_send(Integer.toHexString(page + 1));

        i2c_stop();
        i2c_start();
        i2c_send("78");
        i2c_send("40");

        for (int j = 0; j < splitByWordsData.length; j++) {
            int flag = 1;

            for (int i = 0; i < printString(splitByWordsData[j]).length; i++) {
                i2c_send(printString(splitByWordsData[j])[i + flag]);
                flag = flag * -1;
            }

            i2c_send("00");
            i2c_send("00");
        }

    }

    private String[] printString(String string) {

        String result[] = null;

        return result;
    }

}
