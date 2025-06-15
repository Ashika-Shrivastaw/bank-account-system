package project.io.bank_account_system.service.AuditSystem;

import project.io.bank_account_system.model.Audit.Batch;

import java.util.List;

public interface AuditSystem {
    void submit (List<Batch> batches);
}
