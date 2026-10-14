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

import uz.jtscorp.xarajatlar.R;
import uz.jtscorp.xarajatlar.data.entity.PaymentMethod;

public class PaymentMethodAdapter extends ListAdapter<PaymentMethod, PaymentMethodAdapter.ViewHolder> {

    private final OnItemClickListener onItemClick;
    private final OnDeleteClickListener onDeleteClick;

    public interface OnItemClickListener {
        void onItemClick(PaymentMethod method);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(PaymentMethod method);
    }

    public PaymentMethodAdapter(OnItemClickListener onItemClick, OnDeleteClickListener onDeleteClick) {
        super(DIFF_CALLBACK);
        this.onItemClick = onItemClick;
        this.onDeleteClick = onDeleteClick;
    }

    private static final DiffUtil.ItemCallback<PaymentMethod> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<PaymentMethod>() {
                @Override
                public boolean areItemsTheSame(@NonNull PaymentMethod oldItem, @NonNull PaymentMethod newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull PaymentMethod oldItem, @NonNull PaymentMethod newItem) {
                    return oldItem.getName().equals(newItem.getName()) &&
                            oldItem.getColor() == newItem.getColor();
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
        PaymentMethod method = getItem(position);
        holder.bind(method);
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

        void bind(PaymentMethod method) {
            titleText.setText(method.getName());

            // Show payment method emoji
            String emoji = method.isCash() ? "\uD83D\uDCB5" : "\uD83D\uDCB3";
            emojiText.setText(emoji);
            emojiText.setVisibility(View.VISIBLE);

            // Show last 4 digits if available
            if (method.getLastFourDigits() != null && !method.getLastFourDigits().isEmpty()) {
                subtitleText.setText(itemView.getContext().getString(
                        R.string.card_masked_digits, method.getLastFourDigits()));
                subtitleText.setVisibility(View.VISIBLE);
            } else {
                subtitleText.setVisibility(View.GONE);
            }

            // Set color
            GradientDrawable drawable = (GradientDrawable) colorIndicator.getBackground();
            drawable.setColor(method.getColor() != 0 ? method.getColor() : 0xFF4CAF50);

            itemView.setOnClickListener(v -> onItemClick.onItemClick(method));
            deleteButton.setOnClickListener(v -> onDeleteClick.onDeleteClick(method));
        }
    }
}
