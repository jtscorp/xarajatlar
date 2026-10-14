package uz.jtscorp.xarajatlar.ui.expense;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import uz.jtscorp.xarajatlar.R;
import uz.jtscorp.xarajatlar.data.entity.Category;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private final List<Category> categories;
    private final OnCategorySelectedListener listener;
    private int selectedPosition = -1;

    public interface OnCategorySelectedListener {
        void onCategorySelected(Category category);
    }

    public CategoryAdapter(List<Category> categories, OnCategorySelectedListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_grid, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.bind(category, position == selectedPosition);

        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getBindingAdapterPosition();
            if (selectedPosition == RecyclerView.NO_POSITION) return;

            if (previousSelected != -1) {
                notifyItemChanged(previousSelected);
            }
            notifyItemChanged(selectedPosition);

            listener.onCategorySelected(category);
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public void setSelectedCategoryId(int categoryId) {
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getId() == categoryId) {
                int previousSelected = selectedPosition;
                selectedPosition = i;

                if (previousSelected != -1) {
                    notifyItemChanged(previousSelected);
                }
                notifyItemChanged(selectedPosition);
                break;
            }
        }
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView emojiView;
        private final TextView nameView;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            emojiView = itemView.findViewById(R.id.categoryEmoji);
            nameView = itemView.findViewById(R.id.categoryName);
        }

        void bind(Category category, boolean isSelected) {
            nameView.setText(category.getName());
            itemView.setSelected(isSelected);

            // Set emoji from category iconName field
            String emoji = category.getIconName();
            if (emoji != null && !emoji.isEmpty()) {
                emojiView.setText(emoji);
            } else {
                // Fallback: first letter of category name
                String name = category.getName();
                if (name != null && !name.isEmpty()) {
                    emojiView.setText(name.substring(0, 1).toUpperCase());
                } else {
                    emojiView.setText("📦");
                }
            }
        }
    }
}
