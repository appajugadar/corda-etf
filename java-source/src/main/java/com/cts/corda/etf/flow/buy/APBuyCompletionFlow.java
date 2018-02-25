package com.cts.corda.etf.flow.buy;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.corda.etf.flow.depository.DepositorySellFlow;
import com.cts.corda.etf.state.SecurityBuyState;
import lombok.extern.slf4j.Slf4j;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.ContractState;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;
import net.corda.finance.flows.CashPaymentFlow;

import java.util.ArrayList;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;

@InitiatingFlow
@InitiatedBy(DepositorySellFlow.class)
@Slf4j
public class APBuyCompletionFlow extends FlowLogic<String> {

    private final FlowSession flowSession;

    public APBuyCompletionFlow(FlowSession flowSession) {
        this.flowSession = flowSession;
        log.info("Inside APBuyCompletionFlow called by " + flowSession.getCounterparty());
    }

    @Suspendable
    @Override
    public String call() throws FlowException {
        log.info("APBuyCompletionFlow inside call method ");

        SignedTransaction stx = subFlow(new APBuyCompletionFlow.SignTxFlow(flowSession, SignTransactionFlow.Companion.tracker()));
        SecurityBuyState buyState = (SecurityBuyState) stx.getTx().getOutputs().get(0).getData();

        Amount<Currency> amount = new Amount<Currency>(buyState.getQuantity() * 100, Currency.getInstance("GBP"));
        CashPaymentFlow.PaymentRequest paymentRequest = new CashPaymentFlow.PaymentRequest(amount, buyState.getSeller(), false, new HashSet<>());
        subFlow(new CashPaymentFlow(paymentRequest));

        log.info("securitySellState.getSeller() " + buyState.getSeller());
        FlowSession sellerSession = initiateFlow(buyState.getSeller());
        log.info("sellerSession.getCounterpartyFlowInfo() " + sellerSession.getCounterpartyFlowInfo());


        //Report to regulator
        subFlow(new ReportToRegulatorFlow(stx));
        return "Success";
    }

    class SignTxFlow extends SignTransactionFlow {
        private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
            super(otherPartyFlow, progressTracker);
        }

        @Override
        protected void checkTransaction(SignedTransaction stx) {
            requireThat(require -> {
                ContractState output = stx.getTx().getOutputs().get(0).getData();
                require.using("This must be an SecurityBuy transaction.", output instanceof SecurityBuyState);
                SecurityBuyState newSellState = (SecurityBuyState) output;
                log.info("Adding new state to o/p");
                require.using("I won't accept SecurityBuy with a quantity over 100.", newSellState.getQuantity() <= 100);
                require.using("I won't accept SecurityBuy with a quantity over 100.", newSellState.getQuantity() <= 100);
                return null;
            });
        }
    }


    @InitiatingFlow
    public class ReportToRegulatorFlow extends FlowLogic<String> {
        private final SignedTransaction fullySignedTx;

        public ReportToRegulatorFlow(SignedTransaction fullySignedTx) {
            this.fullySignedTx = fullySignedTx;
            log.info("Inside ReportToRegulatorFlow for BuyRequest called by ");
        }

        @Override
        @Suspendable
        public String call() throws FlowException {
            log.info("Inside ReportToRegulatorFlow for BuyRequest call method ");
            Party regulator = (Party) getServiceHub().getIdentityService().partiesFromName("Regulator", true).toArray()[0];
            FlowSession session = initiateFlow(regulator);
            subFlow(new SendTransactionFlow(session, fullySignedTx));
            return "Success";
        }
    }


}

