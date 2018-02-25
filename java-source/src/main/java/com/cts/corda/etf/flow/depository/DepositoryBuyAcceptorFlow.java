package com.cts.corda.etf.flow.depository;

import com.cts.corda.etf.flow.AbstractTransactionAcceptorFlow;
import com.cts.corda.etf.flow.buy.ApBuySettleFlow;
import com.cts.corda.etf.flow.buy.UpdateBuyRequestToMatch;
import lombok.extern.slf4j.Slf4j;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.InitiatedBy;
import net.corda.core.flows.InitiatingFlow;

@InitiatedBy(UpdateBuyRequestToMatch.class)
@InitiatingFlow
@Slf4j
public class DepositoryBuyAcceptorFlow extends AbstractTransactionAcceptorFlow {
    public DepositoryBuyAcceptorFlow(FlowSession flowSession) {
        super(flowSession);
    }
}