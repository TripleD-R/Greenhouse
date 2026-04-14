package com.example.test.network;

import com.example.test.model.SensorData;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MicrocontrollerRepository {

    private static final String SERVER_URL = "https://greenhouse-server09.onrender.com";
    private static final String API_URL = SERVER_URL + "/api";

    private final OkHttpClient client = new OkHttpClient();

    // Получение текущих данных (последние 20 значений сессии)
    public SensorData fetchSensorData() {
        Request request = new Request.Builder()
                .url(API_URL + "/data")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                JSONArray array = new JSONArray(json);
                if (array.length() > 0) {
                    JSONObject obj = array.getJSONObject(array.length() - 1);
                    float temp = (float) obj.getDouble("temperature");
                    float hum = (float) obj.getDouble("humidity");
                    float light = (float) obj.getDouble("light");
                    return new SensorData(temp, hum, light);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Получение истории текущей сессии (до 20 значений)
    public List<SensorData> fetchHistory() {
        Request request = new Request.Builder()
                .url(API_URL + "/data")
                .build();

        List<SensorData> list = new ArrayList<>();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    float temp = (float) obj.getDouble("temperature");
                    float hum = (float) obj.getDouble("humidity");
                    float light = (float) obj.getDouble("light");
                    list.add(new SensorData(temp, hum, light));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // Отправка настроек (JSON body)
    public boolean sendSettings(int maxTemp, int minHum, int minLight) {
        try {
            JSONObject json = new JSONObject();
            json.put("max_temp", maxTemp);
            json.put("min_hum", minHum);
            json.put("min_light", minLight);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(API_URL + "/settings")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Получение текущих настроек
    public JSONObject fetchSettings() {
        Request request = new Request.Builder()
                .url(API_URL + "/settings")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                return new JSONObject(json);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Проверка связи с сервером
    public boolean pingServer() {
        Request request = new Request.Builder()
                .url(SERVER_URL)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
