package filter.ast.builder;

import filter.FilterBaseVisitor;
import filter.FilterParser;
import filter.ast.nodes.CompOp;
import filter.ast.nodes.Expr;
import filter.ast.nodes.Value;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class AstBuilderVisitor extends FilterBaseVisitor<Void> {

  private final Deque<Expr> exprStack = new ArrayDeque<>();
  private final Deque<Value> valueStack = new ArrayDeque<>();
  private final Deque<List<Value>> listStack = new ArrayDeque<>();

  // Public entry point
  public Expr translate(FilterParser.QueryContext ctx) {
    visit(ctx);
    return exprStack.pop();
  }

  // query  : expr EOF
  @Override
  public Void visitQuery(FilterParser.QueryContext ctx) {
    visit(ctx.expr());
    return null;
  }

  // expr: orExpr
  @Override
  public Void visitExpr(FilterParser.ExprContext ctx) {
    visit(ctx.orExpr());
    return null;
  }

  // orExpr : andExpr (OR andExpr)*
  @Override
  public Void visitOrExpr(FilterParser.OrExprContext ctx) {
    var children = ctx.andExpr();
    for (var child : children) {
      visit(child);
    }
    int n = children.size();
    if (n == 1) return null;
    Deque<Expr> temp = new ArrayDeque<>();
    for (int i = 0; i < n; i++) temp.push(exprStack.pop());
    Expr result = temp.pop();
    while (!temp.isEmpty()) result = new Expr.Or(result, temp.pop());
    exprStack.push(result);
    return null;
  }

  // andExpr: notExpr (AND notExpr)*
  @Override
  public Void visitAndExpr(FilterParser.AndExprContext ctx) {
    var children = ctx.notExpr();
    for (var child : children) {
      visit(child);
    }
    int n = children.size();
    if (n == 1) return null;
    Deque<Expr> temp = new ArrayDeque<>();
    for (int i = 0; i < n; i++) temp.push(exprStack.pop());
    Expr result = temp.pop();
    while (!temp.isEmpty()) result = new Expr.And(result, temp.pop());
    exprStack.push(result);
    return null;
  }

  // notExpr: NOT notExpr | primary
  @Override
  public Void visitNotExpr(FilterParser.NotExprContext ctx) {
    if (ctx.NOT() != null) {
      visit(ctx.notExpr());
      exprStack.push(new Expr.Not(exprStack.pop()));
    } else {
      visit(ctx.primary());
    }
    return null;
  }

  // primary: comparison | '(' expr ')'
  @Override
  public Void visitPrimary(FilterParser.PrimaryContext ctx) {
    if (ctx.comparison() != null) {
      visit(ctx.comparison());
    } else {
      visit(ctx.expr());
    }
    return null;
  }

  // comparison
  //   : IDENTIFIER op=COMPOP value=literal
  //   | IDENTIFIER IN '(' literalList ')'
  @Override
  public Void visitComparison(FilterParser.ComparisonContext ctx) {
    String field = ctx.IDENTIFIER().getText();
    if (ctx.IN() != null) {
      visit(ctx.literalList());
      exprStack.push(new Expr.InList(field, listStack.pop()));
    } else {
      visit(ctx.value);
      CompOp op = CompOp.fromSymbol(ctx.op.getText());
      exprStack.push(new Expr.Comparison(field, op, valueStack.pop()));
    }
    return null;
  }

  // literalList: literal (',' literal)*
  @Override
  public Void visitLiteralList(FilterParser.LiteralListContext ctx) {
    var literals = ctx.literal();
    for (var lit : literals) visit(lit);
    int n = literals.size();
    List<Value> values = new ArrayList<>();
    for (int i = 0; i < n; i++) values.add(0, valueStack.pop());
    listStack.push(values);
    return null;
  }

  // literal: STRING | NUMBER
  @Override
  public Void visitLiteral(FilterParser.LiteralContext ctx) {
    if (ctx.STRING() != null) {
      String raw = ctx.STRING().getText();
      valueStack.push(
          new Value.Str(raw.substring(1, raw.length() - 1))); // Anfuehrungszeichen entfernen
    } else {
      valueStack.push(new Value.Num(Integer.parseInt(ctx.NUMBER().getText())));
    }
    return null;
  }
}
