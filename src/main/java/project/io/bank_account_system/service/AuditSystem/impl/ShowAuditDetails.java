package project.io.bank_account_system.service.AuditSystem.impl;

import ch.qos.logback.core.net.SyslogOutputStream;
import org.springframework.boot.env.SystemEnvironmentPropertySourceEnvironmentPostProcessor;
import project.io.bank_account_system.model.Audit.Batch;
import project.io.bank_account_system.service.AuditSystem.AuditSystem;

import java.util.List;

public class ShowAuditDetails implements AuditSystem {
    @Override
    public void submit(List<Batch> batches) {
        System.out.println("{");
        System.out.println(" submission: {");
        System.out.println("        batches: [");
        for(int i=0; i<batches.size(); i++){
            Batch batch = batches.get(i);
            System.out.println("        {");
            System.out.println("            totalValueOfAllTransactions: " + batch.getTotalValueOfTransactions());
            System.out.println("            countOfTransactions: " + batch.getTransactionCount());
            System.out.println("        }");
            if(i < batches.size() - 1)
                System.out.println("    ,");
            else
                System.out.println();
        }
        System.out.println("     ]");
        System.out.println("  }");
        System.out.println("}");
    }
}
