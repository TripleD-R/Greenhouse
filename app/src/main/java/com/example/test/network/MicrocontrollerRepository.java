package com.example.test.network;

import com.example.test.model.SensorData;
import com.example.test.model.SessionItem;
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
                    float temp = (float) obj.optDouble("temperature", 0);
                    float hum = (float) obj.optDouble("humidity", 0);
                    float light = (float) obj.optDouble("light", 0);
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
                    float temp = (float) obj.optDouble("temperature", 0);
                    float hum = (float) obj.optDouble("humidity", 0);
                    float light = (float) obj.optDouble("light", 0);
                    list.add(new SensorData(temp, hum, light));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // Получение списка всех сессий
    public java.util.List<com.example.test.model.SessionItem> fetchSessions() {
        Request request = new Request.Builder()
                .url(API_URL + "/sessions")
                .build();

        List<com.example.test.model.SessionItem> sessions = new java.util.ArrayList<>();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    SessionItem session = new SessionItem(
                            obj.optInt("id", -1),
                            obj.optString("status", ""),
                            obj.optString("device_ip", ""),
                            obj.optString("start_time", ""),
                            obj.optString("end_time", ""),
                            obj.optInt("data_count", 0)
                    );
                    sessions.add(session);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sessions;
    }

    // Получение данных по конкретной сессии
    public List<SensorData> fetchSessionData(int sessionId) {
        Request request = new Request.Builder()
                .url(API_URL + "/sessions/" + sessionId + "/data")
                .build();

        List<SensorData> list = new ArrayList<>();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    float temp = (float) obj.optDouble("temperature", 0);
                    float hum = (float) obj.optDouble("humidity", 0);
                    float light = (float) obj.optDouble("light", 0);
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
