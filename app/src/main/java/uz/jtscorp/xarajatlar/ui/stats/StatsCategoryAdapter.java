package uz.jtscorp.xarajatlar.ui.stats;

import uz.jtscorp.xarajatlar.utils.Formats;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import uz.jtscorp.xarajatlar.R;

public class StatsCategoryAdapter extends RecyclerView.Adapter<StatsCategoryAdapter.ViewHolder> {

    private java.util.function.Consumer<CategoryStat> onItemClick;
    public void setOnItemClick(java.util.function.Consumer<CategoryStat> listener) { onItemClick = listener; }

    private List<CategoryStat> items = new ArrayList<>();

    public static class CategoryStat {
        public final int id;
        public final String name;
        public final String emoji;
        public final int color;
        public final double amount;
        public final float percent;

        public CategoryStat(int id, String name, String emoji, int color, double amount, float percent) {
            this.id = id;
            this.name = name;
            this.emoji = emoji;
            this.color = color;
            this.amount = amount;
            this.percent = percent;
        }
    }

    public void setItems(List<CategoryStat> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stat_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryStat item = items.get(position);
        holder.bind(item);
        holder.itemView.setOnClickListener(v -> { if (onItemClick != null) onItemClick.accept(item); });
        holder.itemView.setFocusable(true);
        holder.itemView.setContentDescription(holder.itemView.getContext().getString(
                R.string.stats_item_description, item.name,
                Formats.money(holder.itemView.getContext(), item.amount)));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView categoryEmoji;
        private final TextView categoryName;
        private final ProgressBar categoryProgress;
        private final TextView categoryPercent;
        private final TextView categoryAmount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryEmoji = itemView.findViewById(R.id.categoryEmoji);
            categoryName = itemView.findViewById(R.id.categoryName);
            categoryProgress = itemView.findViewById(R.id.categoryProgress);
            categoryPercent = itemView.findViewById(R.id.categoryPercent);
            categoryAmount = itemView.findViewById(R.id.categoryAmount);
        }

        void bind(CategoryStat stat) {
            categoryName.setText(stat.name);
            categoryAmount.setText(Formats.money(itemView.getContext(), stat.amount));
            categoryPercent.setText(String.format(Formats.locale(itemView.getContext()), "%.0f%%", stat.percent));
            categoryProgress.setProgress((int) stat.percent);

            // Emoji
            if (stat.emoji != null && !stat.emoji.isEmpty()) {
                categoryEmoji.setText(stat.emoji);
            } else {
                String name = stat.name;
                categoryEmoji.setText(name != null && !name.isEmpty() ? name.substring(0, 1).toUpperCase() : "");
            }

            // Circle color
            GradientDrawable drawable = (GradientDrawable) categoryEmoji.getBackground();
            if (drawable != null) {
                drawable = (GradientDrawable) drawable.mutate();
                drawable.setColor(stat.color);
            }
        }
    }
}
