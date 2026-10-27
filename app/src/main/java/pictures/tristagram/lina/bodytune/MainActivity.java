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

    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(date);
    }

    private String determineGoalType(float weight, float target) {
        float diff = weight - target;
        if (diff > 0.6f) {
            return "lose";
        }
        if (diff < -0.6f) {
            return "gain";
        }
        return "maintain";
    }

    private Float parseNullableFloat(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return Float.parseFloat(trimmed.replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void setupCollapsibleGraph(int headerId, View container, ImageView icon, String prefKey) {
        boolean isVisible = getSharedPreferences("GraphSettings", MODE_PRIVATE).getBoolean(prefKey, true);
        container.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        icon.setRotation(isVisible ? 180 : 0);

        findViewById(headerId).setOnClickListener(v -> {
            boolean nowVisible = container.getVisibility() == View.VISIBLE;

            if (nowVisible) {
                container.setVisibility(View.GONE);
                icon.animate().rotation(0).setDuration(300).start();
            } else {
                container.setVisibility(View.VISIBLE);
                icon.animate().rotation(180).setDuration(300).start();
            }

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
        chart.setExtraBottomOffset(30f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(getColor(R.color.sub_text));
        xAxis.setTypeface(sm);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(getColor(R.color.sub_text));
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(7, false);
        xAxis.setTextSize(12f);
        xAxis.setYOffset(10f);
        chart.setXAxisRenderer(new MultiLineXAxisRenderer(
                chart.getViewPortHandler(),
                xAxis,
                chart.getTransformer(YAxis.AxisDependency.LEFT)));

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTypeface(tf);
        leftAxis.setTextColor(getColor(R.color.primary));
        leftAxis.setGridColor(getColor(R.color.sub_text));
        leftAxis.setDrawZeroLine(false);

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
            chart.setNoDataText(getString(R.string.no_data_for) + " " + measurementName);
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

        chart.setVisibleXRangeMaximum(7f);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.notifyDataSetChanged();
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

        Float weight = parseNullableFloat(weightStr);
        Float heightCm = parseNullableFloat(heightStr);

        if (weight != null && heightCm != null && heightCm > 0) {
            float heightM = heightCm / 100f;
            if (heightM > 0) {
                float bmi = weight / (heightM * heightM);
                tvBMIResult.setText(String.format(Locale.getDefault(), "ИМТ: %.1f \n(%s)", bmi, getBMICategory(bmi)));
            } else {
                tvBMIResult.setText(getString(R.string.bmi_title));
            }
        } else {
            tvBMIResult.setText(getString(R.string.bmi_title));
        }

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
                cal.add(Calendar.DAY_OF_MONTH, amount);

                selectedDate = formatDate(cal.getTime());
                tvSelectedDate.setText(selectedDate);
                loadWeightForSelectedDate();

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

            WeightEntry lastKnownEntry = db.weightDao().getLastAnyEntry();

            WeightEntry trendCurrent;
            WeightEntry trendPrevious;

            if (currentEntry != null) {
                trendCurrent = currentEntry;
                trendPrevious = db.weightDao().getLatestEntryBefore(this.selectedDate);
            } else {
                trendCurrent = lastKnownEntry;
                if (trendCurrent != null) {
                    trendPrevious = db.weightDao().getLatestEntryBefore(trendCurrent.date);
                } else {
                    trendPrevious = null;
                }
            }

            WeightEntry finalTrendCurrent = trendCurrent;
            runOnUiThread(() -> {
                updateUIWithLoadedData(lastKnownEntry);
                displayTrend(finalTrendCurrent, trendPrevious);
                updateChart();
            });
        }).start();
    }

    private void displayTrend(WeightEntry current, WeightEntry previous) {
        if (current != null && previous != null) {
            float diff = current.weight - previous.weight;
            String dateInfo = (currentEntry == null) ? getString(R.string.last_data_text) + current.date + ")" : "";

            if (diff < 0) {
                tvTrend.setText(getString(R.string.trend_text_lose) + String.format(" (%.1f ", Math.abs(diff)) + getString(R.string.unit_kg) + ")" + dateInfo);
            } else if (diff > 0) {
                tvTrend.setText(getString(R.string.trend_text_gain) + String.format(" (+%.1f ", diff) + getString(R.string.unit_kg) + ")" + dateInfo);
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
            if (lastKnownEntry != null) {
                etTargetWeight.setText(String.valueOf(lastKnownEntry.targetWeight));
            }
        }

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
        long selectionMillis = MaterialDatePicker.todayInUtcMilliseconds();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(selectedDate);
            if (date != null) {
                selectionMillis = date.getTime();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse selected date", e);
        }

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.date_picker_title))
                .setSelection(selectionMillis)
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.setTimeInMillis(selection);
            selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
            tvSelectedDate.setText(selectedDate);
            loadWeightForSelectedDate();
            if (lineChartWeight.getData() != null && !lineChartWeight.getData().getDataSets().isEmpty()) {
                float days = (float) TimeUnit.MILLISECONDS.toDays(selection);
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
        lineChartWeight.getXAxis().setTypeface(tf);
        lineChartWeight.getAxisLeft().setTypeface(sm);

        lineChartWeight.setBackgroundColor(getColor(R.color.card));
        lineChartWeight.setGridBackgroundColor(getColor(R.color.card));
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
        lineChartWeight.setXAxisRenderer(new MultiLineXAxisRenderer(
                lineChartWeight.getViewPortHandler(),
                lineChartWeight.getXAxis(),
                lineChartWeight.getTransformer(YAxis.AxisDependency.LEFT)));

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

        Float weight = parseNullableFloat(weightStr);
        if (weight == null || weight <= 0) {
            Toast.makeText(this, R.string.msg_enter_weight, Toast.LENGTH_SHORT).show();
            return;
        }

        Float target = parseNullableFloat(targetStr);
        if (target == null) {
            target = weight;
        }

        Float chest = parseNullableFloat(etChest.getText().toString());
        Float waist = parseNullableFloat(etWaist.getText().toString());
        Float hips = parseNullableFloat(etHips.getText().toString());
        Float bicep = parseNullableFloat(etBicep.getText().toString());

        if (chest == null) chest = 0f;
        if (waist == null) waist = 0f;
        if (hips == null) hips = 0f;
        if (bicep == null) bicep = 0f;

        new Thread(() -> {
            List<WeightEntry> allEntries = db.weightDao().getAllEntries();
            currentGoalType = determineGoalType(weight, target);

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
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing weight date: " + current.date, e);
                }

                if (current.targetWeight != null && current.targetWeight > 0) {
                    lastTarget.set(current.targetWeight);
                }

                int color = getColor(R.color.text_goal_color);
                if (i > 0) {
                    WeightEntry prev = entries.get(i - 1);
                    String goal = current.goalType != null ? current.goalType : "maintain";
                    if (goal.equals("lose")) {
                        color = (current.weight < prev.weight) ? getColor(R.color.text_goal_color_green) : getColor(R.color.text_goal_color_red);
                    } else if (goal.equals("gain")) {
                        color = (current.weight > prev.weight) ? getColor(R.color.text_goal_color_green) : getColor(R.color.text_goal_color_red);
                    } else if (goal.equals("maintain")) {
                        color = (Math.abs(current.weight - lastTarget.get()) <= 0.4f) ? getColor(R.color.text_goal_color_green) : getColor(R.color.text_goal_color_red);
                    }
                }
                circleColors.add(color);
            }

            int trendColor = getColor(R.color.text_goal_color_green);
            String autoGoalType = currentGoalType;

            if (lastTarget.get() > 0 && !entries.isEmpty() && entries.size() > 1) {
                WeightEntry latestEntry = entries.get(entries.size() - 1);
                WeightEntry prevEntry = entries.get(entries.size() - 2);
                float currentActualWeight = latestEntry.weight;
                float prevActualWeight = prevEntry.weight;
                String goal = latestEntry.goalType != null ? latestEntry.goalType : "maintain";

                boolean goalPassed = false;
                float remaining = 0;

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
                    autoGoalType = "maintain";
                } else {
                    int lastPointColor = circleColors.get(circleColors.size() - 1);
                    trendColor = ColorUtils.blendARGB(lastPointColor, Color.BLACK, 0.4f);
                }
            }

            final float finalLastTarget = lastTarget.get();
            final float finalLastWeight = lastWeight;
            final float finalMinX = minX;
            final float finalMaxX = maxX;
            final List<WeightEntry> history = new ArrayList<>(entries);
            Collections.reverse(history);

            final int finalTrendColor = trendColor;
            runOnUiThread(() -> {
                adapter.setData(history);
                if (history.isEmpty()) {
                    lineChartWeight.clear();
                    lineChartWeight.setNoDataText(getString(R.string.start_text));
                    tvTrend.setText("");
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
                    leftAxis.setDrawLimitLinesBehindData(true);

                    float currentMax = leftAxis.getAxisMaximum();
                    float currentMin = leftAxis.getAxisMinimum();

                    if (finalLastTarget >= currentMax) {
                        leftAxis.setAxisMaximum(finalLastTarget + 2f);
                    }
                    if (finalLastTarget <= currentMin) {
                        leftAxis.setAxisMinimum(finalLastTarget - 2f);
                    }
                }

                if (!chartPoints.isEmpty()) {
                    lineChartWeight.getXAxis().setAxisMinimum(finalMinX - 0.5f);
                    lineChartWeight.getXAxis().setAxisMaximum(Math.max(finalMaxX + 0.5f, finalMinX + 6.5f));
                    lineChartWeight.setVisibleXRangeMaximum(7f);
                }

                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date d = sdf.parse(selectedDate);
                    if (d != null) {
                        float selectedX = (float) TimeUnit.MILLISECONDS.toDays(d.getTime());
                        lineChartWeight.moveViewToX(selectedX - 3f);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error moving chart to selected date", e);
                }

                buildMeasurementChart(entries, "chest", lineChartChest, selectedDate);
                buildMeasurementChart(entries, "waist", lineChartWaist, selectedDate);
                buildMeasurementChart(entries, "hips", lineChartHips, selectedDate);
                buildMeasurementChart(entries, "bicep", lineChartBicep, selectedDate);

                lineChartWeight.animateX(400);
                lineChartWeight.invalidate();
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
            holder.tvWeight.setText(String.format(Locale.getDefault(), "%s %s", entry.weight, getString(R.string.unit_kg)));

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

        private void appendMeasurement(SpannableStringBuilder sb, String label, float value) {
            if (value > 0) {
                if (sb.length() > 0) {
                    sb.append("  ");
                }

                int startLabel = sb.length();
                sb.append(label);
                sb.append(" ");

                String valueText;
                if (value == (long) value) {
                    valueText = String.format(Locale.getDefault(), "%d ", (long) value);
                } else {
                    valueText = String.format(Locale.getDefault(), "%.1f ", value);
                }

                valueText += getString(R.string.unit_cm);

                int startValue = sb.length();
                sb.append(valueText);
                int endValue = sb.length();

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
                if (os == null) {
                    throw new IOException("Unable to open output stream for export");
                }
                os.write(csvContent.toString().getBytes());
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.export_text), Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                Log.e(TAG, "CSV export failed", e);
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.import_error), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void openCsvFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        startActivityForResult(intent, PICK_PDF_FILE);
    }

    private void importFromCsv(Uri uri) {
        new Thread(() -> {
            try (InputStream is = getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

                if (is == null) {
                    throw new IOException("Unable to open input stream for import");
                }

                String line;
                String header = reader.readLine();
                if (header == null) {
                    throw new IOException("Empty file");
                }

                int importedCount = 0;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",", -1);
                    if (parts.length >= 2) {
                        try {
                            String date = parts[0].trim();
                            Float weightValue = safeParseFloat(parts[1]);
                            if (date.isEmpty() || weightValue == null || weightValue <= 0) {
                                continue;
                            }
                            WeightEntry entry = new WeightEntry(date, weightValue);

                            if (parts.length > 2) entry.targetWeight = safeParseFloat(parts[2]) != null ? safeParseFloat(parts[2]) : 0f;
                            if (parts.length > 3) entry.waist = safeParseFloat(parts[3]);
                            if (parts.length > 4) entry.chest = safeParseFloat(parts[4]);
                            if (parts.length > 5) entry.hips = safeParseFloat(parts[5]);
                            if (parts.length > 6) entry.bicep = safeParseFloat(parts[6]);
                            if (parts.length > 7) entry.goalType = parts[7].trim();

                            db.weightDao().insert(entry);
                            importedCount++;
                        } catch (Exception rowException) {
                            Log.e("IMPORT", "Ошибка в строке: " + line, rowException);
                        }
                    }
                }

                final int finalCount = importedCount;
                runOnUiThread(() -> {
                    loadWeightForSelectedDate();
                    Toast.makeText(this, getString(R.string.import_completed) + finalCount, Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                Log.e("IMPORT", "Общая ошибка импорта", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.import_error), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private Float safeParseFloat(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
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
