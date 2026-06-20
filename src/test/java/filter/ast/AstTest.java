package filter.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;

import filter.ast.builder.AstBuilderPattern;
import filter.ast.builder.AstBuilderVisitor;
import filter.ast.builder.AstBuilders;
import filter.ast.nodes.CompOp;
import filter.ast.nodes.Expr;
import filter.ast.nodes.Value;
import filter.ast.printer.AstPrinter;
import java.util.List;
import org.junit.jupiter.api.Test;

public class AstTest {

  private Expr parsePattern(String query) {
    return AstBuilders.fromQuery(query, ctx -> new AstBuilderPattern().translate(ctx));
  }

  private Expr parseVisitor(String query) {
    return AstBuilders.fromQuery(query, ctx -> new AstBuilderVisitor().translate(ctx));
  }

  @Test
  void einfacherStringVergleich() {
    Expr expected = new Expr.Comparison("artist", CompOp.EQ, new Value.Str("Beatles"));
    assertEquals(expected, parsePattern("artist == \"Beatles\""));
    assertEquals(expected, parseVisitor("artist == \"Beatles\""));
  }

  @Test
  void einfacherZahlVergleich() {
    Expr expected = new Expr.Comparison("year", CompOp.EQ, new Value.Num(1965));
    assertEquals(expected, parsePattern("year == 1965"));
    assertEquals(expected, parseVisitor("year == 1965"));
  }

  @Test
  void alleVergleichsoperatoren() {
    assertEquals(
        new Expr.Comparison("year", CompOp.LT, new Value.Num(1970)), parsePattern("year < 1970"));
    assertEquals(
        new Expr.Comparison("year", CompOp.LE, new Value.Num(1970)), parsePattern("year <= 1970"));
    assertEquals(
        new Expr.Comparison("year", CompOp.GT, new Value.Num(1970)), parsePattern("year > 1970"));
    assertEquals(
        new Expr.Comparison("year", CompOp.GE, new Value.Num(1970)), parsePattern("year >= 1970"));
    assertEquals(
        new Expr.Comparison("artist", CompOp.NE, new Value.Str("Beatles")),
        parsePattern("artist != \"Beatles\""));
  }

  @Test
  void andVerknuepfung() {
    Expr expected =
        new Expr.And(
            new Expr.Comparison("artist", CompOp.EQ, new Value.Str("Beatles")),
            new Expr.Comparison("year", CompOp.EQ, new Value.Num(1965)));
    assertEquals(expected, parsePattern("artist == \"Beatles\" and year == 1965"));
    assertEquals(expected, parseVisitor("artist == \"Beatles\" and year == 1965"));
  }

  @Test
  void orVerknuepfung() {
    Expr expected =
        new Expr.Or(
            new Expr.Comparison("artist", CompOp.EQ, new Value.Str("Beatles")),
            new Expr.Comparison("year", CompOp.EQ, new Value.Num(1965)));
    assertEquals(expected, parsePattern("artist == \"Beatles\" or year == 1965"));
    assertEquals(expected, parseVisitor("artist == \"Beatles\" or year == 1965"));
  }

  @Test
  void notAusdruck() {
    Expr expected =
        new Expr.Not(new Expr.Comparison("artist", CompOp.EQ, new Value.Str("Beatles")));
    assertEquals(expected, parsePattern("not artist == \"Beatles\""));
    assertEquals(expected, parseVisitor("not artist == \"Beatles\""));
  }

  @Test
  void inListAusdruck() {
    Expr expected = new Expr.InList("genre", List.of(new Value.Str("rock"), new Value.Str("jazz")));
    assertEquals(expected, parsePattern("genre in (\"rock\", \"jazz\")"));
    assertEquals(expected, parseVisitor("genre in (\"rock\", \"jazz\")"));
  }

  @Test
  void inListMitZahlen() {
    Expr expected =
        new Expr.InList(
            "year", List.of(new Value.Num(1965), new Value.Num(1970), new Value.Num(1975)));
    assertEquals(expected, parsePattern("year in (1965, 1970, 1975)"));
    assertEquals(expected, parseVisitor("year in (1965, 1970, 1975)"));
  }

  @Test
  void komplexerAusdruck() {
    // genre in ("rock", "jazz") or year <= 1990 and not artist == "Beatles"
    // Praezedenz: or bindet am schwaechsten → or(inList, and(comparison, not(comparison)))
    Expr expected =
        new Expr.Or(
            new Expr.InList("genre", List.of(new Value.Str("rock"), new Value.Str("jazz"))),
            new Expr.And(
                new Expr.Comparison("year", CompOp.LE, new Value.Num(1990)),
                new Expr.Not(new Expr.Comparison("artist", CompOp.EQ, new Value.Str("Beatles")))));
    assertEquals(
        expected,
        parsePattern(
            "genre in (\"rock\", \"jazz\") or year <= 1990 and not artist == \"Beatles\""));
    assertEquals(
        expected,
        parseVisitor(
            "genre in (\"rock\", \"jazz\") or year <= 1990 and not artist == \"Beatles\""));
  }

  @Test
  void klammerungAendertPraezedenz() {
    // (year <= 1990 or artist == "Beatles") and year > 1960
    Expr expected =
        new Expr.And(
            new Expr.Or(
                new Expr.Comparison("year", CompOp.LE, new Value.Num(1990)),
                new Expr.Comparison("artist", CompOp.EQ, new Value.Str("Beatles"))),
            new Expr.Comparison("year", CompOp.GT, new Value.Num(1960)));
    assertEquals(expected, parsePattern("(year <= 1990 or artist == \"Beatles\") and year > 1960"));
    assertEquals(expected, parseVisitor("(year <= 1990 or artist == \"Beatles\") and year > 1960"));
  }

  @Test
  void dreiAndLinksAssoziativ() {
    // year <= 1990 and artist == "Beatles" and year > 1960
    // → (year <= 1990 and artist == "Beatles") and year > 1960
    Expr expected =
        new Expr.And(
            new Expr.And(
                new Expr.Comparison("year", CompOp.LE, new Value.Num(1990)),
                new Expr.Comparison("artist", CompOp.EQ, new Value.Str("Beatles"))),
            new Expr.Comparison("year", CompOp.GT, new Value.Num(1960)));
    assertEquals(expected, parsePattern("year <= 1990 and artist == \"Beatles\" and year > 1960"));
    assertEquals(expected, parseVisitor("year <= 1990 and artist == \"Beatles\" and year > 1960"));
  }

  @Test
  void dreiOrLinksAssoziativ() {
    Expr expected =
        new Expr.Or(
            new Expr.Or(
                new Expr.Comparison("artist", CompOp.EQ, new Value.Str("Beatles")),
                new Expr.Comparison("artist", CompOp.EQ, new Value.Str("Stones"))),
            new Expr.Comparison("artist", CompOp.EQ, new Value.Str("Zeppelin")));
    assertEquals(
        expected,
        parsePattern("artist == \"Beatles\" or artist == \"Stones\" or artist == \"Zeppelin\""));
    assertEquals(
        expected,
        parseVisitor("artist == \"Beatles\" or artist == \"Stones\" or artist == \"Zeppelin\""));
  }

  // ---- Aufgabe 5: simplify ----

  @Test
  void simplifyDoppeltesNot() {
    // not not x → x
    Expr x = new Expr.Comparison("artist", CompOp.EQ, new Value.Str("Beatles"));
    assertEquals(x, AstBuilders.simplify(new Expr.Not(new Expr.Not(x))));
  }

  @Test
  void simplifyDoppeltesNotViaQuery() {
    // "not not artist == ..." wird direkt durch fromQuery vereinfacht
    Expr expected = new Expr.Comparison("artist", CompOp.EQ, new Value.Str("Beatles"));
    assertEquals(expected, parsePattern("not not artist == \"Beatles\""));
    assertEquals(expected, parseVisitor("not not artist == \"Beatles\""));
  }

  @Test
  void simplifyDreifachesNot() {
    // not(not(not(x))) → not(x)
    Expr x = new Expr.Comparison("year", CompOp.GT, new Value.Num(1980));
    Expr expected = new Expr.Not(x);
    assertEquals(expected, AstBuilders.simplify(new Expr.Not(new Expr.Not(new Expr.Not(x)))));
  }

  @Test
  void simplifyIdempotent() {
    // simplify(simplify(e)) == simplify(e)
    Expr e = parsePattern("not not artist == \"Beatles\" and year <= 1970");
    assertEquals(AstPrinter.toString(e), AstPrinter.toString(AstBuilders.simplify(e)));
  }

  @Test
  void beideBauerLiefernGleichesErgebnis() {
    String[] queries = {
      "artist == \"Beatles\"",
      "year < 1965",
      "artist == \"Beatles\" and year == 1965",
      "artist == \"Beatles\" or year <= 1965",
      "not artist == \"Beatles\"",
      "genre in (\"rock\", \"jazz\")",
      "not not year == 1970",
      "genre in (\"rock\", \"jazz\") or year <= 1990 and not artist == \"Beatles\""
    };
    for (String query : queries) {
      assertEquals(
          AstPrinter.toString(parsePattern(query)),
          AstPrinter.toString(parseVisitor(query)),
          "Builder liefern unterschiedliche Ergebnisse fuer: " + query);
    }
  }
}
