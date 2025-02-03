package com.github.abargnesi.parser_combinators;

import java.util.Optional;
import java.util.function.Function;

/// # Language
///
/// Parser combinators for mathemtical expressions:
///
/// ```
/// # positive number (unary plus of Num)
/// 10
/// # negative number (unary minus of Num)
/// -10
/// # addition of (10, 50)
/// 10 + 50
/// # unary minus of expression
/// -(25 + 25)
/// # parenthesized expression
/// (25 - 5) * 10
/// # multiple expressions
/// ((50 * 7) + (7 * 50))
/// ```
///
/// PEG grammer:
///
/// ```
/// Num    <- [0-9]+
/// Plus   <- "+"
/// Minus  <- "-"
/// Mult   <- "*"
/// Div    <- "/"
/// Unary  <- (Plus / Minus) Num
/// Binary <- Num (Plus / Minus / Mult / Div) Num
/// Expr   <- Num / Unary / Binary / "(" Expr ")"
/// ```
///
/// Blank space is not necessary but can be used for clear separation. It will be thrown away.
public class MathExpressionParser {

  static final Error NoError = new Error("", -1, "");

  record Source(String input, int offset) {}

  record Error(String input, int position, String error) {}

  record Value<T>(T value) {}

  record Result<T, Error>(Source source, Optional<Value<T>> value, Error error) {}

  sealed interface Either<L, R> permits Either.Left, Either.Right {
    record Left<L, R>(L value) implements Either<L, R> {}

    record Right<L, R>(R value) implements Either<L, R> {}

    static <L, R> Either<L, R> ofLeft(L value) {
      return new Left<>(value);
    }

    static <L, R> Either<L, R> ofRight(R value) {
      return new Right<>(value);
    }
  }

  static <T> Parser<T> constant(final T constant) {
    return s -> new Result<>(s, Optional.of(new Value<>(constant)), NoError);
  }

  static <T> Parser<T> and(Parser<T> lhp, Parser<T> rhp) {
    return s -> {
      Result<T, Error> leftResult = lhp.parse(s);
      if (leftResult.value.isEmpty()) {
        return new Result<>(s, Optional.empty(), NoError);
      }
      return rhp.parse(s);
    };
  }

  static <T> Parser<T> or(Parser<T> lhp, Parser<T> rhp) {
    return s -> {
      Result<T, Error> result = lhp.parse(s);
      Optional<Value<T>> value = result.value;
      if (value.isPresent()) {
        return result;
      } else {
        return rhp.parse(s);
      }
    };
  }

  interface Parser<T> {

    Result<T, Error> parse(Source s);

    default <U> Parser<Either<T, U>> and(Parser<U> rhp) {
      return s -> {
        Result<T, Error> leftResult = this.parse(s);
        if (leftResult.value.isEmpty()) {
          return new Result<>(s, Optional.empty(), NoError);
        }
        Result<U, Error> rightResult = rhp.parse(leftResult.source);
        return rightResult
            .value
            .<Result<Either<T, U>, Error>>map(
                uValue ->
                    new Result<>(
                        rightResult.source,
                        Optional.of(new Value<>(Either.ofRight(uValue.value))),
                        NoError))
            .orElseGet(() -> new Result<>(s, Optional.empty(), NoError));
      };
    }

    default <U> Parser<U> bind(Function<Value<T>, Parser<U>> func) {
      return s -> {
        Result<T, Error> result = this.parse(s);
        Optional<Value<T>> value = result.value;
        if (value.isPresent()) {
          return func.apply(value.get()).parse(result.source);
        } else {
          return new Result<>(s, Optional.empty(), NoError);
        }
      };
    }
  }

  abstract static class SymbolParser implements Parser<String> {

    private final String symbol;

    SymbolParser(String symbol) {
      this.symbol = symbol;
    }

    @Override
    public Result<String, Error> parse(Source s) {
      if (s.input.startsWith(symbol, s.offset)) {
        // found the symbol
        return new Result<>(
            new Source(s.input, s.offset + symbol.length()),
            Optional.of(new Value<>(symbol)),
            NoError);
      } else {
        // did not find the symbol
        return new Result<>(s, Optional.empty(), NoError);
      }
    }
  }

  static class PlusParser extends SymbolParser {

    static final PlusParser singleton = new PlusParser();

    private PlusParser() {
      super("+");
    }
  }

  static class MinusParser extends SymbolParser {

    static final MinusParser singleton = new MinusParser();

    private MinusParser() {
      super("-");
    }
  }

  static class MultiplyParser extends SymbolParser {

    static final MultiplyParser singleton = new MultiplyParser();

    private MultiplyParser() {
      super("*");
    }
  }

  static class DivideParser extends SymbolParser {

    static final DivideParser singleton = new DivideParser();

    private DivideParser() {
      super("/");
    }
  }

  static class NumberParser implements Parser<Long> {

    @Override
    public Result<Long, Error> parse(Source s) {
      StringBuilder b = null;
      for (int i = s.offset, l = s.input.length(); i < l; i++) {
        int cp = s.input.codePointAt(i);
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
        // did not find a number
        return new Result<>(s, Optional.empty(), NoError);
      } else {
        // found a whole number
        String numberString = b.toString();
        return new Result<>(
            new Source(s.input, s.offset + numberString.length()),
            Optional.of(new Value<>(Long.parseLong(numberString))),
            NoError);
      }
    }
  }

  public static void main(String[] args) {
    // Num <- [0-9]+
    {
      final Parser<Long> np = new NumberParser();

      assert np.parse(new Source("123", 0))
              .equals(new Result<>(new Source("123", 3), Optional.of(new Value<>(123L)), NoError))
          : ("123 did not yield 123");

      assert np.parse(new Source("123AB", 0))
              .equals(new Result<>(new Source("123AB", 3), Optional.of(new Value<>(123L)), NoError))
          : ("123AB did not yield 123");

      assert np.parse(new Source("AB123", 0))
              .equals(new Result<>(new Source("AB123", 0), Optional.empty(), NoError))
          : "AB123 did not result in empty";

      assert np.parse(new Source("  123", 0))
              .equals(new Result<>(new Source("  123", 0), Optional.empty(), NoError))
          : "  123 did not result in empty";
    }

    // Plus   <- "+"
    {
      final PlusParser pp = PlusParser.singleton;

      assert pp.parse(new Source("+", 0))
              .equals(new Result<>(new Source("+", 1), Optional.of(new Value<>("+")), NoError))
          : "+ did not yield plus symbol";

      assert pp.parse(new Source(" +", 0))
              .equals(new Result<>(new Source(" +", 0), Optional.empty(), NoError))
          : "' +' did not result in empty";

      assert pp.parse(new Source("%", 0))
              .equals(new Result<>(new Source("%", 0), Optional.empty(), NoError))
          : "% did not result in empty";
    }

    // Minus  <- "-"
    {
      final MinusParser mp = MinusParser.singleton;

      assert mp.parse(new Source("-", 0))
              .equals(new Result<>(new Source("-", 1), Optional.of(new Value<>("-")), NoError))
          : "- did not yield minus symbol";

      assert mp.parse(new Source(" -", 0))
              .equals(new Result<>(new Source(" -", 0), Optional.empty(), NoError))
          : "' -' did not result in empty";

      assert mp.parse(new Source("%", 0))
              .equals(new Result<>(new Source("%", 0), Optional.empty(), NoError))
          : "% did not result in empty";
    }

    // Parser OR
    {
      or(MinusParser.singleton, PlusParser.singleton);
    }

    // Parser AND
    {
      and(MinusParser.singleton, PlusParser.singleton);
    }

    // Unary  <- (Plus / Minus) Num
    {
      Result<Either<String, Long>, Error> result =
          MinusParser.singleton
              .and(new NumberParser().bind((number) -> constant(-(number.value))))
              .parse(new Source("-2312", 0));

      assert result.equals(
              new Result<>(
                  new Source("-2312", 5),
                  Optional.of(new Value<>(Either.<String, Long>ofRight(-2312L))),
                  NoError))
          : "-2312 did not result in unary negation value";
    }

    // Binary <- Num (Plus / Minus / Mult / Div) Num
    {
      // TODO - Operation types should either result in itself or not. They are essentially typed tokens.
      // TODO - Try to refactor the type system for them.
      Parser<Long> binaryParser =
          new NumberParser()
              .bind(
                  lhs ->
                      or(MinusParser.singleton, PlusParser.singleton)
                          .bind(
                              op ->
                                  new NumberParser().bind(rhs -> constant(lhs.value + rhs.value))));
      Result<Long, Error> result = binaryParser.parse(new Source("125+517", 0));
      System.out.println(result);
    }
  }
}
