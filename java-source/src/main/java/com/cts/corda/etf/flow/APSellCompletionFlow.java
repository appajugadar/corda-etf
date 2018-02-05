package com.cts.corda.etf.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.corda.etf.state.SecuritySellState;
import net.corda.core.contracts.ContractState;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.corda.core.contracts.ContractsDSL.requireThat;

@InitiatedBy(DepositoryBuyFlow.class)
public class APSellCompletionFlow extends FlowLogic<SignedTransaction> {

    static private final Logger logger = LoggerFactory.getLogger(DepositoryBuyFlow.class);
    private final FlowSession flowSession;

    public APSellCompletionFlow(FlowSession flowSession) {
        this.flowSession = flowSession;
        System.out.println("Inside APSellCompletionFlow called by " + flowSession.getCounterparty());
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        logger.info("APSellCompletionFlow inside call method ");
        SignedTransaction tx = subFlow(new APSellCompletionFlow.SignTxFlow(flowSession, SignTransactionFlow.Companion.tracker()));
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
                require.using("This must be an SecuritySell transaction.", output instanceof SecuritySellState);
                SecuritySellState newSellState = (SecuritySellState) output;
                logger.info("Adding new state to o/p");
                require.using("I won't accept SecurityBuy with a quantity over 100.", newSellState.getQuantity() <= 100);
                return null;
            });
        }
    }
}

