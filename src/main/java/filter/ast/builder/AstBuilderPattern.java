package filter.ast.builder;

import filter.FilterParser;
import filter.ast.nodes.CompOp;
import filter.ast.nodes.Expr;
import filter.ast.nodes.Value;
import java.util.List;

public class AstBuilderPattern {

  // Public entry point
  // query  : expr EOF
  public Expr translate(FilterParser.QueryContext ctx) {
    return buildExpr(ctx.expr());
  }

  // expr: orExpr
  private Expr buildExpr(FilterParser.ExprContext ctx) {
    return buildOrExpr(ctx.orExpr());
  }

  // orExpr : andExpr (OR andExpr)*
  private Expr buildOrExpr(FilterParser.OrExprContext ctx) {
    var children = ctx.andExpr();
    Expr result = buildAndExpr(children.get(0));
    for (int i = 1; i < children.size(); i++) {
      result = new Expr.Or(result, buildAndExpr(children.get(i)));
    }
    return result;
  }

  // andExpr: notExpr (AND notExpr)*
  private Expr buildAndExpr(FilterParser.AndExprContext ctx) {
    var children = ctx.notExpr();
    Expr result = buildNotExpr(children.get(0));
    for (int i = 1; i < children.size(); i++) {
      result = new Expr.And(result, buildNotExpr(children.get(i)));
    }
    return result;
  }

  // notExpr: NOT notExpr | primary
  private Expr buildNotExpr(FilterParser.NotExprContext ctx) {
    if (ctx.NOT() != null) {
      return new Expr.Not(buildNotExpr(ctx.notExpr()));
    }
    return buildPrimary(ctx.primary());
  }

  // primary: comparison | '(' expr ')'
  private Expr buildPrimary(FilterParser.PrimaryContext ctx) {
    if (ctx.comparison() != null) {
      return buildComparison(ctx.comparison());
    }
    return buildExpr(ctx.expr());
  }

  // comparison
  //   : IDENTIFIER op=COMPOP value=literal
  //   | IDENTIFIER IN '(' literalList ')'
  private Expr buildComparison(FilterParser.ComparisonContext ctx) {
    String field = ctx.IDENTIFIER().getText();
    if (ctx.IN() != null) {
      return new Expr.InList(field, buildLiteralList(ctx.literalList()));
    }
    CompOp op = CompOp.fromSymbol(ctx.op.getText());
    Value value = buildLiteral(ctx.value);
    return new Expr.Comparison(field, op, value);
  }

  // literalList: literal (',' literal)*
  private List<Value> buildLiteralList(FilterParser.LiteralListContext ctx) {
    return ctx.literal().stream().map(this::buildLiteral).toList();
  }

  // literal: STRING | NUMBER
  private Value buildLiteral(FilterParser.LiteralContext ctx) {
    if (ctx.STRING() != null) {
      String raw = ctx.STRING().getText();
      return new Value.Str(raw.substring(1, raw.length() - 1)); // Anfuehrungszeichen entfernen
    }
    return new Value.Num(Integer.parseInt(ctx.NUMBER().getText()));
  }
}
