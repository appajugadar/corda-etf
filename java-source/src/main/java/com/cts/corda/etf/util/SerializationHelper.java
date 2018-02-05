package com.cts.corda.etf.util;

import com.cts.corda.etf.state.SecurityBuyState;
import com.cts.corda.etf.state.SecuritySellState;
import net.corda.core.flows.FlowException;
import net.corda.core.utilities.UntrustworthyData;

public class SerializationHelper {

    public static SecuritySellState getSecuritySellState(UntrustworthyData<SecuritySellState> output) throws FlowException {
        return output.unwrap(new UntrustworthyData.Validator<SecuritySellState, SecuritySellState>() {
            @Override
            public SecuritySellState validate(SecuritySellState data) throws FlowException {
                return data;
            }
        });
    }

    public static SecurityBuyState getSecurityBuyState(UntrustworthyData<SecurityBuyState> output) throws FlowException {
        return output.unwrap(new UntrustworthyData.Validator<SecurityBuyState, SecurityBuyState>() {
            @Override
            public SecurityBuyState validate(SecurityBuyState data) throws FlowException {
                return data;
            }
        });
    }

}
