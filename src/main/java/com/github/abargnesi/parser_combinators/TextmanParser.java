package com.github.abargnesi.parser_combinators;

import java.util.Optional;

/// # Language
///
/// ## Types
///
/// - Position
///
///   ```
///   Beginning of line: 0 or ^
///   End of line: $
///   Beginning of next line: N^
///     Here "N" is a modifier.
///   End of next line: N$
///     Here "N" is a modifier.
///```
///
/// - Movement
///
///   ```
///   b for back
///   f for forward
///   l for lines
///   w for words
///```
///
/// - Action
///
///   ```
///   r:U replaces current position with upper case variant if available.
///   r:L replaces current position with lower case variant if available.
///   r'STR' replaces current position with STR
///
///   m for move
///   d for delete
///   i for insert
///```
///
/// - Repetition
///
///   ```
///   Parentheses used for grouping statements for repetition.
///(mw r:U)*
///```
///
/// Command grammar would be something like:
///
/// ```
///
/// <number>     ::= [0-9]+
/// <action>     ::= "m" | "d" | "i"
/// <command>    ::= <action> <number> | <action> <movement> <number>
/// <quantifier> ::= <number> | "+" | "*" | "?"
/// <process>    ::= "(" <command> ")" | "(" <process> ")" | "(" <process> ")" <quantifier>
///```
///
/// Blank space is not necessary but can be used for clear separation. It will be thrown away.
public class TextmanParser {

    static final Error NoError = new Error("", -1, "");

    record Error(String input, int position, String error) {
    }

    record Value<T>(T value) {
    }

    record Result<T, Error>(Optional<Value<T>> value, Error error) {
    }

    interface Parser<T> {

        Result<T, Error> parse(int p, String s);
    }

    interface Symbolic {
        String symbol();
    }

    enum Action implements Symbolic {
        MOVE("m"),
        DELETE("d"),
        INSERT("i");

        final String symbol;

        Action(String symbol) {
            this.symbol = symbol;
        }

        @Override
        public String symbol() {
            return symbol;
        }
    }

    static class ActionParser implements Parser<Action> {

        @Override
        public Result<Action, Error> parse(int p, String s) {
            for (Action action : Action.values()) {
                if (action.symbol().equals(s)) {
                    return new Result<>(Optional.of(new Value<>(action)), NoError);
                }
            }
            return new Result<>(Optional.empty(), new Error(s, p, "no action found"));
        }
    }

    static class NumberParser implements Parser<Long> {

        @Override
        public Result<Long, Error> parse(int p, String s) {
            StringBuilder b = null;
            for (int i = 0, l = s.length(); i < l; i++) {
                int cp = s.codePointAt(i);
                if (Character.isDigit(cp)) {
                    if (b == null) {
                        b = new StringBuilder();
                    }
                    b.append(Integer.parseInt(Character.toString(cp)));
                } else {
                    break;
                }
            }

            if (b == null || b.isEmpty()) {
                return new Result<>(Optional.empty(), new Error(s, p, "no number found"));
            } else {
                return new Result<>(Optional.of(new Value<>(Long.parseLong(b.toString()))), NoError);
            }
        }
    }

    public static void main(String[] args) {
        // example: <number>
        {
            assert new NumberParser().parse(0, "123").equals(new Result<>(Optional.of(new Value<>(123L)), NoError)) : ("123 did not yield 123");
            assert new NumberParser().parse(0, "123AB").equals(new Result<>(Optional.of(new Value<>(123L)), NoError)) : ("123AB did not yield 123");
            assert new NumberParser().parse(0, "AB123").equals(new Result<>(Optional.empty(), new Error("AB123", 0, "no number found"))) : "AB123 did not result in error";
            assert new NumberParser().parse(0, "  123").equals(new Result<>(Optional.empty(), new Error("  123", 0, "no number found"))) : "  123 did not result in error";
        }

        // example: <action>
        {
            assert new ActionParser().parse(0, "m").equals(new Result<>(Optional.of(new Value<>(Action.MOVE)), NoError)) : ("m did not yield Action.MOVE");
            assert new ActionParser().parse(0, "d").equals(new Result<>(Optional.of(new Value<>(Action.DELETE)), NoError)) : ("d did not yield Action.DELETE");
            assert new ActionParser().parse(0, "i").equals(new Result<>(Optional.of(new Value<>(Action.INSERT)), NoError)) : ("i did not yield Action.INSERT");
            assert new ActionParser().parse(0, " i").equals(new Result<>(Optional.empty(), new Error(" i", 0, "no action found"))) : (" i did not result in error");
            assert new ActionParser().parse(0, "i ").equals(new Result<>(Optional.empty(), new Error("i ", 0, "no action found"))) : ("i  did not result in error");
        }
    }
}
