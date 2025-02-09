# parser-combinators

Exploring use of parser combinators in language modeling.

Carriage - Text Manipulation Language

(g-w :up)*

Concise shorthand for text manipulation.

## Input:

```
Here lies a paragraph to demonstrate Carriage.
Feature list:
- position-oriented editing
- token-based concepts
- action oriented functions
- user-defined functions
```

## Examples:

Capitalize specific words.

Short:
```
(g-w :up)*
```

Documented:
```
(
  | Go to the next occurrence of hyphen(-) then to the next word(w).
  g-w

  | Call :up function on word to capitalize.
  :up

| Match zero or more times.
)*
```

---

Downcase each occurrence of `Carriage`.

Short:
```
(gw'Carriage' :dn)*
```

Documented:
```
(
  | Go to the next occurrence of Carriage.
  gw'Carriage'

  | Downcase the word.
  :dn

| Match zero or more times.
)*
```

PEG grammer:

```
Char    <- \w
Literal <- "'" Char+ "'"
Num    <- [0-9]+
```

Blank space is not necessary but can be used for clear separation. It will be thrown away.