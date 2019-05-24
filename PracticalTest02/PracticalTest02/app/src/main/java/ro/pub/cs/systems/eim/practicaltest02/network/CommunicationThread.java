package ro.pub.cs.systems.eim.practicaltest02.network;

import android.util.Log;


import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.ResponseHandler;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.impl.client.BasicResponseHandler;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import ro.pub.cs.systems.eim.practicaltest02.general.Constants;
import ro.pub.cs.systems.eim.practicaltest02.general.Utilities;
import ro.pub.cs.systems.eim.practicaltest02.model.Alarm;


public class CommunicationThread extends Thread {

    private ServerThread serverThread;
    private Socket socket;

    public CommunicationThread(ServerThread serverThread, Socket socket) {
        this.serverThread = serverThread;
        this.socket = socket;
    }

    @Override
    public void run() {
        if (socket == null) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] Socket is null!");
            return;
        }
        try {
            // luam bufferul
            BufferedReader bufferedReader = Utilities.getReader(socket);
            PrintWriter printWriter = Utilities.getWriter(socket);

            if (bufferedReader == null || printWriter == null) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Buffered Reader / Print Writer are null!");
                return;
            }

            Log.i(Constants.TAG, "[COMMUNICATION THREAD] Waiting for parameters from client (hour/minute / information type!");

            HashMap<String, Alarm> data = serverThread.getData();
            Alarm alarm = null;
            // citim ora, minutul
            String hour = bufferedReader.readLine();
            String minute = bufferedReader.readLine();
            String informationType = bufferedReader.readLine();


            Log.i(Constants.TAG, "[COMMUNICATION THREAD] " +hour+":" + minute + " "+informationType);

            if (informationType.equals("set")) {
                // daca exista alarma, suprascriu
                if (data.containsKey(socket.getInetAddress().toString())) {
                    Log.i(Constants.TAG, "[COMMUNICATION THREAD] Getting the information from the cache...");
                    alarm = data.get(socket.getInetAddress().toString());
                    alarm.setHour(hour);
                    alarm.setMinute(minute);
                    serverThread.setData(socket.getInetAddress().toString(), alarm);

                } else {
                    alarm = new Alarm(hour, minute);
//                    alarm.setHour(hour);
//                    alarm.setMinute(minute);
                    serverThread.setData(socket.getInetAddress().toString(), alarm);
                    // altfel o setez
                }
            } else if (informationType.equals("reset")){
                serverThread.removeAlarm(socket.getInetAddress().toString());
                Log.i(Constants.TAG, "[COMMUNICATION THREAD] Remove alarm from cache...");
            } else { //poll
                // get the current hour and minute
                Log.i(Constants.TAG, "[COMMUNICATION THREAD] Poll alarm ...");
                String currentHour = null;
                String currentMinute = null;

                String hostname = "time.nist.gov";
                int port = 13;
                Socket socket = null;
                try {
                    socket = new Socket(hostname, port);
                } catch (UnknownHostException unknownHostException) {
                    Log.e(Constants.TAG, "[COMMUNICATION THREAD] unknownHostException...");
                    return;
                } catch (IOException e) {
                    Log.e(Constants.TAG, "[COMMUNICATION THREAD] IOException...");
                    return;
                }

                String currentLine = "";
                try {

                    BufferedReader bufferedReader2 = Utilities.getReader(socket);
                    currentLine = bufferedReader2.readLine();
                    currentLine = bufferedReader2.readLine();
                } catch (IOException e) {
                    Log.e(Constants.TAG, "[COMMUNICATION THREAD] IOException...");
                    return;
                }

                Log.i(Constants.TAG, "[COMMUNICATION THREAD] line:" + currentLine);


                Log.i(Constants.TAG, "[COMMUNICATION THREAD] currentLine:" + currentLine);
                int index = currentLine.indexOf(' ', currentLine.indexOf(' ') + 1);
                String time = currentLine.substring(index+1, index+6);

                currentHour = time.substring(0,2);
                currentMinute = time.substring(3,5);


                Log.i(Constants.TAG, "[COMMUNICATION THREAD] hour:" + currentHour + "minute:" + currentMinute) ;


                Log.i(Constants.TAG, "[COMMUNICATION THREAD] time:" + time);
                String result = "";
                if (data.containsKey(socket.getInetAddress().toString())) {
                    Log.i(Constants.TAG, "[COMMUNICATION THREAD] NONE...");
                    result = "none";
                } else {
                    if (hour.compareTo(currentHour) < 0) {
                        //inactiv
                        Log.i(Constants.TAG, "[COMMUNICATION THREAD] Inactive Alarm...");
                        result = "inactive";
                    } else if (hour.compareTo(currentHour) == 0){
                        if (minute.compareTo(currentMinute) < 0) {
                            // inactiv
                            Log.i(Constants.TAG, "[COMMUNICATION THREAD] Inactive Alarm...");
                            result = "inactive";
                        } else {
                            //activ
                            Log.i(Constants.TAG, "[COMMUNICATION THREAD] Active Alarm...");
                            result = "active";
                        }
                    } else {
                        //activ
                        Log.i(Constants.TAG, "[COMMUNICATION THREAD] Active Alarm...");
                        result = "active";
                    }
                }


                String hostname2 = "localhost";
                int port2 = 4646;
                Socket socket2 = null;
                try {
                    socket2 = new Socket(hostname2, port2);
                } catch (UnknownHostException unknownHostException) {
                    Log.e(Constants.TAG, "[COMMUNICATION THREAD] unknownHostException...");
                    return;
                } catch (IOException e) {
                    Log.e(Constants.TAG, "[COMMUNICATION THREAD] IOException...");
                    return;
                }


                BufferedReader bufferedReader3 = Utilities.getReader(socket2);

                PrintWriter printWriter3 = Utilities.getWriter(socket2);
                if (bufferedReader == null || printWriter == null) {
                    Log.e(Constants.TAG, "[CLIENT THREAD] Buffered Reader / Print Writer are null!");
                    return;
                }
                printWriter.println(result);
                printWriter.flush();

            }



//            // daca e in chache o luam de acolo
//            if (data.containsKey(socket.getInetAddress().toString())) {
//                Log.i(Constants.TAG, "[COMMUNICATION THREAD] Getting the information from the cache...");
//                alarm = data.get(socket.getInetAddress().toString());

                // altfel o luam de pe net
          //  } else {
//                Log.i(Constants.TAG, "[COMMUNICATION THREAD] Getting the information from the webservice...");
//                HttpClient httpClient = new DefaultHttpClient();
//
//                // folosim post
//                HttpPost httpPost = new HttpPost("http://autocomplete.wunderground.com/aq?query=" + city);
//                Log.i(Constants.TAG, "ce e asta?    " + httpPost.toString());


                //  folosim get
//                HttpGet httpPost = new HttpGet("http://autocomplete.wunderground.com/aq?query=" + city);

                //  adaugare parametri
//                List<NameValuePair> params = new ArrayList<>();
//                params.add(new BasicNameValuePair(Constants.QUERY_ATTRIBUTE, city));
//                UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(params, HTTP.UTF_8);
//                httpPost.setEntity(urlEncodedFormEntity);

//                Log.i(Constants.TAG, "am ajuns aici");
//
//                ResponseHandler<String> responseHandler = new BasicResponseHandler();
//                String pageSourceCode = httpClient.execute(httpPost, responseHandler);
//
//                Log.i(Constants.TAG, "am trecut de aici");
//
//                if (pageSourceCode == null) {
//                    Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error getting the information from the webservice!");
//                    return;
//                }
//
//                Log.i(Constants.TAG, "pageSourceCode:    " + pageSourceCode);
//                // parsare pagina web
//                Document document = Jsoup.parse(pageSourceCode);
//
//                JSONObject content = new JSONObject(pageSourceCode);
//                String currentObservation = content.getString("RESULTS");
//                Log.i(Constants.TAG, "currentObservation:1    " + currentObservation);
//                content = new JSONObject(currentObservation.substring(1,currentObservation.length()-1));
//                String timezone = content.getString("tz");
//                //JSONObject currentObservation = content.getJSONObject("RESULTS");
//                Log.i(Constants.TAG, "timezone:2    " + timezone);

               // String temperature = currentObservation.getString(Constants.TEMPERATURE);
//                Log.i(Constants.TAG, "Document:    " + document.toString());
//
//                Element element = document.child(0);
//                Log.i(Constants.TAG, "Element:    " + element.toString());
//
//                Elements elements = element.getElementsByTag(Constants.SCRIPT_TAG);
//                Log.i(Constants.TAG, "Elements:    " + elements.toString());

//              actualizam informatiile
               // alarm  = new Alarm(document.toString());
               // Log.d("abc", city);
              //  serverThread.setData(city, alarm);


//                for (Element script: elements) {
//                    String scriptData = script.data();
//                    Log.i(Constants.TAG, "Script data:    " + scriptData.toString());
//
//                    if (scriptData.contains(Constants.SEARCH_KEY)) {
//                        int position = scriptData.indexOf(Constants.SEARCH_KEY) + Constants.SEARCH_KEY.length();
//                        scriptData = scriptData.substring(position);
//                        JSONObject content = new JSONObject(scriptData);
//                        JSONObject currentObservation = content.getJSONObject(Constants.CURRENT_OBSERVATION);
//                        String temperature = currentObservation.getString(Constants.TEMPERATURE);
//                        String windSpeed = currentObservation.getString(Constants.WIND_SPEED);
//                        String condition = currentObservation.getString(Constants.CONDITION);
//                        String pressure = currentObservation.getString(Constants.PRESSURE);
//                        String humidity = currentObservation.getString(Constants.HUMIDITY);
//                        alarm = new Alarm(
//                                temperature, windSpeed, condition, pressure, humidity
//                        );
//                        serverThread.setData(city, alarm);
//                        break;
//                    }
//                }

           // }

//            Log.i(Constants.TAG, "De afisat:    " + alarm.toString());
//            String result = alarm.toString();
//            if (alarm == null) {
//                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Weather Forecast Information is null!");
//                return;
//            }
//            String result = null;
//            switch (informationType) {
//                case Constants.ALL:
//                    result = alarm.toString();
//                    break;
//                case Constants.TEMPERATURE:
//                    result = alarm.getTemperature();
//                    break;
//                case Constants.WIND_SPEED:
//                    result = alarm.getWindSpeed();
//                    break;
//                case Constants.CONDITION:
//                    result = alarm.getCondition();
//                    break;
//                case Constants.HUMIDITY:
//                    result = alarm.getHumidity();
//                    break;
//                case Constants.PRESSURE:
//                    result = alarm.getPressure();
//                    break;
//                default:
//                    result = "[COMMUNICATION THREAD] Wrong information type (all / temperature / wind_speed / condition / humidity / pressure)!";
//            }
//            printWriter.println(result);
//            printWriter.flush();
        } catch (IOException ioException) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
            if (Constants.DEBUG) {
                ioException.printStackTrace();
            }
//        } catch (JSONException jsonException) {
//            Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + jsonException.getMessage());
//            if (Constants.DEBUG) {
//                jsonException.printStackTrace();
//            }
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ioException) {
                    Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
                    if (Constants.DEBUG) {
                        ioException.printStackTrace();
                    }
                }
            }
        }
    }

}