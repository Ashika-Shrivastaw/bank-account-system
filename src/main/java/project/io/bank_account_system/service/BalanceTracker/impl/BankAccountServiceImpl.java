package project.io.bank_account_system.service.BalanceTracker.impl;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import project.io.bank_account_system.entity.Transaction;
import project.io.bank_account_system.model.Audit.Batch;
import project.io.bank_account_system.service.AuditSystem.AuditSystem;
import project.io.bank_account_system.service.BalanceTracker.BankAccountService;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class BankAccountServiceImpl implements BankAccountService {
    private final AuditSystem auditSystem;
    private final int submissionSize;
    private final double maxAbsoluteValue;
    private final ConcurrentLinkedQueue<Transaction> queue = new ConcurrentLinkedQueue<>();
    private final AtomicLong balance = new AtomicLong(0);
    private final ExecutorService processForAudit = Executors.newSingleThreadExecutor();

    public BankAccountServiceImpl(AuditSystem auditSystem,
                                  @Value("${audit.submission.size:1000}") int submissionSize,
                                  @Value("${audit.batch.maxAbsolute:1000000}") double maxAbsoluteValue) {
            this.auditSystem      = auditSystem;
            this.submissionSize   = submissionSize;
            this.maxAbsoluteValue = maxAbsoluteValue;
            startProcessingForAudit();
       }
    private void startProcessingForAudit() {
        processForAudit.submit(() -> {
            while (true) {
                List<Transaction> listOfCurrentTransactions = new ArrayList<>(submissionSize);
                for (int i = 0; i < submissionSize; i++) {
                    Transaction txn;
                    while(true)
                    {
                        txn=queue.poll();
                        if(txn!=null)
                            break;
                        Thread.yield();
                    }
                    listOfCurrentTransactions.add(txn);
                }
                processSubmittedAuditDetails(listOfCurrentTransactions);
            }
        });
    }
    @Override
    public synchronized void processTransaction(Transaction transaction) {
        queue.offer(transaction);
        //Updating balance
        long processedAmtInPence = Math.round(transaction.getAmount() * 100);
        balance.addAndGet(processedAmtInPence);
    }
    private void processSubmittedAuditDetails(List<Transaction> transactionsForAudit) {
        long startNs = System.nanoTime();
        //Sorting by absolute value descending (parallelized)
        List<Transaction> sortedTransactions = transactionsForAudit
                .parallelStream()
                .sorted(Comparator.comparingDouble((Transaction t) -> Math.abs(t.getAmount()))
                        .reversed())
                .collect(Collectors.toList());

        //Sorting by least remaining capacity to be preferred first
        PriorityQueue<Batch> sortedBatches = new PriorityQueue<>(
                Comparator.comparingDouble(Batch::fetchRemainingCapacityofBatch)
        );

        for (Transaction tx : sortedTransactions) {
            double absAmt = Math.abs(tx.getAmount());

            // finding the batch with the smallest remainingCapacity ≥ absAmt
            Batch bestFitBatch = null;
            double bestRemaining = Double.MAX_VALUE;
            for (Batch b : sortedBatches) {
                double remaining = b.fetchRemainingCapacityofBatch();
                if (remaining >= absAmt && remaining < bestRemaining) {
                    bestFitBatch = b;
                    bestRemaining = remaining;
                }
            }
            if (bestFitBatch != null) {
                // packing into the existing batch
                sortedBatches.remove(bestFitBatch);
                bestFitBatch.addTransactions(tx);
                sortedBatches.offer(bestFitBatch);
            } else {
                // no existing batch can fit—making a new one
                Batch newBatch = new Batch(maxAbsoluteValue);
                newBatch.addTransactions(tx);
                sortedBatches.offer(newBatch);
            }
        }
        List<Batch> batches = new ArrayList<>(sortedBatches);
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
        System.out.printf("Processed %,d transactions into %d batches in %dms%n",
                transactionsForAudit.size(), batches.size(), elapsedMs);
        auditSystem.submit(batches);
    }
    @Override
    public synchronized double retrieveBalance() {
        double processAmtInPound = balance.get() / 100.0;
        return processAmtInPound;
    }
}
