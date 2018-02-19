package com.cts.corda.etf.flow.buy;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.corda.etf.flow.sell.APSellCompletionFlow;
import net.corda.core.contracts.ContractState;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.corda.core.contracts.ContractsDSL.requireThat;

@InitiatedBy(APSellCompletionFlow.class)
@InitiatingFlow
public class ApBuySettleFlow extends FlowLogic<SignedTransaction> {

    static private final Logger logger = LoggerFactory.getLogger(ApBuySettleFlow.class);
    private final FlowSession flowSession;

    public ApBuySettleFlow(FlowSession flowSession) {
        this.flowSession = flowSession;
        System.out.println("Inside ApBuySettleFlow called by " + flowSession.getCounterparty());
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        logger.info("ApBuySettleFlow inside call method ");
        SignedTransaction tx = subFlow(new ApBuySettleFlow.SignTxFlow(flowSession, SignTransactionFlow.Companion.tracker()));
        return tx;
    }

    class SignTxFlow extends SignTransactionFlow {

        private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
            super(otherPartyFlow, progressTracker);
        }

        @Override
        protected void checkTransaction(SignedTransaction stx) {
            requireThat(require -> {
                ContractState output = stx.getTx().getOutputs().get(0).getData();
                logger.info("ApBuySettleFlow output " + output);
                return null;
            });
        }
    }
}

