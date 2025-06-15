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
		public List<List<Batch>> getList() {
			return submittedBatches;
		}
	}

	@BeforeEach
	void setTestAuditSystem() {
		testAuditSystem = new TestAuditSystem();
		bankAccountService = new BankAccountServiceImpl(
				testAuditSystem, 1000, 1_000_000.0);
	}

	private double setUpTransactions() {
		double expectedBalance = 0.0;
		for (int i = 0; i < 1000; i++) {
			double amount = (i % 3 == 0) ? 5000.0 : -500.0;
			bankAccountService.processTransaction(new Transaction("transaction" + i, amount));
			expectedBalance += amount;
		}
		waitForAuditSubmission();
		return expectedBalance;
	}

	private void waitForAuditSubmission() {
		long deadline = System.currentTimeMillis() + 5000;
		while (testAuditSystem.getList().isEmpty() && System.currentTimeMillis() < deadline) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException ignored) {}
		}
	}
	@Test
	void testTransactions() {
		bankAccountService.processTransaction(new Transaction("Transaction 1:", 1000.0)); //credit
		bankAccountService.processTransaction(new Transaction("Transaction 2:", -500.0)); //debit
		assertEquals(500.0, bankAccountService.retrieveBalance(), 0.001, "Balance should be 500.0");
	}
	@Test
	void testTransactionAmountGeneratorRange() {
		for (int i = 0; i < 10_000; i++) {
			double randomAmt = ThreadLocalRandom.current().nextDouble();
			double amount = 200 + randomAmt * (500_000 - 200);
			assertTrue(
					amount >= 200.0 && amount <= 500_000.0,
					"Generated amount out of bounds: " + amount);
		}
	}
	@Test
	void testBalanceCalculationAfterBulk() {
		double expectedBalance = setUpTransactions();
		assertEquals(expectedBalance, bankAccountService.retrieveBalance(), 0.01);
	}

	@Test
	void testAuditSubmissionCount() {
		setUpTransactions();
		List<List<Batch>> submissions = testAuditSystem.getList();
		assertEquals(1, submissions.size(), "Should have exactly one submission");
	}
	@Test
	void testTotalTransactionsInBatches() {
		setUpTransactions();
		int total = testAuditSystem.getList().get(0).stream()
				.mapToInt(Batch::getTransactionCount)
				.sum();
		assertEquals(1000, total, "Total transactions across all batches should be 1000");
	}
	@Test
	void testBatchConstraintsWithinSubmission() {
		setUpTransactions();
		List<Batch> batches = testAuditSystem.getList().get(0);
		assertFalse(batches.isEmpty(), "There should be at least one batch");
		for (Batch b : batches) {
			assertTrue(b.getTotalValueOfTransactions() <= 1_000_000.0,
					"Batch absolute sum should not exceed £1,000,000");
			assertTrue(b.getTransactionCount() > 0,
					"Each batch must contain at least one transaction");
		}
	}
}
