import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Function;

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

record Source(int offset, String text) {
}

sealed interface Result<T> permits Result.Value, Result.Error {
    record Value<T>(Source source, T value) implements Result<T> {

        @Override
        public Optional<T> getValue() {
            return Optional.of(value);
        }

        @Override
        public Source getSource() {
            return source;
        }

        @Override
        public Optional<String> getErrorMessage() {
            return Optional.empty();
        }
    }

    record Error<T>(Source source, String expected, String actual) implements Result<T> {

        @Override
        public Optional<T> getValue() {
            return Optional.empty();
        }
        
        @Override
        public Source getSource() {
            return source;
        }

        @Override
        public Optional<String> getErrorMessage() {
            return Optional.of("""
              Error occurred at %d in '%s'.
                expected: %s
                actual:   %s
              """);
        }
    }

    static <T> Value<T> ofValue(Source source, T value) {
        return new Value<T>(source, value);
    }

    static <T> Error<T> ofError(Source source, String expected, String actual) {
        return new Error<>(source, expected, actual);
    }

    static <T> Error<T> ofError(Error<?> copy) {
        return new Error<>(copy.source, copy.expected, copy.actual);
    }

    Optional<T> getValue();

    Source getSource();

    Optional<String> getErrorMessage();
}

static <T> void valueTest(String text, Parser<T> parser, Result.Value<T> expected) {
    Result<T> result = parser.parse(new Source(0, text));
    switch(result) {
        case Result.Value<T> v -> {
            if (!v.equals(expected)) {
                throw new AssertionError("failed: result did not equal expected");
            } else {
                System.out.printf("""
                    Value Successful
                    Type: %s
                    Source: %s
                    Value: %s
                    %n""", v.getClass(), v.getSource(), v.getValue());
            }
        }
        case Result.Error<T> _ -> throw new AssertionError("failed: parse failure for %s".formatted(text));
    };
}

static <T> void errorTest(String text, Parser<T> parser, Result.Error<T> expected) {
    Result<T> result = parser.parse(new Source(0, text));
    switch(result) {
        case Result.Error<T> e -> {
            if (!e.equals(expected)) {
                throw new AssertionError("failed: error did not equal expected");
            } else {
                System.out.printf("""
                    Error Successful
                    Type: %s
                    Source: %s
                    ErrorMessage: %s
                    %n""", e.getClass(), e.getSource(), e.getErrorMessage());
            }
        }
        case Result.Value<T> _ -> throw new AssertionError("failed: received value instead of error");
    };
}

void main() {
  valueTest("5060", new NumberParser(), Result.ofValue(new Source(4, "5060"), 5060));
  errorTest("ABC", new NumberParser(), Result.ofError(new Source(0, "ABC"), "number", "empty"));

  valueTest(
      "'Carriage'",
      Parser.token("'").and(Parser.word().map(w -> Parser.token("'").and(Parser.value(w.getValue().get())))),
      Result.ofValue(new Source(10, "'Carriage'"), "Carriage"));

  errorTest(
      "'Carr",
      Parser.oneOrMore(Parser. token("'").and( Parser.word().map(w -> Parser.token("'").and(Parser.value(w.getValue().get()))))),
      Result.ofError(new Source(5, "'Carr"), "token '", "not the token '"));

  valueTest(
      "'Carriage''Text''Manipulation''Language'",
      Parser.oneOrMore(Parser. token("'").and( Parser.word().map(w -> Parser.token("'").and(Parser.value(w.getValue().get()))))),
      Result.ofValue(new Source(40, "'Carriage''Text''Manipulation''Language'"), List.of("Carriage", "Text", "Manipulation", "Language")));

  System.out.println("done");
}

/// # Carriage - Text Manipulation Language
///
/// Concise shorthand for text manipulation.
///
/// ## Input:
///
/// ```
/// Here lies a paragraph to demonstrate Carriage.
/// Feature list:
/// - position-oriented editing
/// - token-based concepts
/// - action oriented functions
/// - user-defined functions
/// ```
///
/// ## Examples:
///
/// Capitalize specific words.
///
/// Short:
/// ```
/// (g-w :up)*
/// ```
///
/// Documented:
/// ```
/// (
///   | Go to the next occurrence of hyphen(-) then to the next word(w).
///   g-w
///
///   | Call :up function on word to capitalize.
///   :up
///
/// | Match zero or more times.
/// )*
/// ```
///
/// ---
///
/// Downcase each occurrence of `Carriage`.
///
/// Short:
/// ```
/// (gw'Carriage' :dn)*
/// ```
///
/// Documented:
/// ```
/// (
///   | Go to the next occurrence of Carriage.
///   gw'Carriage'
///
///   | Downcase the word.
///   :dn
///
/// | Match zero or more times.
/// )*
/// ```
///
/// PEG grammer:
///
/// ```
/// Char    <- \w
/// Literal <- "'" Char+ "'"
/// Num    <- [0-9]+
/// ```
///
/// Blank space is not necessary but can be used for clear separation. It will be thrown away.

interface Parser<T> {
    Result<T> parse(Source source);

    default <U> Parser<U> and(Parser<U> other) {
        return source -> {
            Result<T> left = parse(source);
            return switch(left) {
                case Result.Value<T> _ -> {
                    Result<U> right = other.parse(left.getSource());
                    yield switch(right) {
                        case Result.Value<U> v -> v;
                        case Result.Error<U> re -> Result.ofError(re.source, re.expected, re.actual);
                    };
                }
                case Result.Error<T> e -> Result.ofError(e);
            };
        };
    }

    default <U> Parser<Either<T, U>> or(Parser<U> other) {
        return source -> {
            Result<T> left = parse(source);
            return switch(left) {
                case Result.Value<T> v -> Result.ofValue(left.getSource(), Either.ofLeft(v.value));
                case Result.Error<T> _ -> {
                    Result<U> right = other.parse(source);
                    yield switch(right) {
                        case Result.Value<U> v -> Result.ofValue(right.getSource(), Either.ofRight(v.value));
                        case Result.Error<U> re -> Result.ofError(re.source, re.expected, re.actual);
                    };
                }
            };
        };
    }

    default <U> Parser<U> map(Function<Result<T>, Parser<U>> func) {
        return source -> switch(parse(source)) {
            case Result.Value<T> v -> func.apply(v).parse(v.source);
            case Result.Error<T> e -> Result.ofError(e);
        };
    }

    static <T> Parser<List<T>> zeroOrMore(Parser<T> parser) {
        return source -> {
            List<T> results = new ArrayList<>();
            Source nextSource = source;
            boolean stop = false;
            while (!stop) {
                Result<T> result = parser.parse(nextSource);
                switch(result) {
                    case Result.Value<T> v -> results.add(v.value);
                    case Result.Error<T> _ -> stop = true;
                }

                nextSource = result.getSource();
            }
            return Result.ofValue(nextSource, results);
        };
    }

    static <T> Parser<List<T>> oneOrMore(Parser<T> parser) {
        return source -> {
            List<T> results = new ArrayList<>();
            Source nextSource = source;

            // require first occurrence
            Result<T> result = parser.parse(nextSource);
            if (result instanceof Result.Value<T> v) {
                results.add(v.value);
            } else if (result instanceof Result.Error<T> e) {
                return Result.ofError(e);
            }

            // advance past first occurrence
            nextSource = result.getSource();

            // parse remaining occurrences (or none)
            boolean stop = false;
            while (!stop) {
                result = parser.parse(nextSource);
                switch(result) {
                    case Result.Value<T> v -> results.add(v.value);
                    case Result.Error<T> _ -> stop = true;
                }

                nextSource = result.getSource();
            }
            return Result.ofValue(nextSource, results);
        };
    }

    static <T> ValueParser<T> value(T value) {
        return new ValueParser<>(value);
    }

    static TokenParser token(String token) {
        return new TokenParser(token);
    }

    static WordParser word() {
        return new WordParser();
    }

    static NumberParser num() {
        return new NumberParser();
    }
}

static class ValueParser<T> implements Parser<T> {

    private final T value;

    ValueParser(T value) {
        this.value = value;
    }

    @Override
    public Result<T> parse(Source source) {
        return Result.ofValue(source, value);
    }
}

static class TokenParser implements Parser<String> {

    private final String token;

    TokenParser(String token) {
        this.token = token;
    }

    @Override
    public Result<String> parse(Source source) {
        return source.text.startsWith(token, source.offset) ?
                Result.ofValue(new Source(source.offset + token.length(), source.text), token) :
                Result.ofError(source, "token " + token, "not the token " + token);
    }
}

static class WordParser implements Parser<String> {

    @Override
    public Result<String> parse(Source source) {
        StringBuilder b = null;
        for (int i = source.offset, l = source.text.length(); i < l; i++) {
            int cp = source.text.codePointAt(i);
            if (Character.isLetterOrDigit(cp)) {
                if (b == null) {
                    b = new StringBuilder();
                }
                b.append(Character.toString(cp));
            } else {
                break;
            }
        }

        if (b == null || b.isEmpty()) {
            // did not find a number
            return Result.ofError(source, "word character", "empty");
        } else {
            // found a whole number
            String s = b.toString();
            return Result.ofValue(new Source(source.offset + s.length(), source.text), s);
        }
    }
}

static class NumberParser implements Parser<Integer> {

    public Result<Integer> parse(Source source) {
      StringBuilder b = null;
      for (int i = source.offset, l = source.text.length(); i < l; i++) {
        int cp = source.text.codePointAt(i);
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
        return Result.ofError(source, "number", "empty");
      } else {
        // found a whole number
        String s = b.toString();
        return Result.ofValue(
            new Source(source.offset + s.length(), source.text),
            Integer.parseInt(s));
      }
    }
}