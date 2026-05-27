package com.example.test.ui;

import com.example.test.model.SensorData;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.test.databinding.FragmentStatisticBinding;
import androidx.appcompat.app.AlertDialog;

import com.example.test.model.SessionItem;
import com.example.test.viewmodel.SharedViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StatisticFragment extends Fragment {

    private FragmentStatisticBinding binding;
    private SharedViewModel viewModel;

    private LineDataSet tempDataSet;
    private LineData tempLineData;

    private LineDataSet humDataSet;
    private LineData humLineData;

    private LineDataSet lightDataSet;
    private LineData lightLineData;

    private int maxPoints = 20;
    private int currentMaxPoints = 20;  // Текущий лимит точек (меняется в зависимости от режима)
    private boolean isHistoryMode = false;  // Флаг: режим истории (без движения графика)

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentStatisticBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // Настройка графиков
        setupTempChart();
        setupHumChart();
        setupLightChart();

        // Загрузка последних значений из локальной БД (кэш)
        loadLocalHistory();

        // Настройка кнопки списка сессий
        setupSessionControls();

        // Загрузка истории с сервера (актуальные данные текущей сессии)
        loadServerHistory();

        // Наблюдение за поступающими данными (WebSocket или polling)
        viewModel.getSensorData().observe(getViewLifecycleOwner(), data -> {
            if (data != null) {
                // Обновление текстовых значений
                // Обновляем текстовые значения всегда
                binding.tvTemp.setText(String.format("%.1f °C", data.getTemperature()));
                binding.tvHum.setText(String.format("%.1f %%", data.getHumidity()));
                binding.tvLight.setText(String.format("%.1f %%", data.getLight()));

                // В режиме истории не добавляем живые точки (чтобы не мешать просмотру выбранной сессии)
                if (!isHistoryMode) {
                    addTempPoint(data.getTemperature());
                    addHumPoint(data.getHumidity());
                    addLightPoint(data.getLight());
                }
            }
        });

        viewModel.getSessionStarted().observe(getViewLifecycleOwner(), timestamp -> {
            if (timestamp != null) {
                loadCurrentSession();
            }
        });

        viewModel.getSessionEnded().observe(getViewLifecycleOwner(), timestamp -> {
            if (timestamp != null) {
                binding.tvSessionInfo.setText("Сессия завершена");
            }
        });

        return binding.getRoot();
    }

    /**
     * Загрузить локальный кэш (быстрое отображение при старте)
     */
    private void loadLocalHistory() {
        List<SensorData> lastValues = viewModel.getLastValues(maxPoints);
        for (SensorData data : lastValues) {
            addTempPoint(data.getTemperature());
            addHumPoint(data.getHumidity());
            addLightPoint(data.getLight());
        }
    }

    /**
     * Загрузить актуальные данные текущей сессии с сервера.
     * При этом очищаем локальную историю, чтобы не смешивать
     * данные прошлой и текущей сессии.
     */
    private void loadServerHistory() {
        loadCurrentSession();
    }

    private void setupSessionControls() {
        binding.btnSessionHistory.setOnClickListener(v -> loadSessionsFromServer());
        binding.btnCurrentSession.setOnClickListener(v -> loadCurrentSession());
    }

    private void loadCurrentSession() {
        isHistoryMode = false;
        currentMaxPoints = maxPoints;
        binding.tvSessionInfo.setText("Текущая сессия");
        clearCharts();

        viewModel.loadCurrentSessionFromServer(history -> requireActivity().runOnUiThread(() -> {
            clearCharts();
            for (SensorData data : history) {
                addTempPoint(data.getTemperature());
                addHumPoint(data.getHumidity());
                addLightPoint(data.getLight());
            }
            if (!history.isEmpty()) {
                SensorData last = history.get(history.size() - 1);
                binding.tvTemp.setText(String.format("%.1f °C", last.getTemperature()));
                binding.tvHum.setText(String.format("%.1f %%", last.getHumidity()));
                binding.tvLight.setText(String.format("%.1f %%", last.getLight()));
            }
        }));
    }

    private void loadSessionsFromServer() {
        viewModel.loadSessionsFromServer(sessions -> requireActivity().runOnUiThread(() -> {
            if (sessions.isEmpty()) {
                binding.tvSessionInfo.setText("История сессий отсутствует");
                return;
            }
            showSessionsDialog(sessions);
        }));
    }

    private void showSessionsDialog(List<SessionItem> sessions) {
        List<String> titles = new ArrayList<>();

        for (SessionItem session : sessions) {
            String startStr = formatSessionTime(session.getStartTime());
            String endStr = session.getEndTime().isEmpty() ? "В процессе" : formatSessionTime(session.getEndTime());
            titles.add(String.format("Сессия %d: %s",
                    session.getId(), startStr));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Выберите сессию");
        builder.setItems(titles.toArray(new String[0]), (dialog, which) -> {
            SessionItem session = sessions.get(which);
            String startStr = formatSessionTime(session.getStartTime());
            String endStr = session.getEndTime().isEmpty() ? "В процессе" : formatSessionTime(session.getEndTime());
            String title = String.format("Сессия %d: %s → %s", session.getId(), startStr, endStr);
            loadSessionDataById(session.getId(), title);
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private String formatSessionTime(String isoDateTime) {
        try {
            // Входной формат: "2026-05-27T11:31:45.123Z" или похожий
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = isoFormat.parse(isoDateTime.split("\\.")[0]); // Убираем миллисекунды
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            return isoDateTime; // Возвращаем оригинальное значение если ошибка парсинга
        }
    }

    private void loadSessionDataById(int sessionId, String sessionInfo) {
        isHistoryMode = true;  // Включаем режим истории (без движения графика)
        currentMaxPoints = 10000;  // Большое число для отображения всех точек сессии
        viewModel.loadSessionDataFromServer(sessionId, history -> requireActivity().runOnUiThread(() -> {
            clearCharts();
            for (SensorData data : history) {
                addTempPointNoLimit(data.getTemperature());
                addHumPointNoLimit(data.getHumidity());
                addLightPointNoLimit(data.getLight());
            }
            if (!history.isEmpty()) {
                SensorData last = history.get(history.size() - 1);
                binding.tvTemp.setText(String.format("%.1f °C", last.getTemperature()));
                binding.tvHum.setText(String.format("%.1f %%", last.getHumidity()));
                binding.tvLight.setText(String.format("%.1f %%", last.getLight()));
            }
            binding.tvSessionInfo.setText(sessionInfo);
        }));
    }

    // График температуры
    private void setupTempChart() {
        LineChart chart = binding.chartTemp;

        tempDataSet = new LineDataSet(null, "Temperature");
        tempDataSet.setDrawCircles(false);
        tempDataSet.setDrawValues(false);
        tempDataSet.setLineWidth(2f);
        tempDataSet.setColor(Color.parseColor("#FF5722"));

        tempLineData = new LineData(tempDataSet);
        chart.setData(tempLineData);

        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);

        XAxis x = chart.getXAxis();
        x.setDrawLabels(false);
        x.setDrawGridLines(false);

        YAxis y = chart.getAxisLeft();
        y.setDrawGridLines(false);

        chart.getAxisRight().setEnabled(false);
    }

    private void addTempPoint(float value) {
        tempDataSet.addEntry(new Entry(tempDataSet.getEntryCount(), value));

        if (tempDataSet.getEntryCount() > currentMaxPoints) {
            tempDataSet.removeFirst();
            reindex(tempDataSet);
        }

        tempLineData.notifyDataChanged();
        binding.chartTemp.notifyDataSetChanged();
        binding.chartTemp.invalidate();
        binding.chartTemp.setVisibleXRangeMaximum(maxPoints);
        if (!isHistoryMode) {
            binding.chartTemp.moveViewToX(tempDataSet.getEntryCount());
        }
    }

    private void addTempPointNoLimit(float value) {
        tempDataSet.addEntry(new Entry(tempDataSet.getEntryCount(), value));
        tempLineData.notifyDataChanged();
        binding.chartTemp.notifyDataSetChanged();
        binding.chartTemp.invalidate();
    }

    // График влажности
    private void setupHumChart() {
        LineChart chart = binding.chartHum;

        humDataSet = new LineDataSet(null, "Humidity");
        humDataSet.setDrawCircles(false);
        humDataSet.setDrawValues(false);
        humDataSet.setLineWidth(2f);
        humDataSet.setColor(Color.parseColor("#2196F3"));

        humLineData = new LineData(humDataSet);
        chart.setData(humLineData);

        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);

        XAxis x = chart.getXAxis();
        x.setDrawLabels(false);
        x.setDrawGridLines(false);

        YAxis y = chart.getAxisLeft();
        y.setDrawGridLines(false);

        chart.getAxisRight().setEnabled(false);
    }

    private void addHumPoint(float value) {
        humDataSet.addEntry(new Entry(humDataSet.getEntryCount(), value));

        if (humDataSet.getEntryCount() > currentMaxPoints) {
            humDataSet.removeFirst();
            reindex(humDataSet);
        }

        humLineData.notifyDataChanged();
        binding.chartHum.notifyDataSetChanged();
        binding.chartHum.invalidate();
        binding.chartHum.setVisibleXRangeMaximum(maxPoints);
        if (!isHistoryMode) {
            binding.chartHum.moveViewToX(humDataSet.getEntryCount());
        }
    }

    private void addHumPointNoLimit(float value) {
        humDataSet.addEntry(new Entry(humDataSet.getEntryCount(), value));
        humLineData.notifyDataChanged();
        binding.chartHum.notifyDataSetChanged();
        binding.chartHum.invalidate();
    }

    // График освещённости
    private void setupLightChart() {
        LineChart chart = binding.chartLight;

        lightDataSet = new LineDataSet(null, "Light");
        lightDataSet.setDrawCircles(false);
        lightDataSet.setDrawValues(false);
        lightDataSet.setLineWidth(2f);
        lightDataSet.setColor(Color.parseColor("#DBCB16"));

        lightLineData = new LineData(lightDataSet);
        chart.setData(lightLineData);

        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);

        XAxis x = chart.getXAxis();
        x.setDrawLabels(false);
        x.setDrawGridLines(false);

        YAxis y = chart.getAxisLeft();
        y.setDrawGridLines(false);

        chart.getAxisRight().setEnabled(false);
    }

    private void addLightPoint(float value) {
        lightDataSet.addEntry(new Entry(lightDataSet.getEntryCount(), value));

        if (lightDataSet.getEntryCount() > currentMaxPoints) {
            lightDataSet.removeFirst();
            reindex(lightDataSet);
        }

        lightLineData.notifyDataChanged();
        binding.chartLight.notifyDataSetChanged();
        binding.chartLight.invalidate();
        binding.chartLight.setVisibleXRangeMaximum(maxPoints);
        if (!isHistoryMode) {
            binding.chartLight.moveViewToX(lightDataSet.getEntryCount());
        }
    }

    private void addLightPointNoLimit(float value) {
        lightDataSet.addEntry(new Entry(lightDataSet.getEntryCount(), value));
        lightLineData.notifyDataChanged();
        binding.chartLight.notifyDataSetChanged();
        binding.chartLight.invalidate();
    }

    private void reindex(LineDataSet set) {
        for (int i = 0; i < set.getEntryCount(); i++) {
            set.getEntryForIndex(i).setX(i);
        }
    }

    /**
     * Очистить все графики
     */
    private void clearCharts() {
        if (tempDataSet != null) {
            tempDataSet.clear();
            tempLineData = new LineData(tempDataSet);
            binding.chartTemp.setData(tempLineData);
            binding.chartTemp.notifyDataSetChanged();
            binding.chartTemp.invalidate();
        }
        if (humDataSet != null) {
            humDataSet.clear();
            humLineData = new LineData(humDataSet);
            binding.chartHum.setData(humLineData);
            binding.chartHum.notifyDataSetChanged();
            binding.chartHum.invalidate();
        }
        if (lightDataSet != null) {
            lightDataSet.clear();
            lightLineData = new LineData(lightDataSet);
            binding.chartLight.setData(lightLineData);
            binding.chartLight.notifyDataSetChanged();
            binding.chartLight.invalidate();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Запускаем polling только если WebSocket не активен
        viewModel.startAutoUpdate(2000);
    }

    @Override
    public void onPause() {
        super.onPause();
        viewModel.stopAutoUpdate();
    }
}
