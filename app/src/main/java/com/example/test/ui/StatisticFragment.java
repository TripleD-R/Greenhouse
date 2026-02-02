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

public class StatisticFragment extends Fragment {

    private FragmentStatisticBinding binding;
    private SharedViewModel viewModel;

    private LineDataSet tempDataSet;
    private LineData tempLineData;

    private LineDataSet humDataSet;
    private LineData humLineData;

    private LineDataSet lightDataSet;
    private LineData lightLineData;

    private int maxPoints = 20; // Отображение последних N значений

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

        // Загрузка последних значений
        java.util.List<SensorData> lastValues = viewModel.getLastValues(maxPoints);
        for (SensorData data : lastValues) {
            addTempPoint(data.getTemperature());
            addHumPoint(data.getHumidity());
            addLightPoint(data.getLight());
        }

        // Наблюдение за поступающими данными
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

    // График температуры
    private void setupTempChart() {
        LineChart chart = binding.chartTemp;

        // Создание набора данных для температуры
        tempDataSet = new LineDataSet(null, "Temperature");
        tempDataSet.setDrawCircles(false);   // Отключение точек
        tempDataSet.setDrawValues(false);    // Отключение подписей
        tempDataSet.setLineWidth(2f);        // Толщина линии

        // Цвет красный
        tempDataSet.setColor(Color.parseColor("#FF5722"));

        // Подключение набора данных к графику
        tempLineData = new LineData(tempDataSet);
        chart.setData(tempLineData);

        chart.getDescription().setEnabled(false); // Отключение надписи Description
        chart.getLegend().setEnabled(false);      // Отключение легенды

        // Настройка оси X
        XAxis x = chart.getXAxis();
        x.setDrawLabels(false);       // Отсутствие подписей
        x.setDrawGridLines(false);    // Отсутствие сетки

        // Настройка оси Y
        YAxis y = chart.getAxisLeft();
        y.setDrawGridLines(false);

        chart.getAxisRight().setEnabled(false); // Отключение правой оси
    }

    private void addTempPoint(float value) {
        LineChart chart = binding.chartTemp;

        // Добавление новой точки
        tempDataSet.addEntry(new Entry(tempDataSet.getEntryCount(), value));

        // Удаление первой точки, если их больше максимума
        if (tempDataSet.getEntryCount() > maxPoints) {
            tempDataSet.removeFirst();
            reindex(tempDataSet); // Сдвижение графика
        }

        // Обновление графика
        tempLineData.notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.setVisibleXRangeMaximum(maxPoints); // последние N точек
        chart.moveViewToX(tempDataSet.getEntryCount()); // автопрокрутка вправо
    }

    // График влажности
    private void setupHumChart() {
        LineChart chart = binding.chartHum;

        // Создание набора данных для влажности
        humDataSet = new LineDataSet(null, "Humidity");
        humDataSet.setDrawCircles(false);
        humDataSet.setDrawValues(false);
        humDataSet.setLineWidth(2f);

        // Цвет синий
        humDataSet.setColor(Color.parseColor("#2196F3"));

        // Подключение набора данных
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
        LineChart chart = binding.chartHum;

        // Добавление новой точки
        humDataSet.addEntry(new Entry(humDataSet.getEntryCount(), value));

        // Ограничение количества
        if (humDataSet.getEntryCount() > maxPoints) {
            humDataSet.removeFirst();
            reindex(humDataSet);
        }

        // Обновление графика
        humLineData.notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.setVisibleXRangeMaximum(maxPoints);
        chart.moveViewToX(humDataSet.getEntryCount());
    }

    // График освещённости
    private void setupLightChart() {
        LineChart chart = binding.chartLight;

        // Создание набора данных для освещённости
        lightDataSet = new LineDataSet(null, "Light");
        lightDataSet.setDrawCircles(false);
        lightDataSet.setDrawValues(false);
        lightDataSet.setLineWidth(2f);

        // Цвет жёлтый
        lightDataSet.setColor(Color.parseColor("#DBCB16"));

        // Подключение набора данных
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
        LineChart chart = binding.chartLight;

        // Добавление новой точки
        lightDataSet.addEntry(new Entry(lightDataSet.getEntryCount(), value));

        // Ограничение количества
        if (lightDataSet.getEntryCount() > maxPoints) {
            lightDataSet.removeFirst();
            reindex(lightDataSet);
        }

        // Обновление графика
        lightLineData.notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.setVisibleXRangeMaximum(maxPoints);
        chart.moveViewToX(lightDataSet.getEntryCount());
    }

    // Смещение графика
    private void reindex(LineDataSet set) {
        // После удаления первой точки нужно пересчитать X,
        // иначе график "прыгает" из-за разрывов в индексации
        for (int i = 0; i < set.getEntryCount(); i++) {
            set.getEntryForIndex(i).setX(i);
        }
    }

    // Запуск автообновления
    @Override
    public void onResume() {
        super.onResume();
//        viewModel.startTestData();
        String ip = viewModel.getSavedIp(requireContext());
        if (!ip.isEmpty()) {
            viewModel.startAutoUpdate(ip, 2000); // обновление каждые 2 сек
        }
    }

//    // Остановка автообновления
//    @Override
//    public void onPause() {
//        super.onPause();
//        viewModel.stopTestData();
//        viewModel.stopAutoUpdate();
//    }
}
