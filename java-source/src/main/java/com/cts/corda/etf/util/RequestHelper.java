package com.cts.corda.etf.util;

import com.cts.corda.etf.state.SecurityBuyState;
import com.cts.corda.etf.state.SecuritySellState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestHelper {

    public static List<SecuritySellState> getUnmatchedSecuritySellState(List<StateAndRef<SecuritySellState>> ref) {

        Map<UniqueIdentifier, SecuritySellState> requestsMap = new HashMap();
        for (StateAndRef<SecuritySellState> stateref : ref) {
            SecuritySellState securitySellState = stateref.getState().getData();
            if (!securitySellState.getStatus().equals("SELL_MATCHED")) {
                requestsMap.put(securitySellState.getLinearId(), securitySellState);
            }
        }

        for (StateAndRef<SecuritySellState> stateref : ref) {
            SecuritySellState securitySellState = stateref.getState().getData();
            if (securitySellState.getStatus().equals("SELL_MATCHED")) {
                requestsMap.remove(securitySellState.getLinearId());
            }
        }
        List<SecuritySellState> ls = new ArrayList<>();
        if (!requestsMap.values().isEmpty())
            ls.addAll(requestsMap.values());
        return ls;
    }


    public static List<SecurityBuyState> getUnmatchedSecurityBuyState(List<StateAndRef<SecurityBuyState>> ref) {

        Map<UniqueIdentifier, SecurityBuyState> requestsMap = new HashMap();
        for (StateAndRef<SecurityBuyState> stateref : ref) {
            SecurityBuyState securityBuyState = stateref.getState().getData();
            if (!securityBuyState.getStatus().equals("BUY_MATCHED")) {
                requestsMap.put(securityBuyState.getLinearId(), securityBuyState);
            }
        }

        for (StateAndRef<SecurityBuyState> stateref : ref) {
            SecurityBuyState securityBuyState = stateref.getState().getData();
            if (securityBuyState.getStatus().equals("BUY_MATCHED")) {
                requestsMap.remove(securityBuyState.getLinearId());
            }
        }

        List<SecurityBuyState> ls = new ArrayList<>();
        if (!requestsMap.values().isEmpty())
            ls.addAll(requestsMap.values());

        return ls;
    }

}
