package uz.jtscorp.xarajatlar.ui.home;

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
import uz.jtscorp.xarajatlar.data.entity.Income;
import uz.jtscorp.xarajatlar.data.entity.FamilyMember;
import uz.jtscorp.xarajatlar.data.entity.IncomeCategory;
import uz.jtscorp.xarajatlar.data.entity.PaymentMethod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IncomeAdapter extends ListAdapter<Income, IncomeAdapter.IncomeViewHolder> {

    private OnIncomeClickListener listener;
    private OnIncomeLongClickListener longClickListener;
    private Map<Integer, IncomeCategory> categoryMap = new HashMap<>();
    private Map<Integer, PaymentMethod> paymentMethodMap = new HashMap<>();
    private Map<Integer, FamilyMember> familyMemberMap = new HashMap<>();

    public interface OnIncomeClickListener {
        void onIncomeClick(Income income);
    }

    public interface OnIncomeLongClickListener {
        boolean onIncomeLongClick(Income income);
    }

    public IncomeAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setCategories(List<IncomeCategory> categories) {
        categoryMap.clear();
        if (categories != null) {
            for (IncomeCategory category : categories) {
                categoryMap.put(category.getId(), category);
            }
        }
        notifyDataSetChanged();
    }

    public void setPaymentMethods(List<PaymentMethod> methods) {
        paymentMethodMap.clear();
        if (methods != null) {
            for (PaymentMethod method : methods) {
                paymentMethodMap.put(method.getId(), method);
            }
        }
        notifyDataSetChanged();
    }

    public void setFamilyMembers(List<FamilyMember> members) {
        familyMemberMap.clear();
        if (members != null) for (FamilyMember member : members) familyMemberMap.put(member.getId(), member);
        notifyDataSetChanged();
    }

    public void setOnIncomeClickListener(OnIncomeClickListener listener) {
        this.listener = listener;
    }

    public void setOnIncomeLongClickListener(OnIncomeLongClickListener listener) {
        this.longClickListener = listener;
    }

    private static final DiffUtil.ItemCallback<Income> DIFF_CALLBACK = new DiffUtil.ItemCallback<Income>() {
        @Override
        public boolean areItemsTheSame(@NonNull Income oldItem, @NonNull Income newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Income oldItem, @NonNull Income newItem) {
            return oldItem.getAmount() == newItem.getAmount() &&
                   oldItem.getTimestamp() == newItem.getTimestamp();
        }
    };

    @NonNull
    @Override
    public IncomeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_income, parent, false);
        return new IncomeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IncomeViewHolder holder, int position) {
        Income income = getItem(position);
        holder.bind(income);
    }

    class IncomeViewHolder extends RecyclerView.ViewHolder {
        private final TextView incomeEmoji;
        private final TextView incomeDescription;
        private final TextView incomeDetails;
        private final TextView incomeAmount;

        IncomeViewHolder(@NonNull View itemView) {
            super(itemView);
            incomeEmoji = itemView.findViewById(R.id.incomeEmoji);
            incomeDescription = itemView.findViewById(R.id.incomeDescription);
            incomeDetails = itemView.findViewById(R.id.incomeDetails);
            incomeAmount = itemView.findViewById(R.id.incomeAmount);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onIncomeClick(getItem(position));
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && longClickListener != null) {
                    return longClickListener.onIncomeLongClick(getItem(position));
                }
                return false;
            });
        }

        void bind(Income income) {
            // Emoji from category map
            String emoji = "\uD83D\uDCB0"; // Default money bag emoji
            Integer categoryId = income.getIncomeCategoryId();
            if (categoryId != null && categoryMap.containsKey(categoryId)) {
                IncomeCategory category = categoryMap.get(categoryId);
                if (category != null && category.getEmoji() != null && !category.getEmoji().isEmpty()) {
                    emoji = category.getEmoji();
                }
            }
            incomeEmoji.setText(emoji);

            // Description - prefer source, then description, then category name
            String description = income.getSource();
            if (description == null || description.isEmpty()) {
                description = income.getDescription();
            }
            if (description == null || description.isEmpty()) {
                if (categoryId != null && categoryMap.containsKey(categoryId)) {
                    IncomeCategory category = categoryMap.get(categoryId);
                    if (category != null) {
                        description = category.getName();
                    }
                }
            }
            if (description == null || description.isEmpty()) {
                description = itemView.getContext().getString(R.string.ui_daromad);
            }
            incomeDescription.setText(description);

            // Date + payment method formatting
            String dateStr = Formats.dateTime(income.getTimestamp());
            FamilyMember member = income.getFamilyMemberId() == null
                    ? null : familyMemberMap.get(income.getFamilyMemberId());
            if (member != null) dateStr += " · " + member.getName();
            Integer pmId = income.getPaymentMethodId();
            if (pmId != null && paymentMethodMap.containsKey(pmId)) {
                PaymentMethod pm = paymentMethodMap.get(pmId);
                if (pm != null) {
                    String pmEmoji = pm.isCash() ? "\uD83D\uDCB5" : "\uD83D\uDCB3"; // 💵 or 💳
                    String pmName = pm.getName();
                    if (pm.getLastFourDigits() != null && !pm.getLastFourDigits().isEmpty()) {
                        pmName += " *" + pm.getLastFourDigits();
                    }
                    dateStr += " · " + pmEmoji + " " + pmName;
                }
            }
            incomeDetails.setText(dateStr);

            // Amount formatting with "+" prefix
            double amount = income.getAmount();
            String amountStr;
            if (amount > 0) {
                amountStr = Formats.signedMoney(itemView.getContext(), "+", (long) amount);
            } else {
                amountStr = Formats.money(itemView.getContext(), 0);
            }
            incomeAmount.setText(amountStr);
        }
    }
}
