package project.io.bank_account_system.model.Audit;

import org.springframework.beans.factory.annotation.Value;
import project.io.bank_account_system.entity.Transaction;

import java.util.ArrayList;
import java.util.List;

public class Batch {

    private List<Transaction> transactions = new ArrayList<>();
    private double totalAbsoluteValue = 0.0;
    private final double maxAbsoluteValue;
    public Batch(double maxAbsoluteValue) {
        this.maxAbsoluteValue = maxAbsoluteValue;
    }

    public boolean checkTransactionLimit(Transaction t){
        double newAbsoluteTotal = totalAbsoluteValue + Math.abs(t.getAmount());
        return newAbsoluteTotal <= maxAbsoluteValue;
    }

    public void addTransactions(Transaction tx){
        transactions.add(tx);
        totalAbsoluteValue += Math.abs(tx.getAmount());
    }

    public int getTransactionCount(){
        return transactions.size();
    }

    public double getTotalValueOfTransactions(){
        return totalAbsoluteValue;
    }

    public double fetchRemainingCapacityofBatch(){
        return 1000000.0 - totalAbsoluteValue;
    }
}
