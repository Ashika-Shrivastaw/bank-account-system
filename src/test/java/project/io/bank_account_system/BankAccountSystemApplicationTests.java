package project.io.bank_account_system;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import project.io.bank_account_system.entity.Transaction;
import project.io.bank_account_system.model.Audit.Batch;
import project.io.bank_account_system.service.AuditSystem.AuditSystem;
import project.io.bank_account_system.service.BalanceTracker.BankAccountService;
import project.io.bank_account_system.service.BalanceTracker.impl.BankAccountServiceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BankAccountSystemApplicationTests {
	private BankAccountService bankAccountService;
	private TestAuditSystem testAuditSystem;

	static class TestAuditSystem implements AuditSystem {
		private final List<List<Batch>> submittedBatches = new ArrayList<>();
		@Override
		public void submit(List<Batch> batches){
			System.out.println("Submitting: "+ batches.size()+ " batches");
			submittedBatches.add(new ArrayList<>(batches));
		}
		public List<List<Batch>> getList(){
			return submittedBatches;
		}
	}


	@BeforeEach
	void setTestAuditSystem(){
		testAuditSystem = new TestAuditSystem();
		bankAccountService = new BankAccountServiceImpl(testAuditSystem);
	}
	private double setUpTransactions(){
		//Creating 1000 transactions
		List<Transaction> transactionsList = new ArrayList<>();
		double expectedBalance = 0.0;
		for(int i=0; i<1000; i++){
			double amount = (i%3 == 0) ? 5000 : -500;
			transactionsList.add(new Transaction("Transaction" + i, amount));
			expectedBalance = expectedBalance + amount;
		}

		System.out.println("Created " + transactionsList.size() + "transactions");
		for(Transaction eachTransaction : transactionsList){
			bankAccountService.processTransaction(eachTransaction);
		}
		return expectedBalance;
	}

	@Test
	void testTransactions(){
		bankAccountService.processTransaction(new Transaction("Transaction 1:", 1000.0)); //credit
		bankAccountService.processTransaction(new Transaction("Transaction 2:", -500.0)); //debit
		assertEquals(500.0, bankAccountService.retrieveBalance(), 0.001, "Balance should be 500.0");
	}
	@Test
	void testTransactionRangeWithinBounds() {
		for (int i = 0; i < 10_000; i++) {
			double randomAmt = ThreadLocalRandom.current().nextDouble(); // [0.0, 1.0)
			double amount = 200 + randomAmt * (500_000 - 200);
			assertTrue(
					amount >= 200.0 && amount <= 500_000.0,
					() -> "Generated amount out of bounds: " + amount
			);
		}
	}

	@Test
	void testBalanceCalculation () {
		double expectedBalance = setUpTransactions();
		assertEquals(expectedBalance, bankAccountService.retrieveBalance(), 0.01, "Balance should match the expected value");
	}

	@Test
	void testSubmittedBatchCount() throws InterruptedException {
		setUpTransactions();
		((BankAccountServiceImpl) bankAccountService).awaitAuditProcessing();
		List<List<Batch>> submittedBatches = testAuditSystem.getList();
		System.out.println("Number of submissions: " + submittedBatches.size());
		assertEquals(1, submittedBatches.size(), "Should have one submitted batch");
	}

	@Test
	void testBatchConstraints() throws InterruptedException {
		setUpTransactions();
		((BankAccountServiceImpl) bankAccountService).awaitAuditProcessing();
		List<List<Batch>> submittedBatches = testAuditSystem.getList();
		assertFalse(submittedBatches.isEmpty(), "At least one submission should exist");

		List<Batch> batches = submittedBatches.get(0);
		for (Batch batch : batches) {
			assertTrue(batch.getTotalValueOfTransactions() <= 1000000.0, "Batch total value should not exceed 1,000,000");
			assertTrue(batch.getTransactionCount() > 0, "Batch should contain at least one transaction");
		}
	}

	@Test
	void testTotalTransactionCount() throws InterruptedException {
		setUpTransactions();
		((BankAccountServiceImpl) bankAccountService).awaitAuditProcessing();
		int totalTransactions = 0;
		List<List<Batch>> submittedBatches = testAuditSystem.getList();
		assertFalse(submittedBatches.isEmpty(), "At least one submission should exist");

		List<Batch> batches = submittedBatches.get(0);
		for(Batch batch : batches){
			totalTransactions = totalTransactions + batch.getTransactionCount();
		}
		assertEquals(1000, totalTransactions, "Total transaction should be 1000");
	}
}
