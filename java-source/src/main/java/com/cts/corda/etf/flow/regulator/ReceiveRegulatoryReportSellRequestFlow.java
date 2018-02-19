package com.cts.corda.etf.flow.regulator;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.corda.etf.flow.sell.ApSellSettleFlow;
import net.corda.core.flows.*;
import net.corda.core.node.StatesToRecord;

@InitiatedBy(ApSellSettleFlow.ReportToRegulatorFlow.class)
public class ReceiveRegulatoryReportSellRequestFlow extends FlowLogic<String> {

    private final FlowSession flowSession;

    public ReceiveRegulatoryReportSellRequestFlow(FlowSession flowSession) {
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
