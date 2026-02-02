package com.example.test.network;

import com.example.test.model.SensorData;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import java.io.IOException;

public class MicrocontrollerRepository {
    private final OkHttpClient client = new OkHttpClient();

    // Отправка команд
    public String post(String ip, String command) {
        Request request = new Request.Builder()
                .url("http://" + ip + "/" + command)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    // Отправка значений настроек (maxTemp, minHum, minLight)
    public String sendSettings(String ip, int maxTemp, int minHum, int minLight) {
        String url = "http://" + ip + "/set?maxTemp=" + maxTemp + "&minHum=" + minHum + "&minLight=" + minLight;
        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    // Получение данных от микроконтроллера (/data возвращает данные в формате JSON)
    public SensorData fetchSensorData(String ip) {
        String url = "http://" + ip + "/data";
        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();

                // обработка JSON может выбросить JSONException → ловим отдельно
                try {
                    JSONObject obj = new JSONObject(json);

                    float temp = (float) obj.getDouble("temperature");
                    float hum = (float) obj.getDouble("humidity");
                    float light = (float) obj.getDouble("light");

                    return new SensorData(temp, hum, light);
                } catch (org.json.JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
