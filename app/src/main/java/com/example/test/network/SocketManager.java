package com.example.test.network;

import android.util.Log;

import com.example.test.model.SensorData;

import org.json.JSONObject;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Менеджер WebSocket-подключения к серверу теплицы.
 */
public class SocketManager {

    private static final String TAG = "SocketManager";
    private static final String SERVER_URL = "https://greenhouse-server09.onrender.com";

    private Socket socket;

    // Слушатели событий
    public interface SensorDataListener {
        void onSensorData(SensorData data);
    }

    public interface SettingsListener {
        void onSettingsChanged(int maxTemp, int minHum, int minLight);
    }

    public interface SessionListener {
        void onSessionStarted(int sessionId, String startTime);
        void onSessionEnded(int sessionId, String endTime);
    }

    public interface ConnectionStateListener {
        void onConnected();
        void onDisconnected();
        void onConnectionError(String error);
    }

    private SensorDataListener sensorDataListener;
    private SettingsListener settingsListener;
    private ConnectionStateListener connectionStateListener;
    private SessionListener sessionListener;

    public void setSensorDataListener(SensorDataListener listener) {
        this.sensorDataListener = listener;
    }

    public void setSettingsListener(SettingsListener listener) {
        this.settingsListener = listener;
    }

    public void setSessionListener(SessionListener listener) {
        this.sessionListener = listener;
    }

    public void setConnectionStateListener(ConnectionStateListener listener) {
        this.connectionStateListener = listener;
    }

    /**
     * Подключиться к серверу по WebSocket
     */
    public void connect() {
        // Если уже подключён — отключаемся
        if (socket != null && socket.connected()) {
            disconnect();
        }

        try {
            Log.d(TAG, "Connecting to: " + SERVER_URL);

            IO.Options options = new IO.Options();
            options.forceNew = true;
            options.reconnection = true;
            options.reconnectionDelay = 2000;
            options.reconnectionAttempts = 10;
            options.timeout = 10000;

            socket = IO.socket(SERVER_URL, options);

            // sensor_update — сервер прислал новые данные с датчиков
            socket.on("sensor_update", args -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    float temp = (float) data.getDouble("temperature");
                    float hum = (float) data.getDouble("humidity");
                    float light = (float) data.getDouble("light");
                    SensorData sensorData = new SensorData(temp, hum, light);

                    if (sensorDataListener != null) {
                        sensorDataListener.onSensorData(sensorData);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing sensor_update", e);
                }
            });

            // settings_update — сервер прислал обновлённые настройки
            socket.on("settings_update", args -> {
                try {
                    JSONObject settings = (JSONObject) args[0];
                    int maxTemp = settings.optInt("max_temp", 25);
                    int minHum = settings.optInt("min_hum", 40);
                    int minLight = settings.optInt("min_light", 30);

                    if (settingsListener != null) {
                        settingsListener.onSettingsChanged(maxTemp, minHum, minLight);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing settings_update", e);
                }
            });

            // session_started — сервер начал новую сессию ESP
            socket.on("session_started", args -> {
                try {
                    JSONObject payload = (JSONObject) args[0];
                    int sessionId = payload.optInt("id", -1);
                    String startTime = payload.optString("start_time", "");
                    if (sessionListener != null) {
                        sessionListener.onSessionStarted(sessionId, startTime);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing session_started", e);
                }
            });

            // session_ended — сервер завершил сессию ESP
            socket.on("session_ended", args -> {
                try {
                    JSONObject payload = (JSONObject) args[0];
                    int sessionId = payload.optInt("id", -1);
                    String endTime = payload.optString("end_time", "");
                    if (sessionListener != null) {
                        sessionListener.onSessionEnded(sessionId, endTime);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing session_ended", e);
                }
            });

            // connect — успешное подключение
            socket.on(Socket.EVENT_CONNECT, args -> {
                Log.d(TAG, "WebSocket connected");
                if (connectionStateListener != null) {
                    connectionStateListener.onConnected();
                }
            });

            // disconnect — отключение
            socket.on(Socket.EVENT_DISCONNECT, args -> {
                Log.d(TAG, "WebSocket disconnected");
                if (connectionStateListener != null) {
                    connectionStateListener.onDisconnected();
                }
            });

            // connect_error — ошибка подключения
            socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
                String error = args.length > 0 ? args[0].toString() : "Unknown error";
                Log.e(TAG, "WebSocket connect error: " + error);
                if (connectionStateListener != null) {
                    connectionStateListener.onConnectionError(error);
                }
            });

            socket.connect();

        } catch (Exception e) {
            Log.e(TAG, "Error creating socket", e);
            if (connectionStateListener != null) {
                connectionStateListener.onConnectionError(e.getMessage());
            }
        }
    }

    /**
     * Отправить настройки на сервер через WebSocket
     */
    public void sendSettings(int maxTemp, int minHum, int minLight) {
        if (socket == null || !socket.connected()) {
            Log.w(TAG, "Socket not connected, cannot send settings");
            return;
        }

        try {
            JSONObject settings = new JSONObject();
            settings.put("max_temp", maxTemp);
            settings.put("min_hum", minHum);
            settings.put("min_light", minLight);

            socket.emit("settings_push", settings);
            Log.d(TAG, "Settings sent via WebSocket");
        } catch (Exception e) {
            Log.e(TAG, "Error sending settings via WebSocket", e);
        }
    }

    /**
     * Запросить историю текущей сессии
     */
    public void requestHistory(final HistoryCallback callback) {
        if (socket == null || !socket.connected()) {
            Log.w(TAG, "Socket not connected, cannot request history");
            return;
        }

        socket.emit("get_history", new io.socket.client.Ack() {
            @Override
            public void call(Object... args) {
                try {
                    org.json.JSONArray array = (org.json.JSONArray) args[0];
                    if (callback != null) {
                        java.util.List<SensorData> list = new java.util.ArrayList<>();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            float temp = (float) obj.getDouble("temperature");
                            float hum = (float) obj.getDouble("humidity");
                            float light = (float) obj.getDouble("light");
                            list.add(new SensorData(temp, hum, light));
                        }
                        callback.onHistoryReceived(list);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing history", e);
                    if (callback != null) {
                        callback.onHistoryReceived(new java.util.ArrayList<>());
                    }
                }
            }
        });
    }

    /**
     * Запросить текущие настройки
     */
    public void requestSettings(final SettingsRequestCallback callback) {
        if (socket == null || !socket.connected()) {
            Log.w(TAG, "Socket not connected, cannot request settings");
            return;
        }

        socket.emit("get_settings", new io.socket.client.Ack() {
            @Override
            public void call(Object... args) {
                try {
                    if (args[0] != null) {
                        JSONObject settings = (JSONObject) args[0];
                        int maxTemp = settings.optInt("max_temp", 25);
                        int minHum = settings.optInt("min_hum", 40);
                        int minLight = settings.optInt("min_light", 30);
                        callback.onSettingsReceived(maxTemp, minHum, minLight);
                    } else {
                        callback.onSettingsReceived(25, 40, 30);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing settings response", e);
                    callback.onSettingsReceived(25, 40, 30);
                }
            }
        });
    }

    /**
     * Отключиться от сервера
     */
    public void disconnect() {
        if (socket != null) {
            socket.disconnect();
            socket = null;
            Log.d(TAG, "Socket disconnected");
        }
    }

    /**
     * Проверить, подключён ли сокет
     */
    public boolean isConnected() {
        return socket != null && socket.connected();
    }

    public interface HistoryCallback {
        void onHistoryReceived(java.util.List<SensorData> history);
    }

    public interface SettingsRequestCallback {
        void onSettingsReceived(int maxTemp, int minHum, int minLight);
    }
}
