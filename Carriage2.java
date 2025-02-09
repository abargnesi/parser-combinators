import java.util.regex.Matcher;
import java.util.regex.Pattern;

static void p(Object... values) {
  pd(" ", values);
}

static void pl(Object... values) {
  pd("\n", values);
}

static void pd(String delim, Object... values) {
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

  // Parse zero words.
  {
    Parser<String> wordStarParser = zeroOrMore(wordParser);
    Match<String> match = wordStarParser.parse(new Source("", 0));
    p(match);
  }

  // Parse one word.
  {
    Match<String> match = wordParser.parse(new Source("ThereIsSomethingInTheWater", 0));
    p(match);
  }

  // Parse zero or more words.
  {
    Parser<String> wordStarParser = zeroOrMore(wordParser);
    Match<String> match = wordStarParser.parse(new Source("ThereIsSomethingInTheWater", 0));
    p(match);
  }
}

/** Higher order parser that matches zero or more occurrences of another parser. */
static <T> Parser<T> zeroOrMore(Parser<T> p) {
  return s -> {
    Source nextSource = s;
    Match<T> lastSuccess = p.parse(nextSource);
    Match<T> match = lastSuccess;

    while (match.error == null) {
      nextSource = match.source;
      
      match = p.parse(nextSource);
      // retain last successful match
      lastSuccess = match.error == null ? match : lastSuccess;
    }

    return lastSuccess;
  };
}