package com.cts.corda.etf.contract;

import kotlin.Unit;
import net.corda.core.contracts.*;
import net.corda.core.crypto.NullKeys;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;


public class SecurityStock implements Contract {

    public static final String SECURITY_STOCK_CONTRACT = "com.cts.corda.etf.contract.SecurityStock";

    private static <T> T onlyElementOf(Iterable<T> iterable) {
        Iterator<T> iter = iterable.iterator();
        T item = iter.next();
        if (iter.hasNext()) {
            throw new IllegalArgumentException("Iterable has more than one element!");
        }
        return item;
    }

    @NotNull
    private List<CommandWithParties<Commands>> extractCommands(@NotNull LedgerTransaction tx) {
        return tx.getCommands()
                .stream()
                .filter((CommandWithParties<CommandData> command) -> command.getValue() instanceof Commands)
                .map((CommandWithParties<CommandData> command) -> new CommandWithParties<>(command.getSigners(), command.getSigningParties(), (Commands) command.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {

        // Group by everything except owner: any modification to the CP at all is considered changing it fundamentally.
        final List<LedgerTransaction.InOutGroup<State, State>> groups = tx.groupStates(State.class, State::withoutOwner);

        // There are two possible things that can be done with this CP. The first is trading it. The second is redeeming
        // it for cash on or after the maturity date.
        final List<CommandWithParties<CommandData>> commands = tx.getCommands().stream().filter(
                it -> it.getValue() instanceof Commands
        ).collect(Collectors.toList());
        final CommandWithParties<CommandData> command = onlyElementOf(commands);
        final TimeWindow timeWindow = tx.getTimeWindow();

        for (final LedgerTransaction.InOutGroup<State, State> group : groups) {
            final List<State> inputs = group.getInputs();
            final List<State> outputs = group.getOutputs();
            if (command.getValue() instanceof Commands.Move) {
                final CommandWithParties<Commands.Move> cmd = requireSingleCommand(tx.getCommands(), Commands.Move.class);
                // There should be only a single input due to aggregation above
                /*final State input = onlyElementOf(inputs);

                if (!cmd.getSigners().contains(input.getOwner().getOwningKey()))
                    throw new IllegalStateException("Failed requirement: the transaction is signed by the owner of the CP");

                // Check the output CP state is the same as the input state, ignoring the owner field.
                if (outputs.size() != 1) {
                    throw new IllegalStateException("the state is propagated");
                }
                */
            } else if (command.getValue() instanceof Commands.Issue) {
                final CommandWithParties<Commands.Issue> cmd = requireSingleCommand(tx.getCommands(), Commands.Issue.class);
                final State output = onlyElementOf(outputs);
                final Instant time = null == timeWindow
                        ? null
                        : timeWindow.getUntilTime();

                requireThat(require -> {
                   /* require.using("output values sum to more than the inputs", inputs.isEmpty());
                    require.using("output values sum to more than the inputs", output.getQuantity() > 0);
                    require.using("must be timestamped", timeWindow != null);
                    require.using("output states are issued by a command signer", cmd.getSigners().contains(output.getIssuance().getParty().getOwningKey()));*/
                    return Unit.INSTANCE;
                });
            }
        }
    }

    public TransactionBuilder generateIssue(@NotNull PartyAndReference issuance, String securityName, Long quantity, @NotNull Party notary, Integer encumbrance) {
        State state = new State(issuance, issuance.getParty(), securityName, quantity);

        TransactionState output = new TransactionState<>(state, SECURITY_STOCK_CONTRACT, notary, encumbrance);

        return new TransactionBuilder(notary).withItems(output, new Command<>(new Commands.Issue(), issuance.getParty().getOwningKey()));
    }

    public TransactionBuilder generateIssue(@NotNull PartyAndReference issuance, String securityName, Long quantity, @NotNull Party notary) {
        return generateIssue(issuance, securityName, quantity, notary, null);
    }

    public void generateMove(TransactionBuilder tx, StateAndRef<State> security, AbstractParty newOwner) {
        tx.addInputState(security);

        tx.addOutputState(new TransactionState<>(new State(security.getState().getData().getIssuance(), newOwner, security.getState().getData().getSecurityName(), security.getState().getData().getQuantity()),
                SECURITY_STOCK_CONTRACT, security.getState().getNotary(), security.getState().getEncumbrance()));

        tx.addCommand(new Command<>(new Commands.Move(), security.getState().getData().getOwner().getOwningKey()));
    }


    public interface Commands extends CommandData {
        class Move implements Commands {
            @Override
            public boolean equals(Object obj) {
                return obj instanceof Move;
            }
        }

        class Redeem implements Commands {
            @Override
            public boolean equals(Object obj) {
                return obj instanceof Redeem;
            }
        }

        class Issue implements Commands {
            @Override
            public boolean equals(Object obj) {
                return obj instanceof Issue;
            }
        }
    }

    public static class State implements OwnableState {
        private PartyAndReference issuance;
        private AbstractParty owner;
        private String securityName;
        private Long quantity;


        public State() {
        }  // For serialization

        public State(PartyAndReference issuance, AbstractParty owner, String securityName, Long quantity) {
            this.issuance = issuance;
            this.owner = owner;
            this.securityName = securityName;
            this.quantity = quantity;

        }

        public State copy() {
            return new State(this.issuance, this.owner, this.securityName, this.quantity);
        }

        @NotNull
        @Override
        public CommandAndState withNewOwner(@NotNull AbstractParty newOwner) {
            return new CommandAndState(new Commands.Move(), new State(this.issuance, newOwner, this.securityName, this.quantity));
        }

        public PartyAndReference getIssuance() {
            return issuance;
        }

        @NotNull
        public AbstractParty getOwner() {
            return owner;
        }

        public String getSecurityName() {
            return securityName;
        }

        public Long getQuantity() {
            return quantity;
        }

        @Override
        public boolean equals(Object that) {
            if (this == that) return true;
            if (that == null || getClass() != that.getClass()) return false;

            State state = (State) that;

            if (issuance != null ? !issuance.equals(state.getIssuance()) : state.getIssuance() != null) return false;
            if (owner != null ? !owner.equals(state.getOwner()) : state.getOwner() != null) return false;
            if (securityName != null ? !securityName.equals(state.getSecurityName()) : state.getSecurityName() != null)
                return false;
            if (quantity != null ? !quantity.equals(state.getQuantity()) : state.getQuantity() != null)
                return false;
            return true;
        }

        @Override
        public int hashCode() {
            int result = issuance != null ? issuance.hashCode() : 0;
            result = 31 * result + (owner != null ? owner.hashCode() : 0);
            result = 31 * result + (securityName != null ? securityName.hashCode() : 0);
            result = 31 * result + (quantity != null ? quantity.hashCode() : 0);
            return result;
        }

        State withoutOwner() {
            return new State(issuance, new AnonymousParty(NullKeys.NullPublicKey.INSTANCE), securityName, quantity);
        }

        @NotNull
        @Override
        public List<AbstractParty> getParticipants() {
            return Collections.singletonList(this.owner);
        }
    }
}
