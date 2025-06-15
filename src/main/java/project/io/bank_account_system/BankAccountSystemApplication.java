package project.io.bank_account_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import project.io.bank_account_system.service.AuditSystem.AuditSystem;
import project.io.bank_account_system.service.AuditSystem.impl.ShowAuditDetails;
import project.io.bank_account_system.service.BalanceTracker.BankAccountService;
import project.io.bank_account_system.service.BalanceTracker.impl.BankAccountServiceImpl;

@SpringBootApplication
public class BankAccountSystemApplication {

	public static void main(String[] args) {

		SpringApplication.run(BankAccountSystemApplication.class, args);
	}

	@Bean
	public BankAccountService bankAccountService(AuditSystem auditSystem){
		return new BankAccountServiceImpl(auditSystem);
	}
	@Bean
	public AuditSystem auditSystem(){
		return new ShowAuditDetails();
	}
}
