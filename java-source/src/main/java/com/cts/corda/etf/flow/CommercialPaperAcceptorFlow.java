package com.cts.corda.etf.flow;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.corda.core.contracts.ContractsDSL.requireThat;

@InitiatedBy(CommercialPaperMoveFlow.class)
@InitiatingFlow
public class CommercialPaperAcceptorFlow extends FlowLogic<SignedTransaction> {

    static private final Logger logger = LoggerFactory.getLogger(CommercialPaperAcceptorFlow.class);
    private final FlowSession flowSession;

    public CommercialPaperAcceptorFlow(FlowSession flowSession) {
        this.flowSession = flowSession;
        System.out.println("Inside CommercialPaperAcceptorFlow called by " + flowSession.getCounterparty());
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        logger.info("CommercialPaperAcceptorFlow inside call method ");
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