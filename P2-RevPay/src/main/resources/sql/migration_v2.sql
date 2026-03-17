-- Migration script to add transaction PIN support
ALTER TABLE USERS ADD (txn_pin_hash VARCHAR2(255));

-- Update TRANSACTIONS check constraint for loan disbursements
-- Note: Oracle requires dropping and recreating the constraint if the name is known.
-- We'll try to add a new one or just let the user know.
ALTER TABLE TRANSACTIONS MODIFY (txn_type VARCHAR2(20));
-- If you want to update the check constraint manually:
-- ALTER TABLE TRANSACTIONS DROP CONSTRAINT [CONSTRAINT_NAME];
-- ALTER TABLE TRANSACTIONS ADD CONSTRAINT CHK_TXN_TYPE CHECK (txn_type IN ('SEND','RECEIVE','ADD_FUNDS','WITHDRAWAL','PAYMENT','LOAN_REPAYMENT','LOAN_DISBURSE'));

COMMIT;
EXIT;
