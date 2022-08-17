package norswap.sigh.ast;
import norswap.utils.Util;
import norswap.autumn.positions.Span;

public class ClassFieldAccessNode extends ExpressionNode
{
    public final ExpressionNode stem;
    public final String field;


    public ClassFieldAccessNode (Span span, Object object, Object field) {
        super(span);
        this.stem = Util.cast(object, ExpressionNode.class);
        this.field = Util.cast(field, String.class);
    }


    @Override public String contents ()
    {
        String candidate = String.format("%s$%s", stem.contents(), field);
        return candidate.length() <= contentsBudget()
            ? candidate : "(?)$" + field;
    }
}
