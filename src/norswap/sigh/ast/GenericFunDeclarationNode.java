package norswap.sigh.ast;

import norswap.autumn.positions.Span;
import norswap.utils.Util;
import java.util.List;

public class GenericFunDeclarationNode extends DeclarationNode{

    public final TypeNode modifier;
    public final String name;
    public final List<ParameterNode> parameters;
    public final BlockNode block;
    public final TypeNode returnType;

    @SuppressWarnings("unchecked")
    public GenericFunDeclarationNode (Span span, Object modifier, Object name, Object parameters, Object returnType, Object block) {
        super(span);
        this.modifier = modifier == null ? new SimpleTypeNode(new Span(span.start, span.start),"pub")
            : Util.cast(modifier, TypeNode.class);
        this.returnType = returnType == null ? new SimpleTypeNode(new Span(span.start, span.start), "Void")
            : Util.cast(returnType, TypeNode.class);
        this.name = Util.cast(name, String.class);
        this.parameters = Util.cast(parameters, List.class);
        this.block = Util.cast(block, BlockNode.class);


    }

    @Override public String name () {
        return name;
    }

    @Override public String contents () {
        return "name: " + name;
    }

    @Override public String declaredThing () {
        return "function";
    }
}
