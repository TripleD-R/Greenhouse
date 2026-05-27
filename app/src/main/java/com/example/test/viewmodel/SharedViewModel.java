package com.example.test.viewmodel;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.test.model.SensorData;
import com.example.test.model.SessionItem;
import com.example.test.network.MicrocontrollerRepository;
import com.example.test.network.SocketManager;
import com.example.test.repository.SensorHistoryRepository;

import java.util.List;

public class SharedViewModel extends AndroidViewModel {

    private static final String PREF_NAME = "MyPref";
    private static final String KEY_MAX_TEMP = "max_temp";
    private static final String KEY_MIN_HUM = "min_hum";
    private static final String KEY_MIN_LIGHT = "min_light";

    private final MicrocontrollerRepository repo;
    private final SensorHistoryRepository historyRepository;
    private final SocketManager socketManager;

    private final MutableLiveData<SensorData> sensorData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isConnected = new MutableLiveData<>(false);
    private final MutableLiveData<String> connectionStatus = new MutableLiveData<>("Не подключено");

    private final MutableLiveData<Integer> maxTempLive = new MutableLiveData<>(25);
    private final MutableLiveData<Integer> minHumLive = new MutableLiveData<>(40);
    private final MutableLiveData<Integer> minLightLive = new MutableLiveData<>(30);

    private final MutableLiveData<Long> sessionStarted = new MutableLiveData<>();
    private final MutableLiveData<Long> sessionEnded = new MutableLiveData<>();
    private final MutableLiveData<Integer> activeSessionId = new MutableLiveData<>();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable;
    private boolean isUpdating = false;
    private static Toast currentToast;

    // Флаг: используем WebSocket или HTTP polling
    private boolean useWebSocket = false;

    // Текущие настройки (для отображения "не сохранено")
    private int currentMaxTemp = 25;
    private int currentMinHum = 40;
    private int currentMinLight = 30;

    public SharedViewModel(@NonNull Application application) {
        super(application);
        historyRepository = new SensorHistoryRepository(application);
        repo = new MicrocontrollerRepository();
        socketManager = new SocketManager();

        // Настраиваем слушатели WebSocket
        setupSocketListeners();

        // Автоматическое подключение при запуске приложения
        connectToServer();
    }

    private void setupSocketListeners() {
        // Данные с датчиков через WebSocket
        socketManager.setSensorDataListener(data -> {
            sensorData.postValue(data);
            historyRepository.insert(data);
        });

        // Настройки изменились (кто-то обновил)
        socketManager.setSettingsListener((maxTemp, minHum, minLight) -> {
            saveSettings(getApplication(), maxTemp, minHum, minLight);
            currentMaxTemp = maxTemp;
            currentMinHum = minHum;
            currentMinLight = minLight;
            maxTempLive.postValue(maxTemp);
            minHumLive.postValue(minHum);
            minLightLive.postValue(minLight);
            // Не показываем тост здесь — он уже показан при отправке настроек
            // showToast(getApplication(), "Настройки обновлены");
        });

        // Состояние подключения
        socketManager.setConnectionStateListener(new SocketManager.ConnectionStateListener() {
            @Override
            public void onConnected() {
                isConnected.postValue(true);
                connectionStatus.postValue("Подключено");
                useWebSocket = true;

                // Запрашиваем текущие настройки
                socketManager.requestSettings((maxTemp, minHum, minLight) -> {
                    saveSettings(getApplication(), maxTemp, minHum, minLight);
                });
            }

            @Override
            public void onDisconnected() {
                isConnected.postValue(false);
                connectionStatus.postValue("Отключено");
                useWebSocket = false;
            }

            @Override
            public void onConnectionError(String error) {
                isConnected.postValue(false);
                connectionStatus.postValue("Ошибка подключения");
                useWebSocket = false;
            }
        });

        socketManager.setSessionListener(new SocketManager.SessionListener() {
            @Override
            public void onSessionStarted(int sessionId, String startTime) {
                clearLocalHistory();
                sessionStarted.postValue(System.currentTimeMillis());
                activeSessionId.postValue(sessionId);
            }

            @Override
            public void onSessionEnded(int sessionId, String endTime) {
                sessionEnded.postValue(System.currentTimeMillis());
                activeSessionId.postValue(null);
            }
        });
    }

    // ======================== Настройки ========================

    public void saveSettings(Context context, int maxTemp, int minHum, int minLight) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        pref.edit()
                .putInt(KEY_MAX_TEMP, maxTemp)
                .putInt(KEY_MIN_HUM, minHum)
                .putInt(KEY_MIN_LIGHT, minLight)
                .apply();
        currentMaxTemp = maxTemp;
        currentMinHum = minHum;
        currentMinLight = minLight;
        maxTempLive.postValue(maxTemp);
        minHumLive.postValue(minHum);
        minLightLive.postValue(minLight);
    }

    public int getMaxTemp(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return pref.getInt(KEY_MAX_TEMP, 25);
    }

    public int getMinHum(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return pref.getInt(KEY_MIN_HUM, 40);
    }

    public int getMinLight(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return pref.getInt(KEY_MIN_LIGHT, 30);
    }

    public int getCurrentMaxTemp() { return currentMaxTemp; }
    public int getCurrentMinHum() { return currentMinHum; }
    public int getCurrentMinLight() { return currentMinLight; }

    public LiveData<Integer> getMaxTempLive() { return maxTempLive; }
    public LiveData<Integer> getMinHumLive() { return minHumLive; }
    public LiveData<Integer> getMinLightLive() { return minLightLive; }

    // ======================== LiveData ========================

    public LiveData<SensorData> getSensorData() {
        return sensorData;
    }

    public LiveData<Boolean> getIsConnected() {
        return isConnected;
    }

    public LiveData<String> getConnectionStatus() {
        return connectionStatus;
    }

    public LiveData<Long> getSessionStarted() {
        return sessionStarted;
    }

    public LiveData<Long> getSessionEnded() {
        return sessionEnded;
    }

    public LiveData<Integer> getActiveSessionId() {
        return activeSessionId;
    }

    // ======================== Подключение к серверу ========================

    /**
     * Подключиться к серверу через WebSocket
     */
    public void connectToServer() {
        useWebSocket = true;
        socketManager.connect();
    }

    /**
     * Отключиться от сервера
     */
    public void disconnectFromServer() {
        useWebSocket = false;
        socketManager.disconnect();
        stopAutoUpdate();
        isConnected.postValue(false);
        connectionStatus.postValue("Отключено");
    }

    /**
     * Проверить связь с сервером (HTTP ping)
     */
    public void pingServer(PingCallback callback) {
        new Thread(() -> {
            boolean result = repo.pingServer();
            handler.post(() -> callback.onPingResult(result));
        }).start();
    }

    public interface PingCallback {
        void onPingResult(boolean success);
    }

    // ======================== Polling (fallback без WebSocket) ========================

    public void startAutoUpdate(int intervalMs) {
        if (isUpdating) return;
        // Если WebSocket активен — polling не нужен
        if (useWebSocket && socketManager.isConnected()) return;

        isUpdating = true;

        updateRunnable = new Runnable() {
            @Override
            public void run() {
                new Thread(() -> {
                    SensorData data = repo.fetchSensorData();
                    if (data != null) {
                        sensorData.postValue(data);
                        historyRepository.insert(data);
                    }
                }).start();

                if (isUpdating) handler.postDelayed(this, intervalMs);
            }
        };

        handler.post(updateRunnable);
    }

    public void stopAutoUpdate() {
        isUpdating = false;
        if (updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
            updateRunnable = null;
        }
    }

    // ======================== Загрузка истории с сервера ========================

    /**
     * Загрузить последние 20 значений текущей сессии с сервера
     */
    public void loadCurrentSessionFromServer(HistoryLoadCallback callback) {
        Integer sessionId = activeSessionId.getValue();
        if (sessionId != null && sessionId > 0) {
            loadSessionDataFromServer(sessionId, callback);
            return;
        }

        if (useWebSocket && socketManager.isConnected()) {
            socketManager.requestHistory(history -> {
                // Сохраняем в локальную БД
                for (SensorData data : history) {
                    historyRepository.insert(data);
                }
                if (callback != null) {
                    callback.onHistoryLoaded(history);
                }
            });
        } else {
            // Fallback на HTTP
            new Thread(() -> {
                List<SensorData> history = repo.fetchHistory();
                for (SensorData data : history) {
                    historyRepository.insert(data);
                }
                handler.post(() -> {
                    if (callback != null) {
                        callback.onHistoryLoaded(history);
                    }
                });
            }).start();
        }
    }

    public void loadHistoryFromServer(HistoryLoadCallback callback) {
        // Пробуем через WebSocket
        if (useWebSocket && socketManager.isConnected()) {
            socketManager.requestHistory(history -> {
                // Сохраняем в локальную БД
                for (SensorData data : history) {
                    historyRepository.insert(data);
                }
                if (callback != null) {
                    callback.onHistoryLoaded(history);
                }
            });
        } else {
            // Fallback на HTTP
            new Thread(() -> {
                List<SensorData> history = repo.fetchHistory();
                for (SensorData data : history) {
                    historyRepository.insert(data);
                }
                handler.post(() -> {
                    if (callback != null) {
                        callback.onHistoryLoaded(history);
                    }
                });
            }).start();
        }
    }

    public interface HistoryLoadCallback {
        void onHistoryLoaded(List<SensorData> history);
    }

    public void loadSessionsFromServer(SessionsLoadCallback callback) {
        new Thread(() -> {
            List<SessionItem> sessions = repo.fetchSessions();
            handler.post(() -> {
                if (callback != null) callback.onSessionsLoaded(sessions);
            });
        }).start();
    }

    public void loadSessionDataFromServer(int sessionId, HistoryLoadCallback callback) {
        new Thread(() -> {
            List<SensorData> history = repo.fetchSessionData(sessionId);
            handler.post(() -> {
                if (callback != null) callback.onHistoryLoaded(history);
            });
        }).start();
    }

    public interface SessionsLoadCallback {
        void onSessionsLoaded(List<SessionItem> sessions);
    }

    // ======================== Очистка истории при смене сессии ========================

    /**
     * Очистить локальную историю (при обнаружении новой сессии)
     */
    public void clearLocalHistory() {
        historyRepository.clearAll();
    }

    // ======================== Отправка настроек ========================

    /**
     * Отправить настройки на сервер (WebSocket приоритет)
     */
    public void sendSettings(int maxTemp, int minHum, int minLight, SettingsSendCallback callback) {
        if (socketManager.isConnected()) {
            socketManager.sendSettings(maxTemp, minHum, minLight);
            // Сохраняем локально сразу — сервер пришлёт подтверждение через settings_update
            saveSettings(getApplication(), maxTemp, minHum, minLight);
            callback.onSettingsSent(true);
        } else {
            // Сервер недоступен — не пытаемся отправить, возвращаем ошибку
            handler.post(() -> callback.onSettingsSent(false));
        }
    }

    public interface SettingsSendCallback {
        void onSettingsSent(boolean success);
    }

    // ======================== Получение последних значений ========================

    public List<SensorData> getLastValues(int count) {
        return historyRepository.getLastN(count);
    }

    // ======================== Утилиты ========================

    public static void showToast(Context context, String message) {
        if (currentToast != null) {
            currentToast.cancel();
        }
        currentToast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        currentToast.show();
    }

    public void clearFocusOnKeyboardClose(Context context, View rootView, EditText editText) {
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            int screenHeight = rootView.getRootView().getHeight();
            int visibleHeight = rootView.getHeight();
            int heightDiff = screenHeight - visibleHeight;
            boolean keyboardOpen = heightDiff > screenHeight * 0.25;
            if (!keyboardOpen && editText.isFocused()) {
                editText.clearFocus();
            }
        });
    }

    // ======================== Тестовые данные ========================

    private final Handler testHandler = new Handler(Looper.getMainLooper());
    private Runnable testRunnable;
    private boolean isTestRunning = false;
    private float lastTemp = 25f;
    private float lastHum = 50f;
    private float lastLight = 50f;

    public void startTestData() {
        if (isTestRunning) return;
        isTestRunning = true;

        testRunnable = new Runnable() {
            @Override
            public void run() {
                lastTemp = clamp(lastTemp + (float)(Math.random() * 4 - 2), 20f, 30f);
                lastHum = clamp(lastHum + (float)(Math.random() * 4 - 2), 30f, 70f);
                lastLight = clamp(lastLight + (float)(Math.random() * 4 - 2), 10f, 100f);

                SensorData testData = new SensorData(lastTemp, lastHum, lastLight);
                sensorData.postValue(testData);

                if (isTestRunning) {
                    testHandler.postDelayed(this, 2000);
                }
            }
        };

        testHandler.post(testRunnable);
    }

    public void stopTestData() {
        isTestRunning = false;
        if (testRunnable != null) {
            testHandler.removeCallbacks(testRunnable);
            testRunnable = null;
        }
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        socketManager.disconnect();
        stopAutoUpdate();
        stopTestData();
    }
}
