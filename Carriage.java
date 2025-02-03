import java.util.Optional;

record Error(String message) {
}

record Source(int offset, String text) {
}

sealed interface Result<T> permits Result.Value, Result.Error {
    record Value<T>(T value) implements Result<T> {
    }

    record Error<T>(Source source, String expected, String actual) implements Result<T> {
    }

    static <T> Value<T> ofValue(T value) {
        return new Value<T>(value);
    }

    static <T> Error<T> ofError(Source source, String expected, String actual) {
        return new Error<T>(source, expected, actual);
    }
}

interface Parser<T> {
    Result<T> parse(Source source);
}

void main() {
  Result<String> valueResult = Result.ofValue("success");
  System.out.println(valueResult.value);

  Result<String> errorResult = Result.ofError(new Source(0, "ABC"), "number", "letter");
  System.out.println(errorResult);
    
  System.out.println("done");
}

class NumberParser implements Parser<Integer> {

    public Result<Integer> parse(Source source) {
      // StringBuilder b = null;
      // for (int i = s.offset, l = source.text.length(); i < l; i++) {
      //   int cp = source.text.codePointAt(i);
      //   if (Character.isDigit(cp)) {
      //     if (b == null) {
      //       b = new StringBuilder();
      //     }
      //     b.append(Integer.parseInt(Character.toString(cp)));
      //   } else {
      //     break;
      //   }
      // }

      // if (b == null || b.isEmpty()) {
      //   // did not find a number
      //   return new Result<>(s, Optional.empty(), NoError);
      // } else {
      //   // found a whole number
      //   String numberString = b.toString();
      //   return new Result<>(
      //       new Source(s.input, s.offset + numberString.length()),
      //       Optional.of(new Value<>(Long.parseLong(numberString))),
      //       NoError);
      // }
      return null;
    }
}