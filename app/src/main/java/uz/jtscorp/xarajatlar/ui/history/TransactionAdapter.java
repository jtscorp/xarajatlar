package uz.jtscorp.xarajatlar.ui.history;

import android.content.Context;
import uz.jtscorp.xarajatlar.utils.Formats;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import uz.jtscorp.xarajatlar.R;
import uz.jtscorp.xarajatlar.data.model.TransactionRow;

public class TransactionAdapter extends ListAdapter<TransactionRow, TransactionAdapter.ViewHolder> {
    public interface OnTransactionClickListener {
        void onTransactionClick(TransactionRow row);
    }

    private static final DiffUtil.ItemCallback<TransactionRow> DIFF =
            new DiffUtil.ItemCallback<TransactionRow>() {
                @Override
                public boolean areItemsTheSame(@NonNull TransactionRow oldItem,
                                               @NonNull TransactionRow newItem) {
                    return oldItem.getId() == newItem.getId()
                            && oldItem.getKind().equals(newItem.getKind());
                }

                @Override
                public boolean areContentsTheSame(@NonNull TransactionRow oldItem,
                                                  @NonNull TransactionRow newItem) {
                    return Double.compare(oldItem.getAmount(), newItem.getAmount()) == 0
                            && oldItem.getTimestamp() == newItem.getTimestamp()
                            && Objects.equals(oldItem.getCategoryId(), newItem.getCategoryId())
                            && Objects.equals(oldItem.getFamilyMemberId(), newItem.getFamilyMemberId())
                            && Objects.equals(oldItem.getPaymentMethodId(), newItem.getPaymentMethodId())
                            && safeEquals(oldItem.getDescription(), newItem.getDescription())
                            && safeEquals(oldItem.getSourceName(), newItem.getSourceName())
                            && safeEquals(oldItem.getReceiptUrl(), newItem.getReceiptUrl())
                            && safeEquals(oldItem.getCategoryName(), newItem.getCategoryName())
                            && safeEquals(oldItem.getCategoryIcon(), newItem.getCategoryIcon())
                            && oldItem.getCategoryColor() == newItem.getCategoryColor()
                            && safeEquals(oldItem.getFamilyMemberName(), newItem.getFamilyMemberName())
                            && safeEquals(oldItem.getPaymentMethodName(), newItem.getPaymentMethodName());
                }
            };

    private final OnTransactionClickListener listener;

    public TransactionAdapter(OnTransactionClickListener listener) {
        super(DIFF);
        this.listener = listener;
        setHasStableIds(true);
    }

    public TransactionRow rowAt(int position) { return getItem(position); }

    @Override
    public long getItemId(int position) {
        TransactionRow row = getItem(position);
        return ((long) row.getId() << 1) | (row.isIncome() ? 1L : 0L);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransactionRow row = getItem(position);
        holder.icon.setText(nonEmpty(row.getCategoryIcon(), row.isIncome() ? "↗" : "↙"));
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        int color = row.getCategoryColor() != 0
                ? row.getCategoryColor() : holder.itemView.getContext().getColor(
                        row.isIncome() ? R.color.history_income : R.color.history_expense);
        background.setColor(withAlpha(color, 0x28));
        holder.icon.setBackground(background);
        holder.title.setText(firstNonEmpty(row.getDescription(), row.getSourceName(),
                row.getCategoryName(), holder.itemView.getContext().getString(
                        row.isIncome() ? R.string.ui_daromad : R.string.expense_label)));
        holder.details.setText(details(holder.itemView.getContext(), row));
        String sign = row.isIncome() ? "+" : "−";
        holder.amount.setText(Formats.signedMoney(holder.itemView.getContext(), sign, row.getAmount()));
        holder.amount.setTextColor(holder.itemView.getContext().getColor(
                row.isIncome() ? R.color.history_income : R.color.history_expense));
        holder.itemView.setOnClickListener(view -> listener.onTransactionClick(row));
    }

    private static String details(Context context, TransactionRow row) {
        List<String> parts = new ArrayList<>();
        addIfNotEmpty(parts, row.getCategoryName());
        addIfNotEmpty(parts, row.getFamilyMemberName());
        addIfNotEmpty(parts, row.getPaymentMethodName());
        parts.add(Formats.dayMonthTime(context,
                Instant.ofEpochMilli(row.getTimestamp()).atZone(ZoneId.systemDefault())));
        return String.join(" • ", parts);
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private static void addIfNotEmpty(List<String> target, String value) {
        if (value != null && !value.trim().isEmpty()) target.add(value);
    }

    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value;
        }
        return "";
    }

    private static String nonEmpty(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private static boolean safeEquals(Object first, Object second) {
        return first == null ? second == null : first.equals(second);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView icon;
        final TextView title;
        final TextView details;
        final TextView amount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.transactionIcon);
            title = itemView.findViewById(R.id.transactionTitle);
            details = itemView.findViewById(R.id.transactionDetails);
            amount = itemView.findViewById(R.id.transactionAmount);
        }
    }
}
