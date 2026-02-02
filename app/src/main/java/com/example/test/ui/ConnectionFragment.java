package com.example.test.ui;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.test.viewmodel.SharedViewModel;
import com.example.test.databinding.FragmentConnectionBinding;
import com.bumptech.glide.Glide;
import com.example.test.R;


public class ConnectionFragment extends Fragment {
    private FragmentConnectionBinding binding;
    private SharedViewModel viewModel;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentConnectionBinding.inflate(inflater, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        Glide.with(this)
                .asGif()
                .load(R.drawable.connection)
                .placeholder(R.drawable.connection_static)
                .into(binding.imgConnection);


        viewModel.clearFocusOnKeyboardClose(requireContext(), binding.getRoot(), binding.edIp);

        binding.getRoot().getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            int screenHeight = binding.getRoot().getRootView().getHeight();
            int visibleHeight = binding.getRoot().getHeight();
            int heightDiff = screenHeight - visibleHeight;

            boolean keyboardOpen = heightDiff > screenHeight * 0.25;

            binding.imgConnection.setVisibility(keyboardOpen ? View.GONE : View.VISIBLE);
        });

        onClickSaveIp();
        getIp();
        onClickRefreshIp();

        return binding.getRoot();
    }

    // Функция вставки ip, если таковой сохранён
    private void getIp() {
        String ip = viewModel.getSavedIp(requireContext());
        if (!ip.isEmpty()) {
            binding.edIp.setText(ip);
        }
    }

    // Вставка ip при нажатии на кнопку
    private void onClickRefreshIp() {
        binding.bResetConnection.setOnClickListener(v -> {
            String ip = viewModel.getSavedIp(requireContext());
            if (!ip.isEmpty()) {
                binding.edIp.setText(ip);
            } else {
                SharedViewModel.showToast(requireContext(), "Нет сохранённого IP");
            }
        });
    }
    // Функция проверки поля при нажатии на кнопку
    private void onClickSaveIp() {
        binding.bSave.setOnClickListener(v -> {
            String newIp = binding.edIp.getText().toString().trim();

            if (newIp.isEmpty()) {
                SharedViewModel.showToast(requireContext(), "Введите IP адрес");
                return;
            }

            if (!isValidIp(newIp)) {
                SharedViewModel.showToast(requireContext(), "Неверный формат IP");
                return;
            }

            viewModel.saveIp(requireContext(), newIp);
            SharedViewModel.showToast(requireContext(), "Адрес успешно сохранён");
        });
    }

    // Метод для проверки корректного IP при помощи регулярного выражения
    private boolean isValidIp(String ip) {
        String ipPattern = "^((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)$";

        // Проверка на соответствие выражению
        if (!ip.matches(ipPattern)) return false;

        // Проверка на отсутствие ведущих нулей
        String[] parts = ip.split("\\.");
        for (String part : parts) {
            if (part.length() > 1 && part.startsWith("0")) {
                return false; // ведущий ноль недопустим
            }
        }
        return true;
    }
}
