package com.example.test.ui;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.test.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private LinearLayout[] tabs;
    private ImageView[] icons;
    private TextView[] texts;
    private int activeIndex = 0;

    private int colorActive;
    private int colorInactive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.viewPager, (v, insets) -> {
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(0, 0, 0, imeInsets.bottom);
            return insets;
        });
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Подключаем адаптер ViewPager
        binding.viewPager.setAdapter(new ViewPagerAdapter(this));

        // Цвета для активного и неактивного состояния
        colorActive = ContextCompat.getColor(this, android.R.color.holo_orange_dark);
        colorInactive = 0xFF666666; // серый

        // Иконки и тексты вкладок
        icons = new ImageView[]{
                binding.iconConnection,
                binding.iconStatistic,
                binding.iconSettings
        };

        texts = new TextView[]{
                binding.textConnection,
                binding.textStatistic,
                binding.textSettings
        };

        tabs = new LinearLayout[]{
                binding.tabConnection,
                binding.tabStatistic,
                binding.tabSettings
        };

        // Устанавливаем цвет активной вкладки по умолчанию
        icons[0].setColorFilter(colorActive);
        texts[0].setTextColor(colorActive);
        activeIndex = 0;

        // Устанавливаем клики по вкладкам
        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            tabs[i].setOnClickListener(v -> {
                binding.viewPager.setCurrentItem(index, true);
                updateTabSelection(index);
            });
        }

        // Синхронизация с ViewPager (если пользователь свайпает)
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateTabSelection(position);
            }
        });

        // Инициализация стартовой вкладки
        updateTabSelection(0);
    }

    // Плавное изменение цвета иконок и текста
    private void updateTabSelection(int index) {
        if (index == activeIndex) return;

        animateColorChange(icons[activeIndex], texts[activeIndex], colorActive, colorInactive);
        animateColorChange(icons[index], texts[index], colorInactive, colorActive);

        activeIndex = index;
    }

    // Анимация плавного перехода между цветами
    private void animateColorChange(ImageView icon, TextView text, int fromColor, int toColor) {
        ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), fromColor, toColor);
        animator.setDuration(250);
        animator.addUpdateListener(animation -> {
            int color = (int) animation.getAnimatedValue();
            icon.setColorFilter(color);
            text.setTextColor(color);
        });
        animator.start();
    }
}
