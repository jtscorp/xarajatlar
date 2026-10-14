package uz.jtscorp.xarajatlar.data.model;

public class TransactionSummary {
    private int count;
    private double incomeTotal;
    private double expenseTotal;

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
    public double getIncomeTotal() { return incomeTotal; }
    public void setIncomeTotal(double incomeTotal) { this.incomeTotal = incomeTotal; }
    public double getExpenseTotal() { return expenseTotal; }
    public void setExpenseTotal(double expenseTotal) { this.expenseTotal = expenseTotal; }
    public double getBalance() { return incomeTotal - expenseTotal; }
}
