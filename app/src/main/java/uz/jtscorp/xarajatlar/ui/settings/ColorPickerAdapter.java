package uz.jtscorp.xarajatlar.ui.settings;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import uz.jtscorp.xarajatlar.R;

public class ColorPickerAdapter extends RecyclerView.Adapter<ColorPickerAdapter.ViewHolder> {

    private final int[] colors;
    private int selectedPosition;
    private final OnColorSelectedListener listener;

    public interface OnColorSelectedListener {
        void onColorSelected(int position);
    }

    public ColorPickerAdapter(int[] colors, int selectedPosition, OnColorSelectedListener listener) {
        this.colors = colors;
        this.selectedPosition = selectedPosition;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_color_picker, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(colors[position], position == selectedPosition);
    }

    @Override
    public int getItemCount() {
        return colors.length;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final View colorView;
        private final View checkMark;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            colorView = itemView.findViewById(R.id.colorView);
            checkMark = itemView.findViewById(R.id.checkMark);

            itemView.setOnClickListener(v -> {
                int previousSelected = selectedPosition;
                selectedPosition = getAdapterPosition();
                notifyItemChanged(previousSelected);
                notifyItemChanged(selectedPosition);
                listener.onColorSelected(selectedPosition);
            });
        }

        void bind(int color, boolean isSelected) {
            GradientDrawable drawable = (GradientDrawable) colorView.getBackground();
            drawable.setColor(color);
            checkMark.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            itemView.setSelected(isSelected);
            itemView.setContentDescription(itemView.getContext().getString(R.string.color_option,
                    getBindingAdapterPosition() + 1,
                    isSelected ? itemView.getContext().getString(R.string.selected) : ""));
        }
    }
}
