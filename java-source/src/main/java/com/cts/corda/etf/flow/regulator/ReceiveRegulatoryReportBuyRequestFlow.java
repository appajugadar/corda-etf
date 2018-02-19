package com.cts.corda.etf.flow.regulator;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.corda.etf.flow.buy.APBuyCompletionFlow;
import net.corda.core.flows.*;
import net.corda.core.node.StatesToRecord;

@InitiatedBy(APBuyCompletionFlow.ReportToRegulatorFlow.class)
public class ReceiveRegulatoryReportBuyRequestFlow extends FlowLogic<String> {

    private final FlowSession flowSession;

    public ReceiveRegulatoryReportBuyRequestFlow(FlowSession flowSession) {
        this.flowSession = flowSession;
        System.out.println("Inside ReceiveRegulatoryReportSellRequestFlow called by " + flowSession.getCounterparty());
    }

    @Override
    @Suspendable
    public String call() throws FlowException {
        System.out.println("Inside ReceiveRegulatoryReportSellRequestFlow call method ");
        subFlow(new ReceiveTransactionFlow(flowSession, true, StatesToRecord.ALL_VISIBLE));
        return "Success";
    }
}
