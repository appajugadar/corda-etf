package com.cts.corda.etf.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.corda.etf.contract.SettlementContract;
import com.cts.corda.etf.state.SecurityBuyState;
import com.google.common.collect.Sets;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.finance.flows.CashPaymentFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static com.cts.corda.etf.contract.SettlementContract.Settlement_SECURITY_CONTRACT_ID;
import static net.corda.core.contracts.ContractsDSL.requireThat;

@InitiatingFlow
@InitiatedBy(DepositorySellFlow.class)
public class APBuyCompletionFlow extends FlowLogic<String> {

    static private final Logger logger = LoggerFactory.getLogger(APBuyCompletionFlow.class);
    private final FlowSession flowSession;

    public APBuyCompletionFlow(FlowSession flowSession) {
        this.flowSession = flowSession;
        System.out.println("Inside APBuyCompletionFlow called by " + flowSession.getCounterparty());
    }

    @Suspendable
    @Override
    public String call() throws FlowException {
        logger.info("APBuyCompletionFlow inside call method ");

        SignedTransaction stx = subFlow(new APBuyCompletionFlow.SignTxFlow(flowSession, SignTransactionFlow.Companion.tracker()));
        ContractState output = stx.getTx().getOutputs().get(0).getData();
        SecurityBuyState buyState = (SecurityBuyState) output;

        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        List<Party> ls = new ArrayList<>();
        ls.add(buyState.getBuyer());
        ls.add(buyState.getSeller());

        Amount<Currency> amount = new Amount<Currency>(200, Currency.getInstance("GBP"));
        CashPaymentFlow.PaymentRequest paymentRequest = new CashPaymentFlow.PaymentRequest(amount, buyState.getSeller(), false, new HashSet<>());
        subFlow(new CashPaymentFlow(paymentRequest));

        logger.info("securitySellState.getSeller() " + buyState.getSeller());
        FlowSession sellerSession = initiateFlow(buyState.getSeller());
        logger.info("sellerSession.getCounterpartyFlowInfo() " + sellerSession.getCounterpartyFlowInfo());
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
                logger.info("Adding new state to o/p");
                require.using("I won't accept SecurityBuy with a quantity over 100.", newSellState.getQuantity() <= 100);
                require.using("I won't accept SecurityBuy with a quantity over 100.", newSellState.getQuantity() <= 100);
                return null;
            });
        }
    }
}

