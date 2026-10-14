package uz.jtscorp.xarajatlar.ui.settings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Objects;

import uz.jtscorp.xarajatlar.R;
import uz.jtscorp.xarajatlar.data.entity.IncomeCategory;

public class IncomeCategoryAdapter extends ListAdapter<IncomeCategory, IncomeCategoryAdapter.ViewHolder> {

    private final OnItemClickListener onItemClick;
    private final OnDeleteClickListener onDeleteClick;

    public interface OnItemClickListener {
        void onItemClick(IncomeCategory category);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(IncomeCategory category);
    }

    public IncomeCategoryAdapter(OnItemClickListener onItemClick, OnDeleteClickListener onDeleteClick) {
        super(DIFF_CALLBACK);
        this.onItemClick = onItemClick;
        this.onDeleteClick = onDeleteClick;
    }

    private static final DiffUtil.ItemCallback<IncomeCategory> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<IncomeCategory>() {
                @Override
                public boolean areItemsTheSame(@NonNull IncomeCategory oldItem, @NonNull IncomeCategory newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull IncomeCategory oldItem, @NonNull IncomeCategory newItem) {
                    return Objects.equals(oldItem.getName(), newItem.getName()) &&
                            Objects.equals(oldItem.getEmoji(), newItem.getEmoji());
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_manage_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IncomeCategory category = getItem(position);
        holder.bind(category);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final View colorIndicator;
        private final TextView emojiText;
        private final TextView titleText;
        private final TextView subtitleText;
        private final ImageButton deleteButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            colorIndicator = itemView.findViewById(R.id.colorIndicator);
            emojiText = itemView.findViewById(R.id.emojiText);
            titleText = itemView.findViewById(R.id.titleText);
            subtitleText = itemView.findViewById(R.id.subtitleText);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }

        void bind(IncomeCategory category) {
            titleText.setText(category.getName());

            // Hide color indicator and subtitle (not needed for income categories)
            colorIndicator.setVisibility(View.GONE);
            subtitleText.setVisibility(View.GONE);

            // Set emoji if available
            String emoji = category.getEmoji();
            if (emoji != null && !emoji.isEmpty()) {
                emojiText.setText(emoji);
                emojiText.setVisibility(View.VISIBLE);
            } else {
                emojiText.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> onItemClick.onItemClick(category));
            deleteButton.setOnClickListener(v -> onDeleteClick.onDeleteClick(category));
        }
    }
}
