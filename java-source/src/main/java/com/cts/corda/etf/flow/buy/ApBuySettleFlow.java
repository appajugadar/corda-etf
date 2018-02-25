package com.cts.corda.etf.flow.buy;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.corda.etf.contract.BuyContract;
import com.cts.corda.etf.flow.regulator.ReportToRegulatorFlow;
import com.cts.corda.etf.flow.sell.APSellCompletionFlow;
import com.cts.corda.etf.state.SecurityBuyState;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import net.corda.core.contracts.*;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.finance.flows.CashPaymentFlow;

import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static com.cts.corda.etf.contract.BuyContract.BUY_SECURITY_CONTRACT_ID;
import static com.cts.corda.etf.util.Constants.BUY_MATCHED;
import static net.corda.core.contracts.ContractsDSL.requireThat;

@InitiatedBy(APSellCompletionFlow.class)
@InitiatingFlow
@Slf4j
public class ApBuySettleFlow extends FlowLogic<SignedTransaction> {

    private final FlowSession flowSession;

    public ApBuySettleFlow(FlowSession flowSession) {
        this.flowSession = flowSession;
        log.info("Inside ApBuySettleFlow called by " + flowSession.getCounterparty());
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        log.info("ApBuySettleFlow inside call method ");
        //SignedTransaction stx = subFlow(new ApBuySettleFlow.SignTxFlow(flowSession, SignTransactionFlow.Companion.tracker()));
        List<StateAndRef<SecurityBuyState>> ref = getServiceHub().getVaultService().queryBy(SecurityBuyState.class).getStates();
        SecurityBuyState securityBuyState = null;

        for (StateAndRef<SecurityBuyState> stateref : ref) {
            securityBuyState = stateref.getState().getData();
        }

        securityBuyState.setSeller(flowSession.getCounterparty());
//send cash
        Amount<Currency> amount = new Amount<Currency>(securityBuyState.getQuantity() * 100, Currency.getInstance("GBP"));
        CashPaymentFlow.PaymentRequest paymentRequest = new CashPaymentFlow.PaymentRequest(amount, securityBuyState.getSeller(), false, new HashSet<>());
        subFlow(new CashPaymentFlow(paymentRequest));

        SignedTransaction stx = null;
        //UPDATE sell request as matched
        if (securityBuyState != null) {
            //update sell state
            stx = subFlow(new UpdateBuyRequestToMatch(securityBuyState));
            //Report to regulator
            subFlow(new ReportToRegulatorFlow(stx));
        }
        return stx;
    }

/*    @InitiatingFlow
    public class ReportToRegulatorFlow extends FlowLogic<String> {
        private final SignedTransaction fullySignedTx;

        public ReportToRegulatorFlow(SignedTransaction fullySignedTx) {
            this.fullySignedTx = fullySignedTx;
            log.info("Inside ApBuySettleFlow ReportToRegulatorFlow ");
        }

        @Override
        @Suspendable
        public String call() throws FlowException {
            log.info("Inside ApBuySettleFlow ReportToRegulatorFlow for BuyRequest call method ");
            Party regulator = (Party) getServiceHub().getIdentityService().partiesFromName("Regulator", true).toArray()[0];
            FlowSession session = initiateFlow(regulator);
            log.info("Inside ApBuySettleFlow ReportToRegulatorFlow after initiate flow ");
            subFlow(new SendTransactionFlow(session, fullySignedTx));
            log.info("Inside ApBuySettleFlow ReportToRegulatorFlow after SendTransactionFlow ");
            return "Success";
        }
    }*/


    class SignTxFlow extends SignTransactionFlow {

        private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
            super(otherPartyFlow, progressTracker);
        }

        @Override
        protected void checkTransaction(SignedTransaction stx) {
            requireThat(require -> {
                ContractState output = stx.getTx().getOutputs().get(0).getData();
                log.info("ApBuySettleFlow output " + output);
                return null;
            });
        }
    }

    @InitiatingFlow
    public class UpdateBuyRequestToMatch extends FlowLogic<SignedTransaction> {
        private final SecurityBuyState securityBuyState;

        public UpdateBuyRequestToMatch(SecurityBuyState securityBuyState) {
            this.securityBuyState = securityBuyState;
            log.info("Inside UpdateBuyRequestToMatch for BuyRequest called by ");
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            log.info("Inside UpdateBuyRequestToMatch for BuyRequest call method ");
            securityBuyState.setStatus(BUY_MATCHED);
            final Command<BuyContract.Commands.Create> txCommand2 = new Command<>(new BuyContract.Commands.Create(),
                    securityBuyState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));
            final TransactionBuilder txBuilder = new TransactionBuilder(notary).withItems(new StateAndContract(securityBuyState, BUY_SECURITY_CONTRACT_ID), txCommand2);
            txBuilder.verify(getServiceHub());
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);
            FlowSession depositorySession = initiateFlow(securityBuyState.getDepository());
            // Send the state to the CounterParty, and receive it back with their signature.
            final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, Sets.newHashSet(depositorySession), CollectSignaturesFlow.Companion.tracker()));
            return subFlow(new FinalityFlow(fullySignedTx));
        }
    }
}

