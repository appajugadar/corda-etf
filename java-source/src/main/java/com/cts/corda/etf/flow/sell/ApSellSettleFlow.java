package com.cts.corda.etf.flow.sell;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.corda.etf.contract.SecurityStock;
import com.cts.corda.etf.contract.SellContract;
import com.cts.corda.etf.flow.buy.APBuyCompletionFlow;
import com.cts.corda.etf.state.SecuritySellState;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.util.List;
import java.util.stream.Collectors;

import static com.cts.corda.etf.contract.SellContract.SELL_SECURITY_CONTRACT_ID;
import static com.cts.corda.etf.util.Constants.SELL_MATCHED;

@InitiatingFlow
@InitiatedBy(APBuyCompletionFlow.class)
@Slf4j
public class ApSellSettleFlow extends FlowLogic<SignedTransaction> {

    private final FlowSession flowSession;

    public ApSellSettleFlow(FlowSession flowSession) {
        this.flowSession = flowSession;
        log.info("Inside ApSellSettleFlow called by " + flowSession.getCounterparty());
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        log.info("Inside ApSellSettleFlow call " + flowSession.getCounterparty());
        List<StateAndRef<SecurityStock.State>> etfTradeStatesQueryResp = getServiceHub().getVaultService().queryBy(SecurityStock.State.class).getStates();
        StateAndRef<SecurityStock.State> stateAndRef = null;
        for (StateAndRef<SecurityStock.State> stateAndRef1 : etfTradeStatesQueryResp) {
            stateAndRef = stateAndRef1;
        }
        SignedTransaction fullySignedTx = subFlow(new MoveSecurityFlow(stateAndRef));

        //UPDATE sell request as matched
        List<StateAndRef<SecuritySellState>> ref = getServiceHub().getVaultService().queryBy(SecuritySellState.class).getStates();
        SecuritySellState securitySellState = null;

        for (StateAndRef<SecuritySellState> stateref : ref) {
            securitySellState = stateref.getState().getData();
        }

        if (securitySellState != null) {
            //update sell state
            SignedTransaction fullySignedTx2 = subFlow(new UpdateSellRequestToMatch(securitySellState));
            //Report to regulator
            subFlow(new ReportToRegulatorFlow(fullySignedTx2));
        }


        return fullySignedTx;
    }


    @InitiatingFlow
    public class ReportToRegulatorFlow extends FlowLogic<String> {
        private final SignedTransaction fullySignedTx;

        public ReportToRegulatorFlow(SignedTransaction fullySignedTx) {
            this.fullySignedTx = fullySignedTx;
            log.info("Inside ReportToRegulatorFlow for SellRequest called by ");
        }

        @Override
        @Suspendable
        public String call() throws FlowException {
            log.info("Inside ReportToRegulatorFlow for SellRequest call method ");
            Party regulator = (Party) getServiceHub().getIdentityService().partiesFromName("Regulator", true).toArray()[0];
            FlowSession session = initiateFlow(regulator);
            subFlow(new SendTransactionFlow(session, fullySignedTx));
            return "Success";
        }
    }


    @InitiatingFlow
    public class MoveSecurityFlow extends FlowLogic<SignedTransaction> {
        private final StateAndRef stateAndRef;

        public MoveSecurityFlow(StateAndRef stateAndRef) {
            this.stateAndRef = stateAndRef;
            log.info("Inside ReportToRegulatorFlow for SellRequest called by ");
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            log.info("Inside ReportToRegulatorFlow for SellRequest call method ");
            final TransactionBuilder txBuilder = new TransactionBuilder(getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0));
            SecurityStock.generateMove(txBuilder, stateAndRef, flowSession.getCounterparty());
            txBuilder.verify(getServiceHub());
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);
            FlowSession receiverSession = initiateFlow(flowSession.getCounterparty());
            final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, Sets.newHashSet(receiverSession), CollectSignaturesFlow.Companion.tracker()));
            SignedTransaction fullySignedTx1 = subFlow(new FinalityFlow(fullySignedTx));
            return fullySignedTx1;
        }
    }


    @InitiatingFlow
    public class UpdateSellRequestToMatch extends FlowLogic<SignedTransaction> {
        private final SecuritySellState securitySellState;

        public UpdateSellRequestToMatch(SecuritySellState securitySellState) {
            this.securitySellState = securitySellState;
            log.info("Inside ReportToRegulatorFlow for SellRequest called by ");
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            log.info("Inside ReportToRegulatorFlow for SellRequest call method ");
            securitySellState.setBuyer(flowSession.getCounterparty());
            securitySellState.setStatus(SELL_MATCHED);
            final Command<SellContract.Commands.Create> txCommand2 = new Command<>(new SellContract.Commands.Create(),
                    securitySellState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));
            final TransactionBuilder txBuilder = new TransactionBuilder(notary).withItems(new StateAndContract(securitySellState, SELL_SECURITY_CONTRACT_ID), txCommand2);
            txBuilder.verify(getServiceHub());
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);
            FlowSession depositorySession = initiateFlow(securitySellState.getDepository());
            // Send the state to the CounterParty, and receive it back with their signature.
            final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, Sets.newHashSet(depositorySession), CollectSignaturesFlow.Companion.tracker()));
            return subFlow(new FinalityFlow(fullySignedTx));
        }
    }
}

