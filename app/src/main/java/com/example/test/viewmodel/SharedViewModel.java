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
import com.example.test.network.MicrocontrollerRepository;
import com.example.test.repository.SensorHistoryRepository;

public class SharedViewModel extends AndroidViewModel {

    private static final String PREF_NAME = "MyPref";
    private static final String KEY_IP = "ip";
    private static final String KEY_MAX_TEMP = "max_temp";
    private static final String KEY_MIN_HUM = "min_hum";
    private static final String KEY_MIN_LIGHT = "min_light";

    private final MicrocontrollerRepository repo = new MicrocontrollerRepository();
    private final SensorHistoryRepository historyRepository;

    private final MutableLiveData<SensorData> sensorData = new MutableLiveData<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable;
    private boolean isUpdating = false;
    private static Toast currentToast;

    public SharedViewModel(@NonNull Application application) {
        super(application);
        historyRepository = new SensorHistoryRepository(application);
    }

    // Сохранение IP
    public void saveIp(Context context, String ip) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        pref.edit().putString(KEY_IP, ip).apply();
    }

    // Получение IP
    public String getSavedIp(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return pref.getString(KEY_IP, "");
    }

    // Сохранение настроек
    public void saveSettings(Context context, int maxTemp, int minHum, int minLight) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        pref.edit()
                .putInt(KEY_MAX_TEMP, maxTemp)
                .putInt(KEY_MIN_HUM, minHum)
                .putInt(KEY_MIN_LIGHT, minLight)
                .apply();
    }

    // Получение сохранённых настроек
    public int getMaxTemp(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return pref.getInt(KEY_MAX_TEMP, 25); // 25 — значение по умолчанию
    }

    public int getMinHum(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return pref.getInt(KEY_MIN_HUM, 40); // 40% — значение по умолчанию
    }

    public int getMinLight(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return pref.getInt(KEY_MIN_LIGHT, 30); // 30 — значение по умолчанию
    }

    // Получение LiveData (для наблюдения из StatisticFragment)
    public LiveData<SensorData> getSensorData() {
        return sensorData;
    }

    // Запуск автоматического обновления каждые intervalMs миллисекунд
    public void startAutoUpdate(String ip, int intervalMs) {
        if (isUpdating) return;
        isUpdating = true;

        updateRunnable = new Runnable() {
            @Override
            public void run() {
                new Thread(() -> {
                    SensorData data = repo.fetchSensorData(ip);
                    if (data != null) {
                        sensorData.postValue(data);

                        // NEW: сохраняем данные в SQLite
                        historyRepository.insert(data);
                    }
                }).start();

                // Планируем следующий запуск, если обновление всё ещё активно
                if (isUpdating) handler.postDelayed(this, intervalMs);
            }
        };

        handler.post(updateRunnable);
    }

    // Остановка автообновления
    public void stopAutoUpdate() {
        isUpdating = false;
        if (updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
            updateRunnable = null;
        }
    }

    // Типа клонирование метода из MicrocontrollerRepository
    public String sendSettings(String ip, int maxTemp, int minHum, int minLight) {
        return repo.sendSettings(ip, maxTemp, minHum, minLight);
    }

    // Отображение Toast с прерыванием предыдущего
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

            if (!keyboardOpen) {
                if (editText.isFocused()) {
                    editText.clearFocus();
                }
            }
        });
    }

    // Получение последних N значений из истории
    public java.util.List<SensorData> getLastValues(int count) {
        return historyRepository.getLastN(count);
    }

    // Тестирование графиков
    private final Handler testHandler = new Handler(Looper.getMainLooper());
    private Runnable testRunnable;
    private boolean isTestRunning = false;

    // Запуск тестовой генерации данных
    private float lastTemp = 25f;
    private float lastHum = 50f;
    private float lastLight = 50f;

    public void startTestData() {
        if (isTestRunning) return; // предотвращаем повторный запуск
        isTestRunning = true;

        testRunnable = new Runnable() {
            @Override
            public void run() {
                // Генерация плавных данных
                lastTemp = clamp(lastTemp + (float)(Math.random() * 4 - 2), 20f, 30f);  // ±2
                lastHum = clamp(lastHum + (float)(Math.random() * 4 - 2), 30f, 70f);     // ±2
                lastLight = clamp(lastLight + (float)(Math.random() * 4 - 2), 10f, 100f);// ±2

                SensorData testData = new SensorData(lastTemp, lastHum, lastLight);

                // Обновление LiveData
                sensorData.postValue(testData);

                // Планируем следующий запуск через 2 сек
                if (isTestRunning) {
                    testHandler.postDelayed(this, 2000);
                }
            }
        };

        testHandler.post(testRunnable);
    }

    // Вспомогательная функция для ограничения диапазона
    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    // Остановка тестовой генерации данных
    public void stopTestData() {
        isTestRunning = false;
        if (testRunnable != null) {
            testHandler.removeCallbacks(testRunnable);
            testRunnable = null;
        }
    }

}
