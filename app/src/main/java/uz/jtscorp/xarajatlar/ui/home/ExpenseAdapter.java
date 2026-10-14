package uz.jtscorp.xarajatlar.ui.home;

import uz.jtscorp.xarajatlar.utils.Formats;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import uz.jtscorp.xarajatlar.R;
import uz.jtscorp.xarajatlar.data.entity.Category;
import uz.jtscorp.xarajatlar.data.entity.Expense;
import uz.jtscorp.xarajatlar.data.entity.FamilyMember;
import uz.jtscorp.xarajatlar.data.entity.PaymentMethod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpenseAdapter extends ListAdapter<Expense, ExpenseAdapter.ExpenseViewHolder> {

    private OnExpenseClickListener listener;
    private Map<Integer, Category> categoryMap = new HashMap<>();
    private Map<Integer, PaymentMethod> paymentMethodMap = new HashMap<>();
    private Map<Integer, FamilyMember> familyMemberMap = new HashMap<>();

    // Category colors for visual variety
    private static final int[] CATEGORY_COLORS = {
            0xFF4CAF50, 0xFF2196F3, 0xFFFF9800, 0xFF9C27B0,
            0xFFE91E63, 0xFF00BCD4, 0xFF795548, 0xFF607D8B
    };

    public interface OnExpenseClickListener {
        void onExpenseClick(Expense expense);
    }

    public ExpenseAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setCategories(List<Category> categories) {
        categoryMap.clear();
        if (categories != null) {
            for (Category category : categories) {
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

    public void setOnExpenseClickListener(OnExpenseClickListener listener) {
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Expense> DIFF_CALLBACK = new DiffUtil.ItemCallback<Expense>() {
        @Override
        public boolean areItemsTheSame(@NonNull Expense oldItem, @NonNull Expense newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Expense oldItem, @NonNull Expense newItem) {
            return oldItem.getAmount() == newItem.getAmount() &&
                   oldItem.getTimestamp() == newItem.getTimestamp();
        }
    };

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = getItem(position);
        holder.bind(expense);
    }

    class ExpenseViewHolder extends RecyclerView.ViewHolder {
        private final TextView categoryColor;
        private final TextView expenseDescription;
        private final TextView expenseDetails;
        private final TextView expenseAmount;

        ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryColor = (TextView) itemView.findViewById(R.id.categoryColor);
            expenseDescription = itemView.findViewById(R.id.expenseDescription);
            expenseDetails = itemView.findViewById(R.id.expenseDetails);
            expenseAmount = itemView.findViewById(R.id.expenseAmount);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onExpenseClick(getItem(position));
                }
            });
        }

        void bind(Expense expense) {
            // Description - prefer shop name, then description
            String description = expense.getShopName();
            if (description == null || description.isEmpty()) {
                description = expense.getDescription();
            }
            if (description == null || description.isEmpty()) {
                Category category = expense.getCategoryId() == null ? null : categoryMap.get(expense.getCategoryId());
                description = category != null ? category.getName()
                        : itemView.getContext().getString(R.string.expense_uncategorized);
            }
            expenseDescription.setText(description);

            // Date + payment method formatting
            String dateStr = Formats.dateTime(expense.getTimestamp());
            FamilyMember member = expense.getFamilyMemberId() == null
                    ? null : familyMemberMap.get(expense.getFamilyMemberId());
            if (member != null) dateStr += " · " + member.getName();
            Integer pmId = expense.getPaymentMethodId();
            if (pmId != null && paymentMethodMap.containsKey(pmId)) {
                PaymentMethod pm = paymentMethodMap.get(pmId);
                if (pm != null) {
                    String pmEmoji = pm.isCash() ? "\uD83D\uDCB5" : "\uD83D\uDCB3";
                    String pmName = pm.getName();
                    if (pm.getLastFourDigits() != null && !pm.getLastFourDigits().isEmpty()) {
                        pmName += " *" + pm.getLastFourDigits();
                    }
                    dateStr += " · " + pmEmoji + " " + pmName;
                }
            }
            expenseDetails.setText(dateStr);

            // Amount formatting - don't show if 0
            double amount = expense.getAmount();
            String amountStr;
            if (amount > 0) {
                amountStr = Formats.signedMoney(itemView.getContext(), "-", (long) amount);
            } else {
                amountStr = Formats.money(itemView.getContext(), 0);
            }
            expenseAmount.setText(amountStr);

            // Get category color
            int color = CATEGORY_COLORS[0]; // Default green
            Integer categoryId = expense.getCategoryId();
            if (categoryId != null && categoryMap.containsKey(categoryId)) {
                Category category = categoryMap.get(categoryId);
                if (category != null && category.getColor() != 0) {
                    color = category.getColor();
                }
            } else if (categoryId != null) {
                // Use category ID to pick a color
                color = CATEGORY_COLORS[categoryId % CATEGORY_COLORS.length];
            }

            GradientDrawable drawable = (GradientDrawable) categoryColor.getBackground();
            if (drawable != null) {
                drawable = (GradientDrawable) drawable.mutate();
                drawable.setColor(color);
            }

            // Set category emoji
            String emoji = null;
            if (categoryId != null && categoryMap.containsKey(categoryId)) {
                Category category = categoryMap.get(categoryId);
                if (category != null) {
                    emoji = category.getIconName();
                }
            }
            if (emoji != null && !emoji.isEmpty()) {
                categoryColor.setText(emoji);
            } else {
                String desc = expenseDescription.getText().toString();
                if (!desc.isEmpty()) {
                    categoryColor.setText(desc.substring(0, 1).toUpperCase());
                } else {
                    categoryColor.setText("");
                }
            }
        }
    }
}
