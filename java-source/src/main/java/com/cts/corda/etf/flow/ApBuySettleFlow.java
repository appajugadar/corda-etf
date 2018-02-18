package com.cts.corda.etf.flow;

import co.paralleluniverse.fibers.Suspendable;
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
       /* final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        final TransactionBuilder txBuilder = new TransactionBuilder(notary);
        Amount<Currency> amount = new Amount<Currency>(200, Currency.getInstance("GBP"));
        net.corda.finance.contracts.asset.Cash.generateSpend(getServiceHub(), txBuilder, amount, flowSession.getCounterparty(), new HashSet<>());
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

