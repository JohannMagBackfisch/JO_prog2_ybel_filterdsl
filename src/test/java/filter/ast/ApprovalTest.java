package filter.ast;

import filter.ast.builder.AstBuilderPattern;
import filter.ast.builder.AstBuilderVisitor;
import filter.ast.builder.AstBuilders;
import filter.ast.nodes.Expr;
import filter.ast.printer.AstPrinter;
import org.approvaltests.Approvals;
import org.junit.jupiter.api.Test;

public class ApprovalTest {

  private String printPattern(String query) {
    return AstPrinter.toString(
        AstBuilders.fromQuery(query, ctx -> new AstBuilderPattern().translate(ctx)));
  }

  private String printVisitor(String query) {
    return AstPrinter.toString(
        AstBuilders.fromQuery(query, ctx -> new AstBuilderVisitor().translate(ctx)));
  }

  // ---- Einfache Ausdruecke ----

  @Test
  void einfacherVergleich() {
    Approvals.verify(printPattern("artist == \"Beatles\""));
  }

  @Test
  void zahlVergleich() {
    Approvals.verify(printPattern("year <= 1990"));
  }

  @Test
  void inListAusdruck() {
    Approvals.verify(printPattern("genre in (\"rock\", \"jazz\", \"grunge\")"));
  }

  @Test
  void notAusdruck() {
    Approvals.verify(printPattern("not artist == \"Beatles\""));
  }

  // ---- Zusammengesetzte Ausdruecke ----

  @Test
  void andAusdruck() {
    Approvals.verify(printPattern("artist == \"Beatles\" and year == 1965"));
  }

  @Test
  void orAusdruck() {
    Approvals.verify(printPattern("artist == \"Beatles\" or year <= 1965"));
  }

  @Test
  void komplexerAusdruck() {
    // Zeigt Praezedenz: or < and < not < comparison
    Approvals.verify(
        printPattern(
            "genre in (\"rock\", \"jazz\") or year <= 1990 and not artist == \"Beatles\""));
  }

  @Test
  void klammerung() {
    Approvals.verify(printPattern("(year <= 1990 or artist == \"Beatles\") and year > 1960"));
  }

  @Test
  void dreiAndLinksAssoziativ() {
    Approvals.verify(printPattern("year <= 1990 and artist == \"Beatles\" and year > 1960"));
  }

  // ---- Beide Builder vergleichen ----

  @Test
  void visitorGleichWiePattern() {
    // Beide Builder muessen fuer denselben Query denselben AST-String liefern.
    String query = "genre in (\"rock\", \"jazz\") or year <= 1990 and not artist == \"Beatles\"";
    Approvals.verify("Pattern: " + printPattern(query) + "\nVisitor: " + printVisitor(query));
  }

  // ---- simplify ----

  @Test
  void simplifyDoppeltesNot() {
    // not(not(e)) wird durch fromQuery bereits vereinfacht
    Approvals.verify(printPattern("not not artist == \"Beatles\""));
  }

  @Test
  void simplifyManuell() {
    // Manuell ein doppeltes Not bauen (ohne fromQuery) und dann simplify aufrufen
    Expr inner =
        AstBuilders.fromQuery(
            "not artist == \"Beatles\"", ctx -> new AstBuilderPattern().translate(ctx));
    Expr doubleNot = new Expr.Not(inner); // not(not(comparison))
    Approvals.verify(AstPrinter.toString(AstBuilders.simplify(doubleNot)));
  }
}
