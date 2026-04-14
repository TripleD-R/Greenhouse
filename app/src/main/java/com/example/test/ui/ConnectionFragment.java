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

        onClickConnect();
        observeConnectionStatus();

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Если уже было подключение — восстанавливаем статус
        Boolean connected = viewModel.getIsConnected().getValue();
        if (connected != null && connected) {
            binding.bConnect.setText("Отключиться");
            binding.bConnect.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFFF44336));
        }
    }

    private void observeConnectionStatus() {
        viewModel.getIsConnected().observe(getViewLifecycleOwner(), connected -> {
            if (connected) {
                binding.bConnect.setText("Отключиться");
                binding.bConnect.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFF44336));
            } else {
                binding.bConnect.setText("Подключиться");
                binding.bConnect.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFF4CAF50));
            }
        });

        viewModel.getConnectionStatus().observe(getViewLifecycleOwner(), status -> {
            binding.tvConnectionStatus.setText(status);
        });
    }

    // Кнопка подключения/отключения
    private void onClickConnect() {
        binding.bConnect.setOnClickListener(v -> {
            Boolean connected = viewModel.getIsConnected().getValue();
            if (connected != null && connected) {
                // Уже подключён — отключаемся
                viewModel.disconnectFromServer();
            } else {
                // Подключаемся
                viewModel.connectToServer();
                binding.tvConnectionStatus.setText("Подключение...");
            }
        });
    }
}
