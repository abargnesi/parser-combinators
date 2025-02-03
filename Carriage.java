import java.util.Optional;

record Error(String message) {
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

    Optional<T> getValue();

    Source getSource();

    Optional<String> getErrorMessage();
}

interface Parser<T> {
    Result<T> parse(Source source);
}

void main() {
  Source source = new Source(0, "ABC123");
    
  Result<String> valueResult = Result.ofValue(source, "success");
  System.out.println("""
    Type: %s
    Source: %s
    Value: %s
    """.formatted(valueResult.getClass(), valueResult.getSource(), valueResult.getValue()));

  Result<String> errorResult = Result.ofError(source, "number", "letter");
  System.out.println("""
    Type: %s
    Source: %s
    Value: %s
    """.formatted(errorResult.getClass(), errorResult.getSource(), errorResult.getValue()));

  {
    NumberParser num = new NumberParser();
    Result<Integer> r = num.parse(new Source(0, "5060"));
    System.out.println(
      switch(r) {
        case Result.Value v -> v.getValue();
        case Result.Error e -> e.getErrorMessage();
      });
  }

  {
    NumberParser num = new NumberParser();
    Result<Integer> r = num.parse(new Source(0, "ABC"));
    System.out.println(
      switch(r) {
        case Result.Value v -> v.getValue();
        case Result.Error e -> e.getErrorMessage();
      });
  }

  System.out.println("done");
}

class NumberParser implements Parser<Integer> {

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