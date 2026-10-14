package uz.jtscorp.xarajatlar.ui.history;

import android.content.Context;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import uz.jtscorp.xarajatlar.R;
import uz.jtscorp.xarajatlar.data.database.AppDatabase;
import uz.jtscorp.xarajatlar.data.entity.Category;
import uz.jtscorp.xarajatlar.data.entity.FamilyMember;
import uz.jtscorp.xarajatlar.data.entity.IncomeCategory;
import uz.jtscorp.xarajatlar.data.entity.PaymentMethod;

public class HistoryFilterBottomSheet extends BottomSheetDialogFragment {
    public static final String RESULT_KEY = "history_filter_result";
    public static final String RESULT_FILTER = "filter";
    public static final String RESULT_SORT = "sort";
    private static final String ARG_FILTER = "current_filter";
    private static final String ARG_SORT = "current_sort";
    private static final String STATE_KIND_INDEX = "kind_index";
    private static final String STATE_SORT_INDEX = "sort_index";
    private static final String STATE_START = "start_date";
    private static final String STATE_END = "end_date";
    private static final String STATE_CATEGORY = "category_choice";
    private static final String STATE_FAMILY = "family_choice";
    private static final String STATE_PAYMENT = "payment_choice";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private TransactionFilter currentFilter;
    private TransactionSort currentSort;
    private AutoCompleteTextView kindInput;
    private AutoCompleteTextView categoryInput;
    private AutoCompleteTextView familyInput;
    private AutoCompleteTextView paymentInput;
    private AutoCompleteTextView sortInput;
    private TextInputEditText startInput;
    private TextInputEditText endInput;
    private TextInputEditText minInput;
    private TextInputEditText maxInput;
    private TextInputLayout startLayout;
    private TextInputLayout endLayout;
    private TextInputLayout minLayout;
    private TextInputLayout maxLayout;
    private MaterialButton applyButton;
    private TextView lookupStatus;
    private LocalDate selectedStart;
    private LocalDate selectedEnd;
    private LookupSelection lookupSelection;
    private int kindIndex;
    private int sortIndex;

    public static HistoryFilterBottomSheet newInstance(TransactionFilter filter,
                                                       TransactionSort sort) {
        HistoryFilterBottomSheet sheet = new HistoryFilterBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_FILTER, filter);
        args.putSerializable(ARG_SORT, sort);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_history_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = requireArguments();
        currentFilter = args.getSerializable(ARG_FILTER, TransactionFilter.class);
        currentSort = args.getSerializable(ARG_SORT, TransactionSort.class);
        if (currentFilter == null) currentFilter = TransactionFilter.empty();
        if (currentSort == null) currentSort = TransactionSort.NEWEST;
        lookupSelection = new LookupSelection(currentFilter);

        bindViews(view);
        setLookupControlsEnabled(false);
        setupStaticDropdowns();
        showCurrentValues();
        setupDatePickers();
        applyButton.setOnClickListener(v -> apply());
        view.findViewById(R.id.filterReset).setOnClickListener(v -> returnResult(
                TransactionFilter.empty(), TransactionSort.NEWEST));
        loadLookupChoices();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_KIND_INDEX, kindIndex);
        outState.putInt(STATE_SORT_INDEX, sortIndex);
        if (selectedStart != null) outState.putLong(STATE_START, selectedStart.toEpochDay());
        if (selectedEnd != null) outState.putLong(STATE_END, selectedEnd.toEpochDay());
        outState.putSerializable(STATE_CATEGORY, lookupSelection.category);
        outState.putSerializable(STATE_FAMILY, lookupSelection.family);
        outState.putSerializable(STATE_PAYMENT, lookupSelection.payment);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState == null) return;
        // Tiklangan matn eski tilda bo'lishi mumkin: tanlov pozitsiyadan qayta chiziladi.
        kindIndex = savedInstanceState.getInt(STATE_KIND_INDEX, kindIndex);
        sortIndex = savedInstanceState.getInt(STATE_SORT_INDEX, sortIndex);
        kindInput.setText(kindInput.getAdapter().getItem(kindIndex).toString(), false);
        sortInput.setText(sortInput.getAdapter().getItem(sortIndex).toString(), false);
        // Qo'llanmagan sana va tanlovlar ham saqlanadi (yo'q kalit = tozalangan sana).
        selectedStart = savedInstanceState.containsKey(STATE_START)
                ? LocalDate.ofEpochDay(savedInstanceState.getLong(STATE_START)) : null;
        selectedEnd = savedInstanceState.containsKey(STATE_END)
                ? LocalDate.ofEpochDay(savedInstanceState.getLong(STATE_END)) : null;
        startInput.setText(selectedStart == null ? "" : DATE_FORMAT.format(selectedStart));
        endInput.setText(selectedEnd == null ? "" : DATE_FORMAT.format(selectedEnd));
        lookupSelection.restore(
                savedInstanceState.getSerializable(STATE_CATEGORY, Choice.class),
                savedInstanceState.getSerializable(STATE_FAMILY, Choice.class),
                savedInstanceState.getSerializable(STATE_PAYMENT, Choice.class));
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) sheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
        }
    }

    private void bindViews(View view) {
        kindInput = view.findViewById(R.id.filterKind);
        categoryInput = view.findViewById(R.id.filterCategory);
        familyInput = view.findViewById(R.id.filterFamily);
        paymentInput = view.findViewById(R.id.filterPayment);
        sortInput = view.findViewById(R.id.filterSort);
        startInput = view.findViewById(R.id.filterStart);
        endInput = view.findViewById(R.id.filterEnd);
        minInput = view.findViewById(R.id.filterMin);
        maxInput = view.findViewById(R.id.filterMax);
        startLayout = view.findViewById(R.id.filterStartLayout);
        endLayout = view.findViewById(R.id.filterEndLayout);
        minLayout = view.findViewById(R.id.filterMinLayout);
        maxLayout = view.findViewById(R.id.filterMaxLayout);
        applyButton = view.findViewById(R.id.filterApply);
        lookupStatus = view.findViewById(R.id.filterLookupStatus);
    }

    private void setupStaticDropdowns() {
        setDropdown(kindInput, List.of(getString(R.string.ui_barchasi),
                getString(R.string.expense_label), getString(R.string.ui_daromad)));
        setDropdown(sortInput, List.of(getString(R.string.sort_newest),
                getString(R.string.sort_oldest), getString(R.string.sort_amount_desc),
                getString(R.string.sort_amount_asc)));
        kindInput.setOnItemClickListener((parent, view, position, id) -> kindIndex = position);
        sortInput.setOnItemClickListener((parent, view, position, id) -> sortIndex = position);
    }

    private void showCurrentValues() {
        kindIndex = TransactionFilter.KIND_EXPENSE.equals(currentFilter.getKind()) ? 1
                : TransactionFilter.KIND_INCOME.equals(currentFilter.getKind()) ? 2 : 0;
        kindInput.setText(kindInput.getAdapter().getItem(kindIndex).toString(), false);
        sortIndex = switch (currentSort) {
            case NEWEST -> 0;
            case OLDEST -> 1;
            case AMOUNT_DESC -> 2;
            case AMOUNT_ASC -> 3;
        };
        sortInput.setText(sortInput.getAdapter().getItem(sortIndex).toString(), false);
        ZoneId zone = ZoneId.systemDefault();
        if (currentFilter.getStartInclusive() != null) {
            selectedStart = Instant.ofEpochMilli(currentFilter.getStartInclusive()).atZone(zone).toLocalDate();
            startInput.setText(DATE_FORMAT.format(selectedStart));
        }
        if (currentFilter.getEndExclusive() != null) {
            selectedEnd = Instant.ofEpochMilli(currentFilter.getEndExclusive()).atZone(zone).toLocalDate().minusDays(1);
            endInput.setText(DATE_FORMAT.format(selectedEnd));
        }
        if (currentFilter.getMinAmount() != null) minInput.setText(String.valueOf(currentFilter.getMinAmount()));
        if (currentFilter.getMaxAmount() != null) maxInput.setText(String.valueOf(currentFilter.getMaxAmount()));
    }

    private void setupDatePickers() {
        startInput.setOnClickListener(v -> showDatePicker(true));
        endInput.setOnClickListener(v -> showDatePicker(false));
        startInput.setOnLongClickListener(v -> { selectedStart = null; startInput.setText(""); return true; });
        endInput.setOnLongClickListener(v -> { selectedEnd = null; endInput.setText(""); return true; });
    }

    private void showDatePicker(boolean start) {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(start ? R.string.filter_start_date : R.string.filter_end_date)
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            LocalDate date = Instant.ofEpochMilli(selection).atZone(ZoneOffset.UTC).toLocalDate();
            if (start) {
                selectedStart = date;
                startInput.setText(DATE_FORMAT.format(date));
            } else {
                selectedEnd = date;
                endInput.setText(DATE_FORMAT.format(date));
            }
        });
        picker.show(getParentFragmentManager(), start ? "historyStart" : "historyEnd");
    }

    private void loadLookupChoices() {
        AppDatabase database = AppDatabase.getDatabase(requireContext());
        View root = requireView();
        Context context = requireContext().getApplicationContext();
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<Choice> categories = categoryChoices(context,
                        database.categoryDao().getAllSync(),
                        database.incomeCategoryDao().getAllSync());
                List<Choice> family = relationChoices(context, database.familyMemberDao().getAllSync());
                List<Choice> payment = paymentChoices(context, database.paymentMethodDao().getAllSync());
                root.post(() -> showLookupChoices(root, categories, family, payment));
            } catch (Throwable error) {
                root.post(() -> showLookupError(root));
            }
        });
    }

    private void showLookupChoices(View expectedRoot, List<Choice> categories,
                                   List<Choice> family, List<Choice> payment) {
        if (getView() != expectedRoot) return;
        lookupSelection.complete(requireContext(), categories, family, payment);
        setChoiceDropdown(categoryInput, categories, lookupSelection.category,
                choice -> lookupSelection.category = choice);
        setChoiceDropdown(familyInput, family, lookupSelection.family,
                choice -> lookupSelection.family = choice);
        setChoiceDropdown(paymentInput, payment, lookupSelection.payment,
                choice -> lookupSelection.payment = choice);
        lookupStatus.setVisibility(View.GONE);
        setLookupControlsEnabled(true);
    }

    private void showLookupError(View expectedRoot) {
        if (getView() != expectedRoot) return;
        lookupSelection.fail();
        lookupStatus.setText(R.string.filter_lookup_failed);
        lookupStatus.setVisibility(View.VISIBLE);
        setLookupControlsEnabled(false);
    }

    private void setLookupControlsEnabled(boolean enabled) {
        categoryInput.setEnabled(enabled);
        familyInput.setEnabled(enabled);
        paymentInput.setEnabled(enabled);
        applyButton.setEnabled(enabled);
    }

    private void apply() {
        if (!lookupSelection.canApply()) return;
        clearErrors();
        Double min = parseAmount(minInput, minLayout);
        Double max = parseAmount(maxInput, maxLayout);
        if (minLayout.getError() != null || maxLayout.getError() != null) return;
        if (min != null && max != null && min > max) {
            minLayout.setError(getString(R.string.filter_min_gt_max));
            return;
        }
        if (selectedStart != null && selectedEnd != null && selectedStart.isAfter(selectedEnd)) {
            startLayout.setError(getString(R.string.filter_start_after_end));
            endLayout.setError(getString(R.string.filter_check_range));
            return;
        }

        String kind = kindAt(kindIndex);
        Choice category = lookupSelection.category;
        Choice family = lookupSelection.family;
        Choice payment = lookupSelection.payment;
        ZoneId zone = ZoneId.systemDefault();
        TransactionFilter.Builder builder = currentFilter.buildUpon()
                .kind(kind)
                .startInclusive(selectedStart == null ? null
                        : selectedStart.atStartOfDay(zone).toInstant().toEpochMilli())
                .endExclusive(selectedEnd == null ? null
                        : selectedEnd.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli())
                .category(category.kind, category.id)
                .familyMemberId(family.id)
                .paymentMethodId(payment.id)
                .minAmount(min)
                .maxAmount(max);
        TransactionSort sort = sortAt(sortIndex);
        try {
            returnResult(builder.build(), sort);
        } catch (IllegalArgumentException error) {
            minLayout.setError(getString(R.string.filter_invalid));
        }
    }

    private void returnResult(TransactionFilter filter, TransactionSort sort) {
        Bundle result = new Bundle();
        result.putSerializable(RESULT_FILTER, filter);
        result.putSerializable(RESULT_SORT, sort);
        getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
        dismiss();
    }

    private void clearErrors() {
        startLayout.setError(null); endLayout.setError(null);
        minLayout.setError(null); maxLayout.setError(null);
    }

    private static Double parseAmount(TextInputEditText input, TextInputLayout layout) {
        String text = input.getText() == null ? "" : input.getText().toString().trim();
        if (text.isEmpty()) return null;
        try {
            double value = Double.parseDouble(text);
            if (!Double.isFinite(value) || value < 0) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException error) {
            layout.setError(layout.getContext().getString(R.string.filter_positive_amount));
            return null;
        }
    }

    static String kindAt(int index) {
        return switch (index) {
            case 1 -> TransactionFilter.KIND_EXPENSE;
            case 2 -> TransactionFilter.KIND_INCOME;
            default -> null;
        };
    }

    static TransactionSort sortAt(int index) {
        return switch (index) {
            case 1 -> TransactionSort.OLDEST;
            case 2 -> TransactionSort.AMOUNT_DESC;
            case 3 -> TransactionSort.AMOUNT_ASC;
            default -> TransactionSort.NEWEST;
        };
    }

    private static void setDropdown(AutoCompleteTextView input, List<String> labels) {
        input.setAdapter(new ArrayAdapter<>(input.getContext(), android.R.layout.simple_list_item_1, labels));
    }

    private static void setChoiceDropdown(AutoCompleteTextView input, List<Choice> choices,
                                          Choice selected, Consumer<Choice> onSelected) {
        input.setAdapter(new ArrayAdapter<>(input.getContext(),
                android.R.layout.simple_list_item_1, choices));
        input.setText(selected.label, false);
        input.setOnItemClickListener((parent, view, position, id) ->
                onSelected.accept((Choice) parent.getItemAtPosition(position)));
    }

    static List<Choice> categoryChoices(Context context, List<Category> expense,
                                        List<IncomeCategory> income) {
        String expenseLabel = context.getString(R.string.expense_label);
        String incomeLabel = context.getString(R.string.ui_daromad);
        String unassigned = context.getString(R.string.unassigned);
        List<Choice> values = new ArrayList<>();
        values.add(new Choice(context.getString(R.string.filter_all_categories), null, null));
        values.add(new Choice(context.getString(R.string.labeled_value, expenseLabel, unassigned),
                TransactionFilter.KIND_EXPENSE, 0));
        for (Category value : expense) {
            values.add(new Choice(context.getString(R.string.labeled_value, expenseLabel, value.getName()),
                    TransactionFilter.KIND_EXPENSE, value.getId()));
        }
        values.add(new Choice(context.getString(R.string.labeled_value, incomeLabel, unassigned),
                TransactionFilter.KIND_INCOME, 0));
        for (IncomeCategory value : income) {
            values.add(new Choice(context.getString(R.string.labeled_value, incomeLabel, value.getName()),
                    TransactionFilter.KIND_INCOME, value.getId()));
        }
        return disambiguateDuplicateLabels(values);
    }

    static List<Choice> relationChoices(Context context, List<FamilyMember> items) {
        List<Choice> values = new ArrayList<>();
        values.add(new Choice(context.getString(R.string.ui_barchasi), null, null));
        values.add(new Choice(context.getString(R.string.unassigned), null, 0));
        for (FamilyMember value : items) values.add(new Choice(value.getName(), null, value.getId()));
        return disambiguateDuplicateLabels(values);
    }

    static List<Choice> paymentChoices(Context context, List<PaymentMethod> items) {
        List<Choice> values = new ArrayList<>();
        values.add(new Choice(context.getString(R.string.ui_barchasi), null, null));
        values.add(new Choice(context.getString(R.string.unassigned), null, 0));
        for (PaymentMethod value : items) values.add(new Choice(value.getName(), null, value.getId()));
        return disambiguateDuplicateLabels(values);
    }

    static Choice findChoice(List<Choice> choices, String kind, Integer id) {
        for (Choice choice : choices) {
            if (equals(choice.kind, kind) && equals(choice.id, id)) return choice;
        }
        return null;
    }

    private static List<Choice> disambiguateDuplicateLabels(List<Choice> choices) {
        Map<String, Integer> counts = new HashMap<>();
        for (Choice choice : choices) counts.merge(choice.label, 1, Integer::sum);
        List<Choice> result = new ArrayList<>(choices.size());
        for (Choice choice : choices) {
            if (counts.get(choice.label) > 1 && choice.id != null) {
                result.add(new Choice(choice.label + " (#" + choice.id + ")",
                        choice.kind, choice.id));
            } else {
                result.add(choice);
            }
        }
        return result;
    }

    private static boolean equals(Object first, Object second) {
        return first == null ? second == null : first.equals(second);
    }

    static final class Choice implements Serializable {
        final String label;
        final String kind;
        final Integer id;

        Choice(String label, String kind, Integer id) {
            this.label = label;
            this.kind = kind;
            this.id = id;
        }

        @NonNull
        @Override
        public String toString() {
            return label;
        }
    }

    static final class LookupSelection {
        Choice category;
        Choice family;
        Choice payment;
        boolean ready;
        boolean failed;

        LookupSelection(TransactionFilter current) {
            category = new Choice("", current.getCategoryKind(), current.getCategoryId());
            family = new Choice("", null, current.getFamilyMemberId());
            payment = new Choice("", null, current.getPaymentMethodId());
        }

        /** Qayta yaratilgandan keyin hali qo'llanmagan tanlovlarni tiklaydi. */
        void restore(Choice savedCategory, Choice savedFamily, Choice savedPayment) {
            if (savedCategory != null) category = savedCategory;
            if (savedFamily != null) family = savedFamily;
            if (savedPayment != null) payment = savedPayment;
        }

        void complete(Context context, List<Choice> categories, List<Choice> familyChoices,
                      List<Choice> paymentChoices) {
            category = resolve(context, categories, category);
            family = resolve(context, familyChoices, family);
            payment = resolve(context, paymentChoices, payment);
            ready = true;
            failed = false;
        }

        void fail() {
            ready = false;
            failed = true;
        }

        boolean canApply() {
            return ready && !failed;
        }

        private static Choice resolve(Context context, List<Choice> choices, Choice current) {
            Choice exact = findChoice(choices, current.kind, current.id);
            if (exact != null) return exact;
            if (current.id != null) {
                return new Choice(context.getString(R.string.filter_selected_id, current.id),
                        current.kind, current.id);
            }
            return choices.get(0);
        }
    }
}
