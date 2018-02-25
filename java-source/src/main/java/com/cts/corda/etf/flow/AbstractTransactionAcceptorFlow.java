package com.cts.corda.etf.flow;

import co.paralleluniverse.fibers.Suspendable;
import lombok.extern.slf4j.Slf4j;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;

import static net.corda.core.contracts.ContractsDSL.requireThat;

@InitiatingFlow
@Slf4j
public abstract class AbstractTransactionAcceptorFlow extends FlowLogic<SignedTransaction> {

    private final FlowSession flowSession;

    public AbstractTransactionAcceptorFlow(FlowSession flowSession) {
        this.flowSession = flowSession;
        log.info("Inside AbstractTransactionAcceptorFlow called by " + flowSession.getCounterparty());
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        log.info("AbstractTransactionAcceptorFlow inside call method ");
        SignedTransaction stx = subFlow(new SignTxFlow(flowSession, SignTransactionFlow.Companion.tracker()));
        return stx;
    }

    class SignTxFlow extends SignTransactionFlow {
        private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
            super(otherPartyFlow, progressTracker);
        }

        @Override
        protected void checkTransaction(SignedTransaction stx) {
            requireThat(require -> {
                return null;
            });
        }
    }
}