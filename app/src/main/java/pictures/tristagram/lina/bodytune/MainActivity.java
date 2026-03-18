package pictures.tristagram.lina.bodytune;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends AppCompatActivity {

    private EditText etCurrentWeight;
    private EditText etTargetWeight;
    private LineChart lineChartWeight, lineChartChest, lineChartWaist, lineChartHips, lineChartBicep;
    private AppDatabase db;
    private TextView tvTrend;
    private TextView tvSelectedDate;
    private String currentGoalType = "";
    private TextView tvCurrentGoalStatus;
    private WeightAdapter adapter;
    private String selectedDate;


    private EditText etChest, etWaist, etHips, etBicep;
    private LinearLayout layoutMeasurementsFields;
    private ImageView ivExpandIcon;
    private EditText etHeight;
    private TextView tvBMIResult;
    ImageButton btnPrev, btnNext;
    private static final String TAG = "MainActivity";
       private TextWatcher weightTextWatcher;
    Typeface tf, sm;

    WeightEntry currentEntry;

    private static final int CREATE_FILE = 1;
    private static final int PICK_PDF_FILE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tf = ResourcesCompat.getFont(this, R.font.montserrat_regular);
        sm = ResourcesCompat.getFont(this, R.font.montserrat_semibold);
        db = AppDatabase.getInstance(this);
        etCurrentWeight = findViewById(R.id.etCurrentWeight);
        etTargetWeight = findViewById(R.id.etTargetWeight);
        Button btnSave = findViewById(R.id.btnSave);
        lineChartWeight = findViewById(R.id.lineChart);
        lineChartChest = findViewById(R.id.lineChartChest);
        lineChartWaist = findViewById(R.id.lineChartWaist);
        lineChartHips = findViewById(R.id.lineChartHips);
        lineChartBicep = findViewById(R.id.lineChartBicep);
        tvTrend = findViewById(R.id.tvTrend);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);


        tvCurrentGoalStatus = findViewById(R.id.tvCurrentGoalStatus);


        setupChart();

        tvSelectedDate.setOnClickListener(v -> showDatePicker());
        btnSave.setOnClickListener(v -> saveWeight());

        RecyclerView rvHistory = findViewById(R.id.rvHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WeightAdapter();
        rvHistory.setAdapter(adapter);
        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        tvSelectedDate.setText(selectedDate);
        loadWeightForSelectedDate();
        btnPrev = findViewById(R.id.btnPrevDate);
        btnPrev.setOnClickListener(v -> changeDate(-1));
        btnNext = findViewById(R.id.btnNextDate);
        btnNext.setOnClickListener(v -> changeDate(1));

        etChest = findViewById(R.id.etChest);
        etWaist = findViewById(R.id.etWaist);
        etHips = findViewById(R.id.etHips);
        etBicep = findViewById(R.id.etBicep);
        layoutMeasurementsFields = findViewById(R.id.containerMeasurementsFields);
        ivExpandIcon = findViewById(R.id.ivExpandIconMeasurements);


        etHeight = findViewById(R.id.etHeight);
        tvBMIResult = findViewById(R.id.tvBMIResult);

        weightTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                calculateBMI();
                Log.d(TAG, "changeDate called, TextWatcher" + ", selectedDate =" + selectedDate);
            }
        };


        etCurrentWeight.addTextChangedListener(weightTextWatcher);

        findViewById(R.id.headerMeasurements).setOnClickListener(v -> {
            if (layoutMeasurementsFields.getVisibility() == View.GONE) {
                layoutMeasurementsFields.setVisibility(View.VISIBLE);
                ivExpandIcon.setRotation(180);
            } else {
                layoutMeasurementsFields.setVisibility(View.GONE);
                ivExpandIcon.setRotation(0);
            }
        });

        String savedHeight = getSharedPreferences("Settings", MODE_PRIVATE).getString("user_height", "");
        etHeight.setText(savedHeight);
        updateChart();
        setupCollapsibleGraph(R.id.headerChest, findViewById(R.id.containerChest),
                findViewById(R.id.ivExpandChest), "show_chest");

        setupCollapsibleGraph(R.id.headerWaist, findViewById(R.id.containerWaist),
                findViewById(R.id.ivExpandWaist), "show_waist");

        setupCollapsibleGraph(R.id.headerHips, findViewById(R.id.containerHips),
                findViewById(R.id.ivExpandHips), "show_hips");

        setupCollapsibleGraph(R.id.headerBicep, findViewById(R.id.containerBicep),
                findViewById(R.id.ivExpandBicep), "show_bicep");

        setupCollapsibleGraph(R.id.headerMeasurements, findViewById(R.id.containerMeasurementsFields),
                findViewById(R.id.ivExpandIconMeasurements), "show_measurements");


        findViewById(R.id.btnExport).setOnClickListener(v -> createCsvFile());
        findViewById(R.id.btnImport).setOnClickListener(v -> openCsvFile());
    }


    private void setupCollapsibleGraph(int headerId, View container, ImageView icon, String prefKey) {
        // 1. Читаем настройки: по умолчанию true (развернуто)
        boolean isVisible = getSharedPreferences("GraphSettings", MODE_PRIVATE).getBoolean(prefKey, true);

        // 2. Устанавливаем начальное состояние
        container.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        icon.setRotation(isVisible ? 180 : 0);

        // 3. Вешаем слушатель на заголовок
        findViewById(headerId).setOnClickListener(v -> {
            boolean nowVisible = container.getVisibility() == View.VISIBLE;

            if (nowVisible) {
                container.setVisibility(View.GONE);
                icon.animate().rotation(0).setDuration(300).start();
            } else {
                container.setVisibility(View.VISIBLE);
                icon.animate().rotation(180).setDuration(300).start();
            }

            // Сохраняем состояние
            getSharedPreferences("GraphSettings", MODE_PRIVATE)
                    .edit()
                    .putBoolean(prefKey, !nowVisible)
                    .apply();
        });
    }

    private void applyUnifiedStyle(LineChart chart) {
        chart.setBackgroundColor(getColor(R.color.card));
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setExtraBottomOffset(30f); // Место для двухстрочных дат

        // Ось X
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(getColor(R.color.sub_text));
        xAxis.setTypeface(sm);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(getColor(R.color.sub_text));
        xAxis.setGranularity(1f);


        xAxis.setLabelCount(7, false);
        xAxis.setTextSize(12f);
        xAxis.setYOffset(10f); // Отступ текста от самой оси
        // Назначаем твой MultiLineXAxisRenderer
        chart.setXAxisRenderer(new MultiLineXAxisRenderer(
                chart.getViewPortHandler(),
                xAxis,
                chart.getTransformer(YAxis.AxisDependency.LEFT)));

        // Ось Y (левая)
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTypeface(tf);
        leftAxis.setTextColor(getColor(R.color.primary));
        leftAxis.setGridColor(getColor(R.color.sub_text));
        leftAxis.setDrawZeroLine(false);

        // Отключаем правую ось
        chart.getAxisRight().setEnabled(false);

    }


    private void buildMeasurementChart(List<WeightEntry> entries, String measurementName, LineChart chart, String selectedDate) {
        List<Entry> chartPoints = new ArrayList<>();
        SimpleDateFormat sdfUtc = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        sdfUtc.setTimeZone(TimeZone.getTimeZone("UTC"));

        for (WeightEntry entry : entries) {
            float value = 0;
            switch (measurementName) {
                case "chest":
                    value = entry.chest;
                    break;
                case "waist":
                    value = entry.waist;
                    break;
                case "hips":
                    value = entry.hips;
                    break;
                case "bicep":
                    value = entry.bicep;
                    break;
            }
            if (value > 0) {
                try {
                    Date date = sdfUtc.parse(entry.date);
                    if (date != null) {
                        float days = (float) TimeUnit.MILLISECONDS.toDays(date.getTime());
                        chartPoints.add(new Entry(days, value));
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (chartPoints.isEmpty()) {
            chart.clear();
            chart.setNoDataText(getString(R.string.no_data_for) + " "+ measurementName);
            chart.invalidate();
            return;
        }

        applyUnifiedStyle(chart);
        LineDataSet dataSet = new LineDataSet(chartPoints, measurementName);
       dataSet.setValueTextColor(getColor(R.color.primary));
        dataSet.setCircleColor(getColor(R.color.point_color));
        dataSet.setLineWidth(1f);
        dataSet.setCircleRadius(6f);
        dataSet.setDrawValues(true);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                // Форматируем: если целое — без десятых, иначе с одним знаком
                if (value == (int) value) {
                    return String.format(Locale.getDefault(), "%d см", (int) value);
                } else {
                    return String.format(Locale.getDefault(), "%.1f см", value);
                }
            }
        });
        dataSet.setMode(LineDataSet.Mode.LINEAR);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        chart.setVisibleXRangeMaximum(7f);   // Показываем примерно 7 дней
        // === Настройки, аналогичные основному графику ===
        chart.setDragEnabled(true);          // Включаем прокрутку
        chart.setScaleEnabled(false);        // Отключаем масштабирование (как в основном)
        chart.setPinchZoom(false);
        chart.notifyDataSetChanged();
        // Настройка осей (аналогично графику веса)
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat dayFormat = new SimpleDateFormat("dd.MM", Locale.getDefault());
            private final SimpleDateFormat weekDayFormat = new SimpleDateFormat("EEE", Locale.getDefault());

            @Override
            public String getFormattedValue(float value) {
                long millis = (long) value * 24 * 60 * 60 * 1000L;
                return dayFormat.format(new Date(millis)) + "\n" + weekDayFormat.format(new Date(millis));
            }
        });
        // Перемещаем к последней дате
            try {
                Date d = sdfUtc.parse(selectedDate);
                if (d != null) {
                    float selectedX = (float) TimeUnit.MILLISECONDS.toDays(d.getTime());
                    chart.moveViewToX(selectedX - 3f);
                }
            } catch (Exception ignored) {
            }

        chart.animateX(400);
        chart.invalidate();
    }

    private void calculateBMI() {
        String weightStr = etCurrentWeight.getText().toString();
        String heightStr = etHeight.getText().toString();

        if (!weightStr.isEmpty() && !heightStr.isEmpty()) {
            float weight = Float.parseFloat(weightStr);
            float heightCm = Float.parseFloat(heightStr);

            if (heightCm > 0) {
                float heightM = heightCm / 100; // Переводим см в метры
                float bmi = weight / (heightM * heightM);

                // Выводим результат и категорию
                tvBMIResult.setText(String.format(Locale.getDefault(), "ИМТ: %.1f \n(%s)", bmi, getBMICategory(bmi)));
            }
        } else {
            tvBMIResult.setText(getString(R.string.bmi_title));
        }

        // Сохранение
        getSharedPreferences("Settings", MODE_PRIVATE).edit().putString("user_height", heightStr).apply();

    }

    private String getBMICategory(float bmi) {
        if (bmi < 18.5) return getString(R.string.under_normal_weight);
        if (bmi < 25) return getString(R.string.normal_weight);
        if (bmi < 30) return getString(R.string.upper_normal_weight);
        return getString(R.string.obesity);
    }

    private void changeDate(int amount) {

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(selectedDate);
            if (date != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                cal.add(Calendar.DAY_OF_MONTH, amount); // Прибавляем или вычитаем дни

                selectedDate = sdf.format(cal.getTime());
                tvSelectedDate.setText(selectedDate);

                // Обновляем данные на экране
                loadWeightForSelectedDate();


                // Сдвигаем график к этой дате

                float days = (float) (cal.getTimeInMillis() / (1000 * 60 * 60 * 24));

                if (lineChartWeight.getData() != null && !lineChartWeight.getData().getDataSets().isEmpty()) {
                    lineChartWeight.moveViewToX(days - 3f);
                }

                if (lineChartChest.getData() != null) lineChartChest.moveViewToX(days - 3f);
                if (lineChartWaist.getData() != null) lineChartWaist.moveViewToX(days - 3f);
                if (lineChartHips.getData() != null) lineChartHips.moveViewToX(days - 3f);
                if (lineChartBicep.getData() != null) lineChartBicep.moveViewToX(days - 3f);
            }
        } catch (Exception ignored) {
        }
    }

    private void loadWeightForSelectedDate() {
        new Thread(() -> {
            currentEntry = db.weightDao().getEntryByDate(this.selectedDate);

            // Находим последнюю запись в базе вообще, чтобы узнать цель
            WeightEntry lastKnownEntry = db.weightDao().getLastAnyEntry();

            WeightEntry trendCurrent;
            WeightEntry trendPrevious;

            if (currentEntry != null) {
                // Если на этот день запись есть, сравниваем её с предыдущей
                trendCurrent = currentEntry;
                trendPrevious = db.weightDao().getLatestEntryBefore(this.selectedDate);
            } else {
                trendCurrent = lastKnownEntry;
                // Если на этот день записи НЕТ (пустой день),
                // берем самую последнюю запись из всей базы
                trendCurrent = db.weightDao().getLastAnyEntry();
                if (trendCurrent != null) {
                    // И сравниваем её с той, что была ПЕРЕД ней
                    trendPrevious = db.weightDao().getLatestEntryBefore(trendCurrent.date);
                } else {
                    trendPrevious = null;
                }
            }


            WeightEntry finalTrendCurrent = trendCurrent;
            runOnUiThread(() -> {
                updateUIWithLoadedData(lastKnownEntry); // Выносим обновление полей в чистый метод
                displayTrend(finalTrendCurrent, trendPrevious);
                updateChart();//
            });
        }).start();
    }

    private void displayTrend(WeightEntry current, WeightEntry previous) {
        if (current != null && previous != null) {
            float diff = current.weight - previous.weight;
            String dateInfo = (currentEntry == null) ? getString(R.string.last_data_text) + current.date + ")" : "";

            if (diff < 0) {
                tvTrend.setText(getString(R.string.trend_text_lose) + String.format(" (%.1f ", Math.abs(diff)) + getString(R.string.unit_kg)+")"+ dateInfo);
            } else if (diff > 0) {
                tvTrend.setText(getString(R.string.trend_text_gain) + String.format(" (+%.1f ", diff) + getString(R.string.unit_kg)+")"+ dateInfo);
            } else {
                tvTrend.setText(getString(R.string.trend_text_stable) + dateInfo);
            }
        } else {
            tvTrend.setText(getString(R.string.trend_text_start));
        }
    }
    private void updateUIWithLoadedData(WeightEntry lastKnownEntry) {
        etCurrentWeight.removeTextChangedListener(weightTextWatcher);

        if (currentEntry != null) {
            etCurrentWeight.setText(String.valueOf(currentEntry.weight));
            etTargetWeight.setText(String.valueOf(currentEntry.targetWeight));

        } else {
            etCurrentWeight.setText("");
            // Предзаполняем поле "Целевой вес" последним известным значением для удобства
            if (lastKnownEntry != null) {
                etTargetWeight.setText(String.valueOf(lastKnownEntry.targetWeight));
            }
        }

        // 2. Логика СТАТУСА ЦЕЛИ (берем либо из текущей, либо из последней известной)
        WeightEntry entryForGoal = (currentEntry != null) ? currentEntry : lastKnownEntry;

        if (entryForGoal != null) {
            float weightToCompare = (currentEntry != null) ? currentEntry.weight : entryForGoal.weight;
            float diffGoal = weightToCompare - entryForGoal.targetWeight;

            if (Math.abs(diffGoal) < 0.2f) {
                tvCurrentGoalStatus.setText(R.string.goal_keep);
            } else {
                tvCurrentGoalStatus.setText(diffGoal < 0 ? R.string.goal_gain : R.string.goal_lose);
            }
        } else {
            tvCurrentGoalStatus.setText(R.string.goal);
        }

        etCurrentWeight.addTextChangedListener(weightTextWatcher);
        calculateBMI();
    }


    private void showDatePicker() {
        Log.d(TAG, "showDatePicker called");
        long selectionMillis = MaterialDatePicker.todayInUtcMilliseconds(); // По умолчанию сегодня
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // Работаем в UTC для корректности календаря
            Date date = sdf.parse(selectedDate);
            if (date != null) {
                selectionMillis = date.getTime();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.date_picker_title))
                .setSelection(selectionMillis)
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
//            Log.d(TAG, "DatePicker positive, selection=" + selection);
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.setTimeInMillis(selection);
//            Log.d(TAG, "showDatePicker called before DatePicker + " + selectedDate + " + selectedDate");
            selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
            tvSelectedDate.setText(selectedDate);
//            Log.d(TAG, "showDatePicker called after DatePicker + " + selectedDate + " + selectedDate");
            loadWeightForSelectedDate();
//            Log.d(TAG, "showDatePicker called after DatePicker  loadWeightForSelectedDate(); + " + selectedDate + " + selectedDate");
            if (lineChartWeight.getData() != null && !lineChartWeight.getData().getDataSets().isEmpty()) {

                float days = (float) TimeUnit.MILLISECONDS.toDays(selection);
                // Смещаем на 3 дня влево, чтобы точка оказалась примерно в центре видимой области (7 дней)
                lineChartWeight.moveViewToX(days - 3f);
                if (lineChartChest.getData() != null) lineChartChest.moveViewToX(days - 3f);
                if (lineChartWaist.getData() != null) lineChartWaist.moveViewToX(days - 3f);
                if (lineChartHips.getData() != null) lineChartHips.moveViewToX(days - 3f);
                if (lineChartBicep.getData() != null) lineChartBicep.moveViewToX(days - 3f);

            }
        });
        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");

    }


    private void setupChart() {


// Применяем к осям
        lineChartWeight.getXAxis().setTypeface(tf);
        lineChartWeight.getAxisLeft().setTypeface(sm);


        lineChartWeight.setBackgroundColor(getColor(R.color.card));
        lineChartWeight.setGridBackgroundColor(getColor(R.color.card));


// Цвет осей и сетки
        lineChartWeight.getAxisLeft().setTextColor(getColor(R.color.primary));
        lineChartWeight.getAxisLeft().setGridColor(getColor(R.color.sub_text));
        lineChartWeight.getXAxis().setGridColor(getColor(R.color.sub_text));


        lineChartWeight.getDescription().setEnabled(false);
        lineChartWeight.getLegend().setEnabled(false);
        lineChartWeight.setTouchEnabled(true);
        lineChartWeight.setDragEnabled(true);
        lineChartWeight.setPinchZoom(false);
        lineChartWeight.setExtraBottomOffset(30f);

        XAxis xAxis = lineChartWeight.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(getColor(R.color.sub_text));
        xAxis.setTextSize(12f);
        xAxis.setYOffset(10f);
        xAxis.setGranularity(1f);
        xAxis.setTypeface(sm);
        xAxis.setGranularityEnabled(true);
        xAxis.setLabelCount(7, false);
        // Устанавливаем кастомный отрисовщик для поддержки нескольких строк
        lineChartWeight.setXAxisRenderer(new MultiLineXAxisRenderer(
                lineChartWeight.getViewPortHandler(),
                lineChartWeight.getXAxis(),
                lineChartWeight.getTransformer(YAxis.AxisDependency.LEFT)
        ));

        // Увеличиваем отступ, чтобы две строки не вылезали за пределы экрана
        lineChartWeight.setExtraBottomOffset(30f);

        xAxis.setLabelCount(7, false);
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat dayFormat = new SimpleDateFormat("dd.MM", Locale.getDefault());
            private final SimpleDateFormat weekDayFormat = new SimpleDateFormat("EEE", Locale.getDefault());

            @Override
            public String getFormattedValue(float value) {
                long millis = (long) value * 24 * 60 * 60 * 1000L;
                return dayFormat.format(new Date(millis)) + "\n" + weekDayFormat.format(new Date(millis));
            }
        });

        lineChartWeight.getAxisRight().setEnabled(false);
        lineChartWeight.setNoDataText(getString(R.string.start_text));
        lineChartWeight.setNestedScrollingEnabled(false);
        lineChartWeight.setExtraBottomOffset(30f);
        lineChartWeight.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                // e.getX() — это количество дней, которое мы сохраняли
                long millis = (long) e.getX() * 24 * 60 * 60 * 1000L;

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                try {
                    Date date = sdf.parse(selectedDate);
                } catch (ParseException ex) {
                    throw new RuntimeException(ex);
                }
                loadWeightForSelectedDate();
            }

            @Override
            public void onNothingSelected() {

            }
        });

    }


    private void saveWeight() {
        String weightStr = etCurrentWeight.getText().toString();
        String targetStr = etTargetWeight.getText().toString();

        if (weightStr.isEmpty()) {
            Toast.makeText(this, R.string.msg_enter_weight, Toast.LENGTH_SHORT).show();
            return;
        }

        float weight = Float.parseFloat(weightStr);
        float target = targetStr.isEmpty() ? weight : Float.parseFloat(targetStr);

        float chest = etChest.getText().toString().isEmpty() ? 0 : Float.parseFloat(etChest.getText().toString());
        float waist = etWaist.getText().toString().isEmpty() ? 0 : Float.parseFloat(etWaist.getText().toString());
        float hips = etHips.getText().toString().isEmpty() ? 0 : Float.parseFloat(etHips.getText().toString());
        float bicep = etBicep.getText().toString().isEmpty() ? 0 : Float.parseFloat(etBicep.getText().toString());

        new Thread(() -> {
// 1. Проверяем, есть ли уже записи в истории
            List<WeightEntry> allEntries = db.weightDao().getAllEntries();

            // 2. Если записей нет (это первая запись), определяем цель автоматически
            if (true) {
                float diff = weight - target;

                if (diff > 0.6f) {
                    currentGoalType = "lose";
                } else if (diff < -0.6f) {
                    currentGoalType = "gain";
                } else {
                    currentGoalType = "maintain";
                }
            }


            WeightEntry entry = new WeightEntry(selectedDate, weight);
            entry.targetWeight = target;
            entry.goalType = currentGoalType;
            entry.chest = chest;
            entry.waist = waist;
            entry.hips = hips;
            entry.bicep = bicep;

            db.weightDao().insert(entry);

            runOnUiThread(() -> {
                Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show();
                etCurrentWeight.setText(String.valueOf(weight));
                etTargetWeight.setText(String.valueOf(target));
                loadWeightForSelectedDate();
                updateChart();
            });
        }).start();
    }

    private void deleteEntry(String date) {
        new Thread(() -> {
            db.weightDao().deleteByDate(date);
            updateChart();
        }).start();
    }

    private void updateChart() {
        new Thread(() -> {
            List<WeightEntry> entries = db.weightDao().getAllEntries();
            List<Entry> chartPoints = new ArrayList<>();
            List<Integer> circleColors = new ArrayList<>();
            AtomicReference<Float> lastTarget = new AtomicReference<>((float) 0);
            float lastWeight = 0;

            float minX = 0;
            float maxX = 0;

            SimpleDateFormat sdfUtc = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            sdfUtc.setTimeZone(TimeZone.getTimeZone("UTC"));

            for (int i = 0; i < entries.size(); i++) {
                WeightEntry current = entries.get(i);
                lastWeight = current.weight;
                try {
                    Date date = sdfUtc.parse(current.date);
                    if (date != null) {

                        float days = (float) TimeUnit.MILLISECONDS.toDays(date.getTime());


                        chartPoints.add(new Entry(days, current.weight));
                        if (days < minX) minX = days;
                        if (days > maxX) maxX = days;
                    } else {

                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (current.targetWeight > 0) {
                    lastTarget.set(current.targetWeight);
                }

                int color = getColor(R.color.text_goal_color);
                if (i > 0) {
                    WeightEntry prev = entries.get(i - 1);
                    String goal = current.goalType != null ? current.goalType : "maintain";
                    if (goal.equals("lose"))
                        color = (current.weight < prev.weight) ?  getColor(R.color.text_goal_color_green) : getColor(R.color.text_goal_color_red);

                    else if (goal.equals("gain"))
                        color = (current.weight > prev.weight) ?  getColor(R.color.text_goal_color_green) : getColor(R.color.text_goal_color_red);
                    else if (goal.equals("maintain"))
                        color = (Math.abs(current.weight - lastTarget.get()) <= 0.4f) ?  getColor(R.color.text_goal_color_green) : getColor(R.color.text_goal_color_red);
                }
                circleColors.add(color);
            }

            // Определяем текст тренда

            int trendColor = getColor(R.color.text_goal_color_green);
            String autoGoalType = currentGoalType; // Текущий тип цели по умолчанию


            if (lastTarget.get() > 0 && !entries.isEmpty() && entries.size() > 1) {

                // Получаем последнюю запись для анализа текущего состояния
                WeightEntry latestEntry = entries.get(entries.size() - 1);
                WeightEntry prevEntry = entries.get(entries.size() - 2);
                float currentActualWeight = latestEntry.weight;
                float prevActualWeight = prevEntry.weight;
                // Тип цели берем из последней записи или общей переменной
                String goal = latestEntry.goalType != null ? latestEntry.goalType : "maintain";
//
                boolean goalPassed = false;
                float remaining = 0;


                // Расчет остатка и проверка достижения
                switch (goal) {
                    case "maintain":
                        remaining = Math.abs(currentActualWeight - lastTarget.get());
                        if (remaining <= 0.4f) goalPassed = true;
                        break;

                    case "lose":
                        remaining = currentActualWeight - lastTarget.get();

                        if (remaining <= 0) goalPassed = true;
                        break;
                    case "gain":
                        remaining = lastTarget.get() - currentActualWeight;
                        if (remaining <= 0) goalPassed = true;
                        break;
                }


                if (goalPassed) {
                    trendColor = ColorUtils.blendARGB(getColor(R.color.text_goal_color_green), getColor(R.color.text_main), 0.4f);
                    // Устанавливаем режим сохранения веса
                    autoGoalType = "maintain";

                } else {
                    // Стандартная логика цветов (красный/зеленый тренд)
                    int lastPointColor = circleColors.get(circleColors.size() - 1);
                    trendColor = ColorUtils.blendARGB(lastPointColor, Color.BLACK, 0.4f);

                    // Формируем строку с остатком
//                    String remainingText = String.format(Locale.getDefault(), " (" + R.string.remaining + "%.1f" + R.string.unit_kg+")", remaining);


                }

            }


//            final String finalTrendText = trendMsg;
            final float finalLastTarget = lastTarget.get();
            final float finalLastWeight = lastWeight;
            final float finalMinX = minX;
            final float finalMaxX = maxX;
            final List<WeightEntry> history = new ArrayList<>(entries);
            Collections.reverse(history);

            int finalTrendColor = trendColor;
            runOnUiThread(() -> {

                adapter.setData(history);
                if (history.isEmpty()) {
                    lineChartWeight.clear(); // Полностью очищаем график
                    lineChartWeight.setNoDataText(getString(R.string.start_text));
                    tvTrend.setText(""); // Очищаем текст тренда
                    etCurrentWeight.setText("");
                    etTargetWeight.setText("");
                    currentGoalType = "";
                    buildMeasurementChart(entries, "chest", lineChartChest, selectedDate);
                    buildMeasurementChart(entries, "waist", lineChartWaist, selectedDate);
                    buildMeasurementChart(entries, "hips", lineChartHips, selectedDate);
                    buildMeasurementChart(entries, "bicep", lineChartBicep, selectedDate);
                    return;
                }

                tvTrend.setTextColor(getColor(R.color.sub_text));


                // Настройка данных графика
                LineDataSet dataSet = new LineDataSet(chartPoints, "Weight");
                dataSet.setCircleColors(circleColors);
                dataSet.setCircleRadius(6f);
                dataSet.setCircleHoleRadius(3f);
                dataSet.setColor(getColor(R.color.text_goal_color));
                dataSet.setLineWidth(3f);
                dataSet.setDrawValues(true);
                dataSet.setMode(LineDataSet.Mode.LINEAR);
                dataSet.setDrawFilled(true);
                dataSet.setFillColor(getColor(R.color.text_goal_color));
                dataSet.setFillAlpha(40);
                dataSet.setValueTextColor(getColor(R.color.primary));
                LineData lineData = new LineData(dataSet);
                lineChartWeight.setData(lineData);

                YAxis leftAxis = lineChartWeight.getAxisLeft();

                leftAxis.removeAllLimitLines();
                // ВАЖНО: Сбрасываем ручные настройки границ, чтобы график не "обрезался"
                leftAxis.resetAxisMaximum();
                leftAxis.resetAxisMinimum();
                lineChartWeight.notifyDataSetChanged();

                if (finalLastTarget > 0) {
                    LimitLine ll = new LimitLine(finalLastTarget, getString(R.string.target_weight) + finalLastTarget + getString(R.string.unit_kg));
                    ll.setLineColor(getColor(R.color.text_goal_chart_color));
                    ll.setLineWidth(2f);
                    ll.enableDashedLine(10f, 5f, 0f);
                    ll.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
                    ll.setTextSize(12f);
                    ll.setTextColor(getColor(R.color.text_goal_chart_color));
                    leftAxis.addLimitLine(ll);

                    // Включаем отображение линии ПОЗАДИ данных, чтобы она не перекрывала точки
                    leftAxis.setDrawLimitLinesBehindData(true);

                    float currentMax = leftAxis.getAxisMaximum();
                    float currentMin = leftAxis.getAxisMinimum();

                    // Если цель выше, чем самый высокий вес на графике, поднимаем потолок
                    if (finalLastTarget >= currentMax) {
                        leftAxis.setAxisMaximum(finalLastTarget + 2f);
                    }
                    // Если цель ниже, чем самый низкий вес, опускаем пол
                    if (finalLastTarget <= currentMin) {
                        leftAxis.setAxisMinimum(finalLastTarget - 2f);
                    }

                }

                if (!chartPoints.isEmpty()) {
                    lineChartWeight.getXAxis().setAxisMinimum(finalMinX - 0.5f);
                    lineChartWeight.getXAxis().setAxisMaximum(Math.max(finalMaxX + 0.5f, finalMinX + 6.5f));
                    lineChartWeight.setVisibleXRangeMaximum(7f);
                }

                // Скролл к выбранной дате
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date d = sdf.parse(selectedDate);
                    if (d != null) {
                        float selectedX = (float) TimeUnit.MILLISECONDS.toDays(d.getTime());
                        lineChartWeight.moveViewToX(selectedX - 3f);


                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                buildMeasurementChart(entries, "chest", lineChartChest, selectedDate);
                buildMeasurementChart(entries, "waist", lineChartWaist, selectedDate);
                buildMeasurementChart(entries, "hips", lineChartHips, selectedDate);
                buildMeasurementChart(entries, "bicep", lineChartBicep, selectedDate);

                lineChartWeight.animateX(400);
                lineChartWeight.invalidate();
//                updateGoalButtonsUI(currentGoalType);
                });
        }).start();
    }


    private class WeightAdapter extends RecyclerView.Adapter<WeightAdapter.ViewHolder> {
        private List<WeightEntry> historyList;

        public void setData(List<WeightEntry> list) {
            this.historyList = list;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_weight, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            WeightEntry entry = historyList.get(position);
            holder.tvDate.setText(entry.date);
            holder.tvWeight.setText(String.format("%s %s", entry.weight, getString(R.string.unit_kg)));

// Формируем строку замеров, если они есть
            SpannableStringBuilder sb = new SpannableStringBuilder();
            appendMeasurement(sb, getString(R.string.chest), entry.chest);
            appendMeasurement(sb, getString(R.string.waist), entry.waist);
            appendMeasurement(sb, getString(R.string.hips), entry.hips);
            appendMeasurement(sb, getString(R.string.bicep), entry.bicep);


            if (sb.length() > 0) {
                holder.tvMeasurements.setVisibility(View.VISIBLE);
                holder.tvMeasurements.setText(sb, TextView.BufferType.SPANNABLE);
            } else {
                holder.tvMeasurements.setVisibility(View.GONE);
            }
            holder.btnDelete.setOnClickListener(v -> deleteEntry(entry.date));
        }


        /**
         * Вспомогательный метод для форматирования:
         * "Название: " - обычный, "Значение " - жирный
         */
        private void appendMeasurement(SpannableStringBuilder sb, String label, float value) {
            if (value > 0) {
                // Если в строке уже что-то есть, добавим отступ
                if (sb.length() > 0) {
                    sb.append("  ");
                }

                // 1. Добавляем обычный текст (название)
                int startLabel = sb.length();
                sb.append(label);
                sb.append(" ");

                // 2. Форматируем число: если 70.0 -> "70", если 70.5 -> "70.5"
                String valueText;
                if (value == (long) value) {
                    valueText = String.format(Locale.getDefault(), "%d ", (long) value);
                } else {
                    valueText = String.format(Locale.getDefault(), "%.1f ", value);
                }

                valueText += getString(R.string.unit_cm); // Добавляем единицы измерения

                // 2. Добавляем значение
                int startValue = sb.length();
                sb.append(valueText);
                int endValue = sb.length();

                // 3. Делаем только значение жирным
                sb.setSpan(
                        new StyleSpan(Typeface.BOLD),
                        startValue,
                        endValue,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }

        @Override
        public int getItemCount() {
            return historyList == null ? 0 : historyList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDate, tvWeight, tvMeasurements;
            ImageButton btnDelete;

            ViewHolder(View itemView) {
                super(itemView);
                tvDate = itemView.findViewById(R.id.tvHistoryDate);
                tvWeight = itemView.findViewById(R.id.tvHistoryWeight);
                tvMeasurements = itemView.findViewById(R.id.tvMeasurements);
                tvMeasurements.setTextColor(ColorUtils.blendARGB(getColor(R.color.primary), Color.BLACK, 0.3f));
                btnDelete = itemView.findViewById(R.id.btnDelete);

            }
        }
    }

    private void createCsvFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/comma-separated-values");
        intent.putExtra(Intent.EXTRA_TITLE, "bodytune_backup.csv");
        startActivityForResult(intent, CREATE_FILE);
    }

    private void exportToCsv(Uri uri) {
        new Thread(() -> {
            List<WeightEntry> entries = db.weightDao().getAllEntries();
            StringBuilder csvContent = new StringBuilder();
            // Заголовок
            csvContent.append("date,weight,targetWeight,waist,chest,hips,bicep,goalType\n");

            for (WeightEntry e : entries) {
                csvContent.append(e.date).append(",")
                        .append(e.weight).append(",")
                        .append(e.targetWeight).append(",")
                        .append(e.waist).append(",")
                        .append(e.chest).append(",")
                        .append(e.hips).append(",")
                        .append(e.bicep).append(",")
                        .append(e.goalType).append("\n");
            }

            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                os.write(csvContent.toString().getBytes());
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.export_text), Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
    private void openCsvFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*"); // Некоторые файловые менеджеры плохо фильтруют CSV
        startActivityForResult(intent, PICK_PDF_FILE);
    }

    private void importFromCsv(Uri uri) {
        new Thread(() -> {
            try (InputStream is = getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

                String line;
                String header = reader.readLine(); // Читаем заголовок

                // Простейшая проверка: если заголовок пустой, файл скорее всего поврежден
                if (header == null) {
                    throw new IOException("Empty file");
                }

                int importedCount = 0;

                while ((line = reader.readLine()) != null) {
                    // Используем лимит -1 для split, чтобы видеть пустые колонки
                    String[] parts = line.split(",", -1);

                    // Проверяем, что в строке есть хотя бы дата и вес
                    if (parts.length >= 2) {
                        try {
                            String date = parts[0].trim();
                            Float weightValue = safeParseFloat(parts[1]);


                            // Если дата пустая или вес 0/null — это мусор, а не данные. Пропускаем.
                            if (date.isEmpty() || weightValue == null || weightValue <= 0) {
                                continue;
                            }
                            WeightEntry entry = new WeightEntry(date, weightValue);

                            // Парсим остальные поля (они могут быть null)
                            if (parts.length > 2) entry.targetWeight = safeParseFloat(parts[2]) != null ? safeParseFloat(parts[2]) : 0f;
                            if (parts.length > 3) entry.waist = safeParseFloat(parts[3]);
                            if (parts.length > 4) entry.chest = safeParseFloat(parts[4]);
                            if (parts.length > 5) entry.hips = safeParseFloat(parts[5]);
                            if (parts.length > 6) entry.bicep = safeParseFloat(parts[6]);
                            if (parts.length > 7) entry.goalType = parts[7].trim();

                            db.weightDao().insert(entry);
                            importedCount++;
                        } catch (Exception rowException) {
                            // Ошибка в конкретной строке — логируем и идем дальше
                            Log.e("IMPORT", "Ошибка в строке: " + line, rowException);
                        }
                    }
                }

                final int finalCount = importedCount;
                runOnUiThread(() -> {
                    loadWeightForSelectedDate(); // Обновляем UI и графики
                    Toast.makeText(this, getString(R.string.import_completed) + finalCount, Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                // Если произошла общая ошибка (файл не читается, формат совсем не тот)
                Log.e("IMPORT", "Общая ошибка импорта", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.import_error), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private Float safeParseFloat(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null; // Важно: возвращаем null, а не 0
        }
        try {
            // Заменяем запятую на точку на случай, если CSV открывали в Excel
            return Float.parseFloat(value.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            if (requestCode == CREATE_FILE) {
                exportToCsv(data.getData());
            } else if (requestCode == PICK_PDF_FILE) {
                importFromCsv(data.getData());
            }
        }
    }
}