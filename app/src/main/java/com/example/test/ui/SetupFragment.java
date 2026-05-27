package com.example.test.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.test.databinding.FragmentSetupBinding;
import com.example.test.viewmodel.SharedViewModel;
import com.google.android.material.slider.Slider;

public class SetupFragment extends Fragment {
    private FragmentSetupBinding binding;
    private SharedViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSetupBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        viewModel.clearFocusOnKeyboardClose(requireContext(), binding.getRoot(), binding.edMaxTemp);
        viewModel.clearFocusOnKeyboardClose(requireContext(), binding.getRoot(), binding.edMinHum);
        viewModel.clearFocusOnKeyboardClose(requireContext(), binding.getRoot(), binding.edMinLight);

        observeSettingsUpdates();
        loadSavedSettings();
        setupSliders();
        setupApplyButton();
        setupResetButton();

        return binding.getRoot();
    }

    private void loadSavedSettings() {
        int maxTemp = viewModel.getMaxTemp(requireContext());
        int minHum = viewModel.getMinHum(requireContext());
        int minLight = viewModel.getMinLight(requireContext());

        updateSettingsFields(maxTemp, minHum, minLight);
    }

    private void observeSettingsUpdates() {
        viewModel.getMaxTempLive().observe(getViewLifecycleOwner(), value -> {
            if (value != null) {
                updateSettingsFields(value, viewModel.getCurrentMinHum(), viewModel.getCurrentMinLight());
            }
        });
        viewModel.getMinHumLive().observe(getViewLifecycleOwner(), value -> {
            if (value != null) {
                updateSettingsFields(viewModel.getCurrentMaxTemp(), value, viewModel.getCurrentMinLight());
            }
        });
        viewModel.getMinLightLive().observe(getViewLifecycleOwner(), value -> {
            if (value != null) {
                updateSettingsFields(viewModel.getCurrentMaxTemp(), viewModel.getCurrentMinHum(), value);
            }
        });
    }

    private void updateSettingsFields(int maxTemp, int minHum, int minLight) {
        maxTemp = Math.max(0, Math.min(maxTemp, 50));
        minHum = Math.max(0, Math.min(minHum, 100));
        minLight = Math.max(0, Math.min(minLight, 100));

        binding.edMaxTemp.setText(String.valueOf(maxTemp));
        binding.sliderMaxTemp.setValue(maxTemp);

        binding.edMinHum.setText(String.valueOf(minHum));
        binding.sliderMinHum.setValue(minHum);

        binding.edMinLight.setText(String.valueOf(minLight));
        binding.sliderMinLight.setValue(minLight);

        binding.tvUnsavedMaxTemp.setVisibility(View.GONE);
        binding.tvUnsavedMinHum.setVisibility(View.GONE);
        binding.tvUnsavedMinLight.setVisibility(View.GONE);
    }

    private void setupSliders() {
        linkSliderAndEditText(binding.sliderMaxTemp, binding.edMaxTemp, binding.tvUnsavedMaxTemp, viewModel.getMaxTemp(requireContext()));
        linkSliderAndEditText(binding.sliderMinHum, binding.edMinHum, binding.tvUnsavedMinHum, viewModel.getMinHum(requireContext()));
        linkSliderAndEditText(binding.sliderMinLight, binding.edMinLight, binding.tvUnsavedMinLight, viewModel.getMinLight(requireContext()));
    }

    private void linkSliderAndEditText(Slider slider, android.widget.EditText editText, View unsavedText, int savedValue) {

        slider.addOnChangeListener((s, value, fromUser) -> {
            if (fromUser) {
                int intValue = (int) value;
                editText.setText(String.valueOf(intValue));
                unsavedText.setVisibility(intValue != savedValue ? View.VISIBLE : View.GONE);
            }
        });

        editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().isEmpty()) return;

                try {
                    int val = Integer.parseInt(s.toString());
                    int min = (int) slider.getValueFrom();
                    int max = (int) slider.getValueTo();

                    if (val < min) val = min;
                    if (val > max) val = max;

                    if (!s.toString().equals(String.valueOf(val))) {
                        editText.setText(String.valueOf(val));
                        editText.setSelection(editText.getText().length());
                    }

                    slider.setValue(val);
                    unsavedText.setVisibility(val != savedValue ? View.VISIBLE : View.GONE);

                } catch (NumberFormatException ignored) {}
            }
        });
    }

    // Кнопка "Применить" — отправка настроек на сервер
    private void setupApplyButton() {
        binding.bApply.setOnClickListener(v -> {
            String maxTempStr = binding.edMaxTemp.getText().toString();
            String minHumStr = binding.edMinHum.getText().toString();
            String minLightStr = binding.edMinLight.getText().toString();

            if (maxTempStr.isEmpty() || minHumStr.isEmpty() || minLightStr.isEmpty()) {
                SharedViewModel.showToast(requireContext(), "Заполните все поля");
                return;
            }

            int maxTemp = Integer.parseInt(maxTempStr);
            int minHum = Integer.parseInt(minHumStr);
            int minLight = Integer.parseInt(minLightStr);

            // Отправляем настройки через ViewModel (WebSocket приоритет)
            viewModel.sendSettings(maxTemp, minHum, minLight, success -> {
                requireActivity().runOnUiThread(() -> {
                    if (success) {
                        SharedViewModel.showToast(requireContext(), "Настройки обновлены");

                        binding.tvUnsavedMaxTemp.setVisibility(View.GONE);
                        binding.tvUnsavedMinHum.setVisibility(View.GONE);
                        binding.tvUnsavedMinLight.setVisibility(View.GONE);
                    } else {
                        SharedViewModel.showToast(requireContext(), "Нет подключения к серверу");
                    }
                });
            });
        });
    }

    private void setupResetButton() {
        binding.bResetSetup.setOnClickListener(v -> {
            loadSavedSettings();
            binding.tvUnsavedMaxTemp.setVisibility(View.GONE);
            binding.tvUnsavedMinHum.setVisibility(View.GONE);
            binding.tvUnsavedMinLight.setVisibility(View.GONE);
        });
    }
}
