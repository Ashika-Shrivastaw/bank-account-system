package project.io.bank_account_system.controller;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import project.io.bank_account_system.entity.Transaction;
import project.io.bank_account_system.service.BalanceTracker.BankAccountService;

import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/account")
public class BalanceController {
    private static final double MIN_AMOUNT = 200.0;
    private static final double MAX_AMOUNT = 500_000.0;
    private final BankAccountService bankAccountService;

    public BalanceController(BankAccountService bankAccountService){
        this.bankAccountService=bankAccountService;
    }

    @GetMapping("/balance")
    public double getAccountBalance(){
        double getBalance = bankAccountService.retrieveBalance();
        return getBalance;
    }
    @PostMapping("/transaction")
    public ResponseEntity<String> addTransactions(@RequestBody Transaction transactionRequest){
        double absAmt = Math.abs(transactionRequest.getAmount());
        if (absAmt < MIN_AMOUNT) {
            return ResponseEntity
                    .badRequest()
                    .body("underlimit: transaction amount must be at least £" + MIN_AMOUNT);
        }

        if (absAmt > MAX_AMOUNT) {
            return ResponseEntity
                    .badRequest()
                    .body("upperlimit: transaction amount must be at most £" + MAX_AMOUNT);
        }
        String id = transactionRequest.getTransactionId() != null
                ? transactionRequest.getTransactionId()
                : java.util.UUID.randomUUID().toString();
        Transaction tx = new Transaction(id, transactionRequest.getAmount());
        bankAccountService.processTransaction(tx);
        return ResponseEntity.ok("Transaction accepted");
    }
}
