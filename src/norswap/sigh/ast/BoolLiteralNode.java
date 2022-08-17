package norswap.sigh.ast;

import norswap.autumn.positions.Span;
public class BoolLiteralNode extends ExpressionNode {
    public final boolean value;

    public BoolLiteralNode(Span span, boolean value) {
        super(span);
        this.value = value;
    }

    @Override public String contents() {
        return String.valueOf(value);
    }

}
