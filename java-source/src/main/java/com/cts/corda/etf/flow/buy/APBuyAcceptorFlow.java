package com.cts.corda.etf.flow.buy;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.corda.etf.flow.sell.ApSellSettleFlow;
import lombok.extern.slf4j.Slf4j;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;

import static net.corda.core.contracts.ContractsDSL.requireThat;


@InitiatedBy(ApSellSettleFlow.MoveSecurityFlow.class)
@InitiatingFlow
@Slf4j
public class APBuyAcceptorFlow extends FlowLogic<SignedTransaction> {

    private final FlowSession flowSession;

    public APBuyAcceptorFlow(FlowSession flowSession) {
        this.flowSession = flowSession;
        log.info("Inside APBuyAcceptorFlow called by " + flowSession.getCounterparty());
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        log.info("APBuyAcceptorFlow inside call method ");
        SignedTransaction stx = subFlow(new SignTxFlow(flowSession, SignTransactionFlow.Companion.tracker()));
        log.info("APBuyAcceptorFlow signed tx");
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