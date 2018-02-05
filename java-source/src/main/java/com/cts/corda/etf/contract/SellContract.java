package com.cts.corda.etf.contract;

import com.cts.corda.etf.state.SecuritySellState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;

import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * A implementation of a basic smart contract in Corda.
 * <p>
 * This contract enforces rules regarding the creation of a valid [SecurityBuyState], which in turn encapsulates an [IOU].
 * <p>
 * For a new [IOU] to be issued onto the ledger, a transaction is required which takes:
 * - Zero input states.
 * - One output state: the new [IOU].
 * - An Create() command with the public keys of both the lender and the borrower.
 * <p>
 * All contracts must sub-class the [Contract] interface.
 */
public class SellContract implements Contract {
    public static final String SELL_SECURITY_CONTRACT_ID = "com.cts.corda.etf.contract.SellContract";

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    @Override
    public void verify(LedgerTransaction tx) {
        final CommandWithParties<Commands.Create> command = requireSingleCommand(tx.getCommands(), Commands.Create.class);
        requireThat(require -> {
            // Generic constraints around the IOU transaction.
            require.using("No inputs should be consumed when issuing an SellRequest.",
                    tx.getInputs().isEmpty());
            require.using("Only one output state should be created.",
                    tx.getOutputs().size() == 1);
            final SecuritySellState out = tx.outputsOfType(SecuritySellState.class).get(0);
            require.using("The seller and the depository cannot be the same entity.",
                    out.getSeller() != out.getBuyer());
            require.using("All of the participants must be signers.",
                    command.getSigners().containsAll(out.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList())));

            // IOU-specific constraints.
            require.using("The Security quantity value must be non-negative.",
                    out.getQuantity() > 0);

            return null;
        });
    }

    /**
     * This contract only implements one command, Create.
     */
    public interface Commands extends CommandData {
        class Create implements Commands {
        }
    }
}