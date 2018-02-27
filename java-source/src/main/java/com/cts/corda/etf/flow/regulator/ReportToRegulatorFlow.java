package com.cts.corda.etf.flow.regulator;

import co.paralleluniverse.fibers.Suspendable;
import lombok.extern.slf4j.Slf4j;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;

@Slf4j
@InitiatingFlow
public class ReportToRegulatorFlow extends FlowLogic<String> {
    private final SignedTransaction fullySignedTx;

    public ReportToRegulatorFlow(SignedTransaction fullySignedTx) {
        this.fullySignedTx = fullySignedTx;
        log.info("Inside ReportToRegulatorFlow ");
    }

    @Override
    @Suspendable
    public String call() throws FlowException {
        log.info("Inside ReportToRegulatorFlow for BuyRequest call method ");
        Party regulator = (Party) getServiceHub().getIdentityService().partiesFromName("FCA", true).toArray()[0];
        FlowSession session = initiateFlow(regulator);
        subFlow(new SendTransactionFlow(session, fullySignedTx));
        return "Success";
    }
}