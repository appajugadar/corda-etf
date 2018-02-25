package com.cts.corda.etf.flow.sell;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.corda.etf.contract.SecurityStock;
import com.cts.corda.etf.flow.regulator.ReportToRegulatorFlow;
import com.cts.corda.etf.flow.depository.DepositoryBuyFlow;
import com.cts.corda.etf.state.SecuritySellState;
import lombok.extern.slf4j.Slf4j;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;

import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;

@InitiatingFlow
@InitiatedBy(DepositoryBuyFlow.class)
@Slf4j
public class APSellCompletionFlow extends FlowLogic<SignedTransaction> {

    private final FlowSession flowSession;
    public APSellCompletionFlow(FlowSession flowSession) {
        this.flowSession = flowSession;
        log.info("Inside APSellCompletionFlow called by " + flowSession.getCounterparty());
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        log.info("APSellCompletionFlow inside call method ");

        SignedTransaction stx = subFlow(new SignTxFlow(flowSession, SignTransactionFlow.Companion.tracker()));
        SecuritySellState sellState = (SecuritySellState) stx.getTx().getOutputs().get(0).getData();

        //Move security
        List<StateAndRef<SecurityStock.State>> etfTradeStatesQueryResp = getServiceHub().getVaultService().queryBy(SecurityStock.State.class).getStates();
        StateAndRef<SecurityStock.State> stateAndRef = null;
        for (StateAndRef<SecurityStock.State> stateAndRef1 : etfTradeStatesQueryResp) {
            stateAndRef = stateAndRef1;
        }
        subFlow(new MoveSecurityFlow(stateAndRef, sellState.getBuyer()));

        // call flow on buyer side for cash payment
        log.info("securitySellState.getBuyer() " + sellState.getBuyer());
        FlowSession sellerSession = initiateFlow(sellState.getBuyer());
        log.info("sellerSession.getCounterpartyFlowInfo() " + sellerSession.getCounterpartyFlowInfo());

        //Report to regulator
        subFlow(new ReportToRegulatorFlow(stx));
        return stx;
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
                log.info("Adding new state to o/p");
                require.using("I won't accept SecurityBuy with a quantity over 100.", newSellState.getQuantity() <= 100);
                return null;
            });
        }
    }
}

