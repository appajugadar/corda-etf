package com.cts.corda.etf.flow.sell;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.corda.etf.contract.SecurityStock;
import com.cts.corda.etf.contract.SellContract;
import com.cts.corda.etf.flow.regulator.ReportToRegulatorFlow;
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

        SignedTransaction fullySignedTx = subFlow(new MoveSecurityFlow(stateAndRef, flowSession.getCounterparty()));

        //UPDATE sell request as matched
        List<StateAndRef<SecuritySellState>> ref = getServiceHub().getVaultService().queryBy(SecuritySellState.class).getStates();
        SecuritySellState securitySellState = null;

        for (StateAndRef<SecuritySellState> stateref : ref) {
            securitySellState = stateref.getState().getData();
        }

        if (securitySellState != null) {
            //update sell state
            securitySellState.setBuyer(flowSession.getCounterparty());
            SignedTransaction fullySignedTx2 = subFlow(new UpdateSellRequestToMatch(securitySellState));
            //Report to regulator
            subFlow(new ReportToRegulatorFlow(fullySignedTx2));
        }

        return fullySignedTx;
    }


}

