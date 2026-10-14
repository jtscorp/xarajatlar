package uz.jtscorp.xarajatlar.ui.stats;

import uz.jtscorp.xarajatlar.R;
import uz.jtscorp.xarajatlar.utils.Formats;
import android.graphics.Color;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.List;

public class StatsChartPagerAdapter extends RecyclerView.Adapter<StatsChartPagerAdapter.ViewHolder> {

    private static final int PAGE_COUNT = 2;

    private List<PieEntry> entries = new ArrayList<>();
    private List<Integer> colors = new ArrayList<>();

    private java.util.function.Consumer<Integer> onGroupSelected;
    public void setOnGroupSelected(java.util.function.Consumer<Integer> listener) { onGroupSelected = listener; }
    private final com.github.mikephil.charting.listener.OnChartValueSelectedListener selection =
            new com.github.mikephil.charting.listener.OnChartValueSelectedListener() {
        @Override public void onValueSelected(com.github.mikephil.charting.data.Entry entry,
                com.github.mikephil.charting.highlight.Highlight highlight) {
            Object id = entry.getData();
            if (id instanceof Integer && onGroupSelected != null) onGroupSelected.accept((Integer) id);
        }
        @Override public void onNothingSelected() {}
    };
    private PieChart pieChart;
    private HorizontalBarChart barChart;

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(android.view.View v) { super(v); }
    }

    @Override
    public int getItemCount() { return PAGE_COUNT; }

    @Override
    public int getItemViewType(int position) { return position; }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 0) {
            PieChart chart = new PieChart(parent.getContext());
            chart.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            int pad = dpToPx(parent, 12);
            chart.setPadding(pad, pad, pad, pad);
            configurePieChart(chart);
            pieChart = chart;
            return new ViewHolder(chart);
        } else {
            HorizontalBarChart chart = new HorizontalBarChart(parent.getContext());
            chart.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            int pad = dpToPx(parent, 12);
            chart.setPadding(pad, pad, pad, pad);
            configureBarChart(chart);
            barChart = chart;
            return new ViewHolder(chart);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position == 0 && pieChart != null) {
            renderPieChart();
        } else if (position == 1 && barChart != null) {
            renderBarChart();
        }
    }

    public void updateData(List<PieEntry> entries, List<Integer> colors) {
        this.entries = entries != null ? entries : new ArrayList<>();
        this.colors = colors != null ? colors : new ArrayList<>();
        if (pieChart != null) renderPieChart();
        if (barChart != null) renderBarChart();
    }

    public void showEmpty() {
        this.entries = new ArrayList<>();
        this.colors = new ArrayList<>();
        if (pieChart != null) {
            List<PieEntry> empty = new ArrayList<>();
            empty.add(new PieEntry(1f,
                    pieChart.getContext().getString(R.string.chart_no_data)));
            PieDataSet ds = new PieDataSet(empty, "");
            ds.setColors(Color.LTGRAY);
            ds.setDrawValues(false);
            pieChart.setData(new PieData(ds));
            pieChart.invalidate();
        }
        if (barChart != null) {
            barChart.clear();
            barChart.invalidate();
        }
    }

    private void configurePieChart(PieChart chart) {
        chart.setOnChartValueSelectedListener(selection);
        int foreground = com.google.android.material.color.MaterialColors.getColor(chart, com.google.android.material.R.attr.colorOnSurface);
        chart.getLegend().setTextColor(foreground);
        chart.setUsePercentValues(true);
        chart.getDescription().setEnabled(false);
        chart.setExtraOffsets(5, 5, 5, 5);
        chart.setDragDecelerationFrictionCoef(0.95f);
        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.TRANSPARENT);
        chart.setHoleRadius(45f);
        chart.setTransparentCircleRadius(50f);
        chart.setDrawEntryLabels(false);
        chart.getLegend().setEnabled(true);
        chart.getLegend().setWordWrapEnabled(true);
        chart.getLegend().setTextSize(12f);
    }

    private void configureBarChart(HorizontalBarChart chart) {
        chart.getDescription().setEnabled(false);
        chart.setOnChartValueSelectedListener(selection);
        chart.getXAxis().setTextColor(com.google.android.material.color.MaterialColors.getColor(chart, com.google.android.material.R.attr.colorOnSurface));
        chart.setDrawGridBackground(false);
        chart.setDrawValueAboveBar(true);
        chart.setFitBars(true);
        chart.setExtraLeftOffset(8f);
        chart.setExtraRightOffset(16f);
        chart.getLegend().setEnabled(false);

        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setEnabled(false);
        chart.getAxisLeft().setAxisMinimum(0f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(12f);
    }

    private void renderPieChart() {
        if (entries.isEmpty()) {
            showEmpty();
            return;
        }
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(11f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setSliceSpace(2f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.animateY(500);
        pieChart.invalidate();
    }

    private void renderBarChart() {
        if (entries.isEmpty()) {
            barChart.clear();
            barChart.invalidate();
            return;
        }

        List<BarEntry> barEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<Integer> barColors = new ArrayList<>();

        int idx = 0;
        for (int i = entries.size() - 1; i >= 0; i--) {
            PieEntry pe = entries.get(i);
            if (pe.getValue() > 0) {
                barEntries.add(new BarEntry(idx, pe.getValue(), pe.getData()));
                labels.add(pe.getLabel());
                if (i < colors.size()) {
                    barColors.add(colors.get(i));
                }
                idx++;
            }
        }

        if (barEntries.isEmpty()) {
            barChart.clear();
            barChart.invalidate();
            return;
        }

        BarDataSet dataSet = new BarDataSet(barEntries, "");
        dataSet.setColors(barColors);
        dataSet.setValueTextSize(10f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return Formats.number((long) value);
            }
        });

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.5f);

        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChart.getXAxis().setLabelCount(labels.size());
        barChart.setData(data);
        barChart.animateY(500);
        barChart.invalidate();
    }

    private int dpToPx(ViewGroup parent, int dp) {
        return (int) (dp * parent.getContext().getResources().getDisplayMetrics().density);
    }
}
