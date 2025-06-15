package project.io.bank_account_system.service.BalanceTracker.impl;

import project.io.bank_account_system.entity.Transaction;
import project.io.bank_account_system.model.Audit.Batch;
import project.io.bank_account_system.service.AuditSystem.AuditSystem;
import project.io.bank_account_system.service.BalanceTracker.BankAccountService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BankAccountServiceImpl implements BankAccountService {
    private double balance = 0.0;
    private List<Transaction> listOfCurrentTransactions = new ArrayList<>();
    private AuditSystem auditSystem;

    private final ExecutorService processForAudit = Executors.newSingleThreadExecutor();
    public BankAccountServiceImpl(AuditSystem auditSystem){
        this.auditSystem = auditSystem;
    }

    //Added below for testing as the submission was being processed asynchronously via processForAudit.submit()
    //and the test cases were fetching getList() before the submission was complete
    public void awaitAuditProcessing() throws InterruptedException{
        if (!listOfCurrentTransactions.isEmpty()) {
            System.out.println("Flushing " + listOfCurrentTransactions.size() + " remaining transactions");
            List<Transaction> remainingTransactions = new ArrayList<>(listOfCurrentTransactions);
            listOfCurrentTransactions.clear();
            processForAudit.submit(() -> processSubmittedAuditDetails(remainingTransactions));
        }
        //Shutting down executor and wait for tasks to complete
        processForAudit.shutdown();
        if(!processForAudit.awaitTermination(1, TimeUnit.SECONDS))
            System.err.println("Audit processor did not terminate in time");
    }

    public synchronized void reset() {
        listOfCurrentTransactions.clear();
        balance = 0.0;
        System.out.println("Service reset");
    }
    @Override
    public synchronized void processTransaction(Transaction transaction) {
        //System.out.println("Transaction: ID=" + transaction.getTransactionId() + ", Amount=" + transaction.getAmount() + ", Count=" + (listOfCurrentTransactions.size() + 1));
        listOfCurrentTransactions.add(transaction);
        balance = balance + transaction.getAmount();
        System.out.println("Transaction added, current size: "+ listOfCurrentTransactions.size() + " and balance is : " + balance);
        if(listOfCurrentTransactions.size() == 1000){
            System.out.println("Reached 1000 transactions, submitting....");
            List<Transaction> transactionsForAudit = new ArrayList<>(listOfCurrentTransactions);
            listOfCurrentTransactions.clear();
            processForAudit.submit(new Runnable() {
                @Override
                public void run() {
                    processSubmittedAuditDetails(transactionsForAudit);
                }
            });
        }
    }

    private void processSubmittedAuditDetails(List<Transaction> transactionsForAudit) {
        transactionsForAudit.sort((t1, t2) ->
            Double.compare(Math.abs(t2.getAmount()), Math.abs(t1.getAmount()))
        );
        TreeSet<Batch> batches = new TreeSet<>((b1, b2) -> {
            int cmp = Double.compare(b1.fetchRemainingCapacityofBatch(), b2.fetchRemainingCapacityofBatch());
            if (cmp != 0) return cmp;
            // breaking ties so two different batches with the same capacity are distinct
            return System.identityHashCode(b1) - System.identityHashCode(b2);
        });

        for(Transaction eachTransaction : transactionsForAudit){
            double absAmt = Math.abs(eachTransaction.getAmount());
            Batch remainingBatches = new Batch() {
                public double fetchRemainingCapacityofBatch(){
                    return absAmt;
                }
            };

            Batch bestBatch = batches.ceiling(remainingBatches);
            if(bestBatch!=null && bestBatch.checkTransactionLimit(eachTransaction))
            {
                batches.remove(bestBatch);
                bestBatch.addTransactions(eachTransaction);
                batches.add(bestBatch);
            } else {
                Batch newBatch = new Batch();
                newBatch.addTransactions(eachTransaction);
                batches.add(newBatch);
            }
        }
        System.out.println("Submitting : " + batches.size() + " batches to the audit system");
        for (Batch batch : batches) {
            System.out.println("Batch: Count=" + batch.getTransactionCount() + ", Total=" + batch.getTotalValueOfTransactions());
        }
        auditSystem.submit(new ArrayList<>(batches));
    }
    @Override
    public synchronized double retrieveBalance() {
        return balance;
    }
}
