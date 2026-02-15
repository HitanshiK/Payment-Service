package com.paymentSystem.project.service;

import org.springframework.stereotype.Service;

@Service
public class ExternalPaymentService {

    /**
     * 1. Idempotency check
     * 2. Create payment intent (INTERNAL)
     * 3. Validate currency compatibility ← ✅ ADD HERE
     * 4. Check wallet balance limit (overflow pre-check)
     * 5. Create external payment record
     * 6. Create gateway order
     * 7. Redirect user → gateway
     * 8. Receive webhook
     * 9. Verify signature & status
     * 10. Handle overflow (refund extra if needed)
     * 11. Update payment status
     * 12. Update external payment
     * 13. Create wallet ledger entry
     * 14. Commit transaction
     */
}
