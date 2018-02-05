package com.cts.corda.etf.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.corda.etf.contract.EtfStock;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.OwnableState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionState;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.identity.PartyAndCertificate;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;
import net.corda.finance.flows.TwoPartyTradeFlow;

import java.util.Currency;
import java.util.List;

@InitiatingFlow
@StartableByRPC
public class EtfSellerFlow extends FlowLogic<SignedTransaction> {

    private final ProgressTracker.Step SELF_ISSUING = new ProgressTracker.Step("Got session ID back, issuing and timestamping some  etf stock.");

    private final ProgressTracker.Step TRADING = new ProgressTracker.Step("Starting the trade flow") {
        @Override
        public ProgressTracker childProgressTracker() {
            return TwoPartyTradeFlow.Seller.Companion.tracker();
        }
    };

    private final ProgressTracker progressTracker = new ProgressTracker(
            SELF_ISSUING, TRADING
    );
    private Party otherParty;
    private Amount<Currency> amount;


    public EtfSellerFlow(Party otherParty, Amount<Currency> amount) {
        this.otherParty = otherParty;
        this.amount = amount;
    }


    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        progressTracker.setCurrentStep(SELF_ISSUING);
        PartyAndCertificate cpOwner = getServiceHub().getMyInfo().getLegalIdentitiesAndCerts().get(0);

        List<StateAndRef<EtfStock.State>> etfStockList = getServiceHub().getVaultService().queryBy(EtfStock.State.class).getStates();
        if (etfStockList.isEmpty())
            throw new IllegalStateException("No Etf stock found.");
        progressTracker.setCurrentStep(TRADING);

        FlowSession session = initiateFlow(otherParty);
        session.send(amount);


        StateAndRef<EtfStock.State> st = etfStockList.get(0);
        TransactionState st1 = st.getState();
        OwnableState st2 = (OwnableState) st1.getData();
        st2.withNewOwner(otherParty);
        return null;
    }
}
