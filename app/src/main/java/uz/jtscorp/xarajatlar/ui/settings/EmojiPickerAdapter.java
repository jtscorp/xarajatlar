package uz.jtscorp.xarajatlar.ui.settings;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import uz.jtscorp.xarajatlar.R;

public class EmojiPickerAdapter extends RecyclerView.Adapter<EmojiPickerAdapter.ViewHolder> {

    // Common category emojis
    public static final String[] CATEGORY_EMOJIS = {
            "🍔", "🛒", "🚗", "🏠", "👕", "💊", "📚", "🎮",
            "✈️", "💰", "📱", "🎬", "⚽", "🎵", "💡", "🔧",
            "🎁", "💳", "🏥", "🐕", "👶", "💼", "🍺", "☕"
    };

    private final String[] emojis;
    private int selectedPosition;
    private final OnEmojiSelectedListener listener;

    public interface OnEmojiSelectedListener {
        void onEmojiSelected(int position, String emoji);
    }

    public EmojiPickerAdapter(String[] emojis, int selectedPosition, OnEmojiSelectedListener listener) {
        this.emojis = emojis;
        this.selectedPosition = selectedPosition;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_emoji_picker, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String emoji = emojis[position];
        holder.emojiText.setText(emoji);

        // Show selection state
        if (position == selectedPosition) {
            holder.cardView.setStrokeWidth(4);
            TypedValue typedValue = new TypedValue();
            holder.itemView.getContext().getTheme().resolveAttribute(
                    com.google.android.material.R.attr.colorOnPrimary, typedValue, true);
            holder.cardView.setStrokeColor(typedValue.data);
        } else {
            holder.cardView.setStrokeWidth(0);
        }

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            int oldPosition = selectedPosition;
            selectedPosition = pos;
            notifyItemChanged(oldPosition);
            notifyItemChanged(selectedPosition);
            if (listener != null) {
                listener.onEmojiSelected(selectedPosition, emojis[selectedPosition]);
            }
        });
    }

    @Override
    public int getItemCount() {
        return emojis.length;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView emojiText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            emojiText = itemView.findViewById(R.id.emojiText);
        }
    }
}
