package uz.jtscorp.xarajatlar.data.dao;

import androidx.room.Dao;
import androidx.room.RawQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.util.List;

import uz.jtscorp.xarajatlar.data.entity.Category;
import uz.jtscorp.xarajatlar.data.entity.Expense;
import uz.jtscorp.xarajatlar.data.entity.FamilyMember;
import uz.jtscorp.xarajatlar.data.entity.Income;
import uz.jtscorp.xarajatlar.data.entity.IncomeCategory;
import uz.jtscorp.xarajatlar.data.entity.PaymentMethod;
import uz.jtscorp.xarajatlar.data.model.TransactionRow;
import uz.jtscorp.xarajatlar.data.model.TransactionSummary;

@Dao
public interface TransactionDao {
    @RawQuery(observedEntities = {Expense.class, Income.class, Category.class,
            IncomeCategory.class, FamilyMember.class, PaymentMethod.class})
    List<uz.jtscorp.xarajatlar.data.model.TransactionGroup> queryGroups(SupportSQLiteQuery query);

    @RawQuery(observedEntities = {Expense.class, Income.class, Category.class,
            IncomeCategory.class, FamilyMember.class, PaymentMethod.class})
    List<TransactionRow> queryRows(SupportSQLiteQuery query);

    @RawQuery(observedEntities = {Expense.class, Income.class, Category.class,
            IncomeCategory.class, FamilyMember.class, PaymentMethod.class})
    TransactionRow queryOne(SupportSQLiteQuery query);

    @RawQuery(observedEntities = {Expense.class, Income.class, Category.class,
            IncomeCategory.class, FamilyMember.class, PaymentMethod.class})
    TransactionSummary querySummary(SupportSQLiteQuery query);
}
