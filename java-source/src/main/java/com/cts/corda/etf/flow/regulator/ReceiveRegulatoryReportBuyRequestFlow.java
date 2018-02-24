package com.cts.corda.etf.flow.regulator;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.corda.etf.flow.buy.APBuyCompletionFlow;
import lombok.extern.slf4j.Slf4j;
import net.corda.core.flows.*;
import net.corda.core.node.StatesToRecord;

@InitiatedBy(APBuyCompletionFlow.ReportToRegulatorFlow.class)
@Slf4j
public class ReceiveRegulatoryReportBuyRequestFlow extends FlowLogic<String> {

    private final FlowSession flowSession;

    public ReceiveRegulatoryReportBuyRequestFlow(FlowSession flowSession) {
        this.flowSession = flowSession;
        log.info("Inside ReceiveRegulatoryReportSellRequestFlow called by " + flowSession.getCounterparty());
    }

    @Override
    @Suspendable
    public String call() throws FlowException {
        log.info("Inside ReceiveRegulatoryReportSellRequestFlow call method ");
        subFlow(new ReceiveTransactionFlow(flowSession, true, StatesToRecord.ALL_VISIBLE));
        return "Success";
    }
}
