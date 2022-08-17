package norswap.sigh.ast;

import norswap.autumn.positions.Span;
import norswap.utils.Util;
import java.util.List;

public class TempCallNode extends ExpressionNode {

    public final ExpressionNode template;
    public final List<ExpressionNode> arguments;
    public final List<TypeNode> types;

    @SuppressWarnings("unchecked")
    public TempCallNode(Span span, Object template, Object types, Object arguments) {
        super(span);
        this.template = Util.cast(template, ExpressionNode.class);
        this.types = Util.cast(types, List.class);
        this.arguments = Util.cast(arguments, List.class);
    }

    @Override
    public String contents() {
        String args = arguments.size() == 0 ? "()" : "(...)";
        return template.contents() + args;
    }
}
