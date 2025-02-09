import java.util.regex.Matcher;
import java.util.regex.Pattern;

static void p(String... values) {
  pd(" ", values);
}

static void pl(String... values) {
  pd("\n", values);
}

static void pd(String delim, String... values) {
  if (values == null || values.length == 0) {
    return;
  }

  System.out.print(values[0]);

  for (int i = 1, l = values.length; i < l; i++) {
    System.out.print(delim);
    System.out.print(values[i]);
  }
  System.out.println("");
}

record Source(String text, int offset) {

  String nextMatch(String pattern) {
    Pattern p = Pattern.compile(pattern);
    Matcher m = p.matcher(text);
    if (m.find(offset)) {
      return m.group();
    }
    return null;
  }
}

record Error(String msg) {
  
}

record Match<T>(T value, Source source, Error error) {
  
}

interface Parser<T> {
  Match<T> parse(Source source);
}

void main() {
  p("Hello", "Parser", "Combinators", "!");

  Parser<String> wordParser = s -> {
    String match = s.nextMatch("\\p{javaUpperCase}\\p{javaLowerCase}*");
    
    return match == null ?
      new Match<>(null, s, new Error("Did not match capital word.")) :
      new Match<>(match, new Source(s.text, s.offset + match.length()), null);
  };

  {
    Match<String> match = wordParser.parse(new Source("ThereIsSomethingInTheWater", 0));
    p(match.value);
  }

  {
    Parser<String> wordStarParser = zeroOrMore(wordParser);
    Match<String> match = wordStarParser.parse(new Source("ThereIsSomethingInTheWater", 0));
    p(match.value);
  }
}

static <T> Parser<T> zeroOrMore(Parser<T> p) {
  return s -> {
    Match<T> m = null;
    do {
      System.out.println(s);
      m = p.parse(s);
      System.out.println(m);
    } while (m.error == null);

    return m;
  };
}