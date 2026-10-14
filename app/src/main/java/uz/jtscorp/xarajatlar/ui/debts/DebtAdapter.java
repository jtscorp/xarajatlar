package uz.jtscorp.xarajatlar.ui.debts;

import uz.jtscorp.xarajatlar.utils.Formats;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import uz.jtscorp.xarajatlar.R;
import uz.jtscorp.xarajatlar.data.entity.Debt;

public class DebtAdapter extends ListAdapter<Debt, DebtAdapter.DebtViewHolder> {


    private OnDebtClickListener listener;

    public interface OnDebtClickListener {
        void onDebtClick(Debt debt);
    }

    public DebtAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnDebtClickListener(OnDebtClickListener listener) {
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Debt> DIFF_CALLBACK = new DiffUtil.ItemCallback<Debt>() {
        @Override
        public boolean areItemsTheSame(@NonNull Debt oldItem, @NonNull Debt newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Debt oldItem, @NonNull Debt newItem) {
            return oldItem.getAmount() == newItem.getAmount() &&
                   oldItem.getPaidAmount() == newItem.getPaidAmount();
        }
    };

    @NonNull
    @Override
    public DebtViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_debt, parent, false);
        return new DebtViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DebtViewHolder holder, int position) {
        Debt debt = getItem(position);
        holder.bind(debt);
    }

    class DebtViewHolder extends RecyclerView.ViewHolder {
        private final TextView personName;
        private final TextView debtAmount;
        private final TextView dueDate;
        private final TextView remainingAmount;

        DebtViewHolder(@NonNull View itemView) {
            super(itemView);
            personName = itemView.findViewById(R.id.personName);
            debtAmount = itemView.findViewById(R.id.debtAmount);
            dueDate = itemView.findViewById(R.id.dueDate);
            remainingAmount = itemView.findViewById(R.id.remainingAmount);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onDebtClick(getItem(position));
                }
            });
        }

        void bind(Debt debt) {
            personName.setText(debt.getPersonName());
            debtAmount.setText(Formats.money(itemView.getContext(), debt.getAmount()));

            if (debt.getDueDate() > 0) {
                dueDate.setText(itemView.getContext().getString(R.string.debt_due,
                        Formats.date(debt.getDueDate())));
                dueDate.setVisibility(View.VISIBLE);
            } else {
                dueDate.setVisibility(View.GONE);
            }

            double remaining = debt.getRemainingAmount();
            if (remaining > 0) {
                remainingAmount.setText(itemView.getContext().getString(R.string.debt_remaining,
                        Formats.money(itemView.getContext(), remaining)));
                remainingAmount.setVisibility(View.VISIBLE);
            } else {
                remainingAmount.setVisibility(View.GONE);
            }
        }
    }
}
