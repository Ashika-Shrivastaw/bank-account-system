package project.io.bank_account_system.service.BalanceTracker;

import project.io.bank_account_system.entity.Transaction;
import project.io.bank_account_system.entity.Transaction;

/**
 * Service to aggregate transactions tracking the overall balance for an account.
 */
public interface BankAccountService {
    /**
     * Process a given transaction - this is to be called by the credit and debit generation threads.
     *
     * @param transaction transaction to process
     */
    void processTransaction(Transaction transaction);
    /**
     * Retrieve the balance in the account
     */
    double retrieveBalance();
}
