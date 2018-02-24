package com.cts.corda.etf.flow;

import co.paralleluniverse.fibers.Suspendable;
import lombok.extern.slf4j.Slf4j;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;

import static net.corda.core.contracts.ContractsDSL.requireThat;


@InitiatedBy(CommercialPaperMoveFlow.class)
@InitiatingFlow
@Slf4j
public class CommercialPaperAcceptorFlow extends FlowLogic<SignedTransaction> {

    private final FlowSession flowSession;

    public CommercialPaperAcceptorFlow(FlowSession flowSession) {
        this.flowSession = flowSession;
        log.info("Inside CommercialPaperAcceptorFlow called by " + flowSession.getCounterparty());
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        log.info("CommercialPaperAcceptorFlow inside call method ");
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