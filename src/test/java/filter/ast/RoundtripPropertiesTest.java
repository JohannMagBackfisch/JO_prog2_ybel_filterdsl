package filter.ast;

import filter.ast.builder.AstBuilderPattern;
import filter.ast.builder.AstBuilderVisitor;
import filter.ast.builder.AstBuilders;
import filter.ast.nodes.Expr;
import filter.ast.printer.AstPrinter;
import net.jqwik.api.*;

public class RoundtripPropertiesTest {

  @Property
  boolean roundtripPattern(@ForAll("simpleQueries") String query) {
    Expr first = AstBuilders.fromQuery(query, ctx -> new AstBuilderPattern().translate(ctx));
    String printed = AstPrinter.toString(first);
    Expr second = AstBuilders.fromQuery(printed, ctx -> new AstBuilderPattern().translate(ctx));
    return AstPrinter.toString(first).equals(AstPrinter.toString(second));
  }

  @Property
  boolean roundtripVisitor(@ForAll("simpleQueries") String query) {
    Expr first = AstBuilders.fromQuery(query, ctx -> new AstBuilderVisitor().translate(ctx));
    String printed = AstPrinter.toString(first);
    Expr second = AstBuilders.fromQuery(printed, ctx -> new AstBuilderVisitor().translate(ctx));
    return AstPrinter.toString(first).equals(AstPrinter.toString(second));
  }

  @Property
  boolean patternUndVisitorStimmenUeberein(@ForAll("simpleQueries") String query) {
    Expr pattern = AstBuilders.fromQuery(query, ctx -> new AstBuilderPattern().translate(ctx));
    Expr visitor = AstBuilders.fromQuery(query, ctx -> new AstBuilderVisitor().translate(ctx));
    return AstPrinter.toString(pattern).equals(AstPrinter.toString(visitor));
  }

  @Property
  boolean verschraenkterRoundtrip(@ForAll("simpleQueries") String query) {
    Expr patternResult =
        AstBuilders.fromQuery(query, ctx -> new AstBuilderPattern().translate(ctx));
    String printed = AstPrinter.toString(patternResult);
    Expr visitorResult =
        AstBuilders.fromQuery(printed, ctx -> new AstBuilderVisitor().translate(ctx));
    return AstPrinter.toString(patternResult).equals(AstPrinter.toString(visitorResult));
  }

  @Property
  boolean simplifyIdempotent(@ForAll("simpleQueries") String query) {
    Expr e = AstBuilders.fromQuery(query, ctx -> new AstBuilderPattern().translate(ctx));
    Expr onceSimplifed = AstBuilders.simplify(e);
    Expr twiceSimplified = AstBuilders.simplify(onceSimplifed);
    return AstPrinter.toString(onceSimplifed).equals(AstPrinter.toString(twiceSimplified));
  }

  @Property
  boolean andEnhaeltBeideTeilausdruecke(
      @ForAll("comparisons") String left, @ForAll("comparisons") String right) {
    String query = left + " and " + right;
    Expr e = AstBuilders.fromQuery(query, ctx -> new AstBuilderPattern().translate(ctx));
    // Der gedruckte AST eines And muss " and " enthalten
    return e instanceof Expr.And;
  }

  @Property
  boolean orErgibtOrKnoten(
      @ForAll("comparisons") String left, @ForAll("comparisons") String right) {
    String query = left + " or " + right;
    Expr e = AstBuilders.fromQuery(query, ctx -> new AstBuilderPattern().translate(ctx));
    return e instanceof Expr.Or;
  }

  @Property
  boolean doppeltesNotWirdVereinfacht(@ForAll("comparisons") String comp) {
    Expr einmal = AstBuilders.fromQuery(comp, ctx -> new AstBuilderPattern().translate(ctx));
    Expr doppeltNot = AstBuilders.simplify(new Expr.Not(new Expr.Not(einmal)));
    return AstPrinter.toString(einmal).equals(AstPrinter.toString(doppeltNot));
  }

  // ---------- @Provide-Methods for Arbitraries ----------

  @Provide
  Arbitrary<String> fields() {
    return Arbitraries.of("title", "artist", "genre", "year");
  }

  @Provide
  Arbitrary<String> stringLiterals() {
    return Arbitraries.strings()
        .withChars("abcxyz")
        .ofMinLength(1)
        .ofMaxLength(5)
        .map(s -> "\"" + s + "\"");
  }

  @Provide
  Arbitrary<String> numberLiterals() {
    return Arbitraries.integers().between(1900, 2025).map(Object::toString);
  }

  @Provide
  Arbitrary<String> comparisons() {
    Arbitrary<String> ops = Arbitraries.of("==", "!=", "<", "<=", ">", ">=");

    Arbitrary<String> stringComp =
        Combinators.combine(fields(), ops, stringLiterals())
            .as((f, op, lit) -> f + " " + op + " " + lit);

    Arbitrary<String> numberComp =
        Combinators.combine(Arbitraries.of("year"), ops, numberLiterals())
            .as((f, op, lit) -> f + " " + op + " " + lit);

    return Arbitraries.oneOf(stringComp, numberComp);
  }

  @Provide
  Arbitrary<String> simpleQueries() {
    return comparisons()
        .list()
        .ofMinSize(1)
        .ofMaxSize(3)
        .map(
            list -> {
              if (list.size() == 1) return list.getFirst();
              StringBuilder sb = new StringBuilder();
              for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                  String conn = Arbitraries.of(" and ", " or ").sample();
                  sb.append(conn);
                }
                sb.append(list.get(i));
              }
              return sb.toString();
            });
  }
}
