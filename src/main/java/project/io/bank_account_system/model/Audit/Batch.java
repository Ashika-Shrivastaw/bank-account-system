package project.io.bank_account_system.model.Audit;

import project.io.bank_account_system.entity.Transaction;

import java.util.ArrayList;
import java.util.List;

public class Batch {

    private List<Transaction> transactions = new ArrayList<>();
    private double totalValue = 0.0;
    private double totalAbsoluteValue = 0.0;

    public boolean checkTransactionLimit(Transaction t){
        double newAbsoluteTotal = totalAbsoluteValue + Math.abs(t.getAmount());
        return newAbsoluteTotal <= 1000000.0;
    }

    public void addTransactions(Transaction tx){
        transactions.add(tx);
        totalValue += tx.getAmount();
        totalAbsoluteValue += Math.abs(tx.getAmount());
    }

    public int getTransactionCount(){
        return transactions.size();
    }

    public double getTotalValueOfTransactions(){
        //System.out.println("Actual balance total: " + totalValue);
        return totalAbsoluteValue;
    }

    public double fetchRemainingCapacityofBatch(){
        return 1000000.0 - totalAbsoluteValue;
    }
}
