package project.io.bank_account_system.model.Producer;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import project.io.bank_account_system.entity.Transaction;
import project.io.bank_account_system.service.BalanceTracker.BankAccountService;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

@Component
public class TransactionProducer {
    private final BankAccountService bankAccountService;
    private final ScheduledExecutorService threadExecutor = Executors.newScheduledThreadPool(2);

    public TransactionProducer(BankAccountService bankAccountService){
        this.bankAccountService = bankAccountService;
    }

    //@PostConstruct
    public void startTransactions(){

        Runnable creditTransaction = new Runnable() {
            @Override
            public void run() {
                double randomAmt = ThreadLocalRandom.current().nextDouble(); // This will generate random no. between 0.0 to 1.0
                //Amount to be between £200 and £500,000
                double creditAmt = 200 + randomAmt * (500000 - 200); //credits having positive amount
                Transaction credit = new Transaction(UUID.randomUUID().toString(), creditAmt);
                bankAccountService.processTransaction(credit);
            }
        };
        threadExecutor.scheduleAtFixedRate(creditTransaction, 0, 40, TimeUnit.MILLISECONDS);

        Runnable debitTransaction = new Runnable() {
            @Override
            public void run() {
                double randomAmt = ThreadLocalRandom.current().nextDouble();
                double debitAmt = - (200 + randomAmt * (500000 - 200)); //debits having negative amount
                Transaction debit =new Transaction(UUID.randomUUID().toString(), debitAmt);
                bankAccountService.processTransaction(debit);
            }
        };
        threadExecutor.scheduleAtFixedRate(debitTransaction,0,40,TimeUnit.MILLISECONDS);
    }
}
