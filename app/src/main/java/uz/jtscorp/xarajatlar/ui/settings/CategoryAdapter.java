package uz.jtscorp.xarajatlar.ui.settings;

import android.graphics.drawable.GradientDrawable;
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
import uz.jtscorp.xarajatlar.data.entity.Category;

public class CategoryAdapter extends ListAdapter<Category, CategoryAdapter.ViewHolder> {

    private final OnItemClickListener onItemClick;
    private final OnDeleteClickListener onDeleteClick;

    public interface OnItemClickListener {
        void onItemClick(Category category);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(Category category);
    }

    public CategoryAdapter(OnItemClickListener onItemClick, OnDeleteClickListener onDeleteClick) {
        super(DIFF_CALLBACK);
        this.onItemClick = onItemClick;
        this.onDeleteClick = onDeleteClick;
    }

    private static final DiffUtil.ItemCallback<Category> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Category>() {
                @Override
                public boolean areItemsTheSame(@NonNull Category oldItem, @NonNull Category newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull Category oldItem, @NonNull Category newItem) {
                    return oldItem.getName().equals(newItem.getName()) &&
                            oldItem.getColor() == newItem.getColor() &&
                            Objects.equals(oldItem.getIconName(), newItem.getIconName());
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
        Category category = getItem(position);
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

        void bind(Category category) {
            titleText.setText(category.getName());
            subtitleText.setVisibility(View.GONE);

            // Set category color
            GradientDrawable drawable = (GradientDrawable) colorIndicator.getBackground();
            drawable.setColor(category.getColor() != 0 ? category.getColor() : 0xFF4CAF50);

            // Set emoji if available
            String iconName = category.getIconName();
            if (iconName != null && !iconName.isEmpty()) {
                emojiText.setText(iconName);
                emojiText.setVisibility(View.VISIBLE);
            } else {
                emojiText.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> onItemClick.onItemClick(category));
            deleteButton.setOnClickListener(v -> onDeleteClick.onDeleteClick(category));
        }
    }
}
