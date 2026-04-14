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
import com.example.test.viewmodel.SharedViewModel;

import java.util.List;

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

        // Загрузка истории с сервера (актуальные данные текущей сессии)
        loadServerHistory();

        // Наблюдение за поступающими данными (WebSocket или polling)
        viewModel.getSensorData().observe(getViewLifecycleOwner(), data -> {
            if (data != null) {
                // Обновление текстовых значений
                binding.tvTemp.setText(String.format("%.1f °C", data.getTemperature()));
                binding.tvHum.setText(String.format("%.1f %%", data.getHumidity()));
                binding.tvLight.setText(String.format("%.1f %%", data.getLight()));

                // Добавление точек в графики
                addTempPoint(data.getTemperature());
                addHumPoint(data.getHumidity());
                addLightPoint(data.getLight());
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
        viewModel.loadHistoryFromServer(history -> {
            // Очищаем графики и заполняем данными с сервера
            requireActivity().runOnUiThread(() -> {
                clearCharts();

                for (SensorData data : history) {
                    addTempPoint(data.getTemperature());
                    addHumPoint(data.getHumidity());
                    addLightPoint(data.getLight());
                }

                // Обновляем текстовые значения последним элементом
                if (!history.isEmpty()) {
                    SensorData last = history.get(history.size() - 1);
                    binding.tvTemp.setText(String.format("%.1f °C", last.getTemperature()));
                    binding.tvHum.setText(String.format("%.1f %%", last.getHumidity()));
                    binding.tvLight.setText(String.format("%.1f %%", last.getLight()));
                }
            });
        });
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

        if (tempDataSet.getEntryCount() > maxPoints) {
            tempDataSet.removeFirst();
            reindex(tempDataSet);
        }

        tempLineData.notifyDataChanged();
        binding.chartTemp.notifyDataSetChanged();
        binding.chartTemp.setVisibleXRangeMaximum(maxPoints);
        binding.chartTemp.moveViewToX(tempDataSet.getEntryCount());
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

        if (humDataSet.getEntryCount() > maxPoints) {
            humDataSet.removeFirst();
            reindex(humDataSet);
        }

        humLineData.notifyDataChanged();
        binding.chartHum.notifyDataSetChanged();
        binding.chartHum.setVisibleXRangeMaximum(maxPoints);
        binding.chartHum.moveViewToX(humDataSet.getEntryCount());
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

        if (lightDataSet.getEntryCount() > maxPoints) {
            lightDataSet.removeFirst();
            reindex(lightDataSet);
        }

        lightLineData.notifyDataChanged();
        binding.chartLight.notifyDataSetChanged();
        binding.chartLight.setVisibleXRangeMaximum(maxPoints);
        binding.chartLight.moveViewToX(lightDataSet.getEntryCount());
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
            tempLineData.notifyDataChanged();
            binding.chartTemp.notifyDataSetChanged();
        }
        if (humDataSet != null) {
            humDataSet.clear();
            humLineData.notifyDataChanged();
            binding.chartHum.notifyDataSetChanged();
        }
        if (lightDataSet != null) {
            lightDataSet.clear();
            lightLineData.notifyDataChanged();
            binding.chartLight.notifyDataSetChanged();
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
