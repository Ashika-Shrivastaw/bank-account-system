package project.io.bank_account_system.entity;

public class Transaction {
    private String transactionId;
    private double amount;

    public Transaction() {}
    public Transaction(String transactionId, double amount){
        this.transactionId = transactionId;
        this.amount = amount;
    }

    public String getTransactionId(){
        return transactionId;
    }
    public double getAmount(){
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
