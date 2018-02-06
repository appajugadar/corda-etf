package com.cts.corda.etf.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.corda.etf.contract.SellContract;
import com.cts.corda.etf.state.SecurityBuyState;
import com.cts.corda.etf.state.SecuritySellState;
import com.google.common.collect.Sets;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static com.cts.corda.etf.contract.SellContract.SELL_SECURITY_CONTRACT_ID;
import static net.corda.core.contracts.ContractsDSL.requireThat;

@InitiatedBy(APSellCompletionFlow.class)
@InitiatingFlow
public class ApBuySettleFlow  extends FlowLogic<SignedTransaction> {

    static private final Logger logger = LoggerFactory.getLogger(DepositoryBuyFlow.class);
    private final FlowSession flowSession;

    public ApBuySettleFlow(FlowSession flowSession) {
        this.flowSession = flowSession;
        System.out.println("Inside ApBuySettleFlow called by " + flowSession.getCounterparty());
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {

        logger.info("ApBuySettleFlow inside call method ");

       /* //check vault for sell states and if found then return
        Vault.Page<SecuritySellState> results = getServiceHub().getVaultService().queryBy(SecuritySellState.class);
        List<StateAndRef<SecuritySellState>> ref = results.getStates();
        SecuritySellState securitySellState = null;

        for (StateAndRef<SecuritySellState> stateref : ref) {
            securitySellState = stateref.getState().getData();
        }

        logger.info("ApBuySettleFlow flowSession " + flowSession.getCounterpartyFlowInfo());
        logger.info("Sending back SecuritySellState to APBuy Flow " + securitySellState);
*/

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


             //   require.using("This must be an SecurityBuy transaction.", output instanceof SecurityBuyState);

             /*   //
                Vault.Page<SecuritySellState> results = getServiceHub().getVaultService().queryBy(SecuritySellState.class);
                List<StateAndRef<SecuritySellState>> ref = results.getStates();
                SecuritySellState securitySellState = null;

                for (StateAndRef<SecuritySellState> stateref : ref) {
                    securitySellState = stateref.getState().getData();
                }
                //
                SecurityBuyState newBuyState = (SecurityBuyState) output;

                if(securitySellState!=null){
                    newBuyState.setSeller(securitySellState.getSeller());
                    newBuyState.setStatus("BUY_MATCHED");
                }

                logger.info("Adding new state to o/p");
                stx.getTx().getOutputStates().add(newBuyState);

                require.using("I won't accept SecurityBuy with a quantity over 100.", newBuyState.getQuantity() <= 100);*/
                return null;
            });
        }
    }
}

