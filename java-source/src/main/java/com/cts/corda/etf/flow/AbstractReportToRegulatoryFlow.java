package com.cts.corda.etf.flow;

import co.paralleluniverse.fibers.Suspendable;
import lombok.extern.slf4j.Slf4j;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;

@Slf4j
public abstract class AbstractReportToRegulatoryFlow extends FlowLogic<String> {
    private final SignedTransaction fullySignedTx;

    public AbstractReportToRegulatoryFlow(SignedTransaction fullySignedTx) {
        this.fullySignedTx = fullySignedTx;
        log.info("Inside AbstractReportToRegulatoryFlow ");
    }

    @Override
    @Suspendable
    public String call() throws FlowException {
        log.info("Inside AbstractReportToRegulatoryFlow for BuyRequest call method ");
        Party regulator = (Party) getServiceHub().getIdentityService().partiesFromName("Regulator", true).toArray()[0];
        FlowSession session = initiateFlow(regulator);
        subFlow(new SendTransactionFlow(session, fullySignedTx));
        return "Success";
    }
}