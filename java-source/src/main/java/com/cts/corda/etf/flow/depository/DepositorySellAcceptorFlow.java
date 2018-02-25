package com.cts.corda.etf.flow.depository;

import com.cts.corda.etf.flow.AbstractTransactionAcceptorFlow;
import com.cts.corda.etf.flow.sell.ApSellSettleFlow;
import com.cts.corda.etf.flow.sell.UpdateSellRequestToMatch;
import lombok.extern.slf4j.Slf4j;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.InitiatedBy;
import net.corda.core.flows.InitiatingFlow;

@InitiatedBy(UpdateSellRequestToMatch.class)
@InitiatingFlow
@Slf4j
public class DepositorySellAcceptorFlow extends AbstractTransactionAcceptorFlow {
    public DepositorySellAcceptorFlow(FlowSession flowSession) {
        super(flowSession);
    }
}