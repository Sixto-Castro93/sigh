package norswap.sigh.ast;

import norswap.autumn.positions.Span;
import norswap.utils.Util;
import java.util.List;

public class TempDeclarationNode extends DeclarationNode {
    public final String name;
    private final List<TempTypeNode> temp_types;
    private final List<ParameterNode> parameters;
    private final TypeNode returnType;
    private final BlockNode block;

    @SuppressWarnings("unchecked")
    public TempDeclarationNode(Span span, Object temp_types, Object name, Object parameters, Object returnType,
            Object block) {
        super(span);
        this.temp_types = Util.cast(temp_types, List.class);
        this.name = Util.cast(name, String.class);
        this.parameters = Util.cast(parameters, List.class);
        this.returnType = returnType == null
                ? new SimpleTypeNode(new Span(span.start, span.start), "Void")
                : Util.cast(returnType, TypeNode.class);
        this.block = Util.cast(block, BlockNode.class);
    }

    @Override
    public String name() {
        return name; // TODO: Adjust this
    }

    @Override
    public String contents() {
        return "template " + name; // TODO: Adjust this
    }

    @Override
    public String declaredThing() {
        return "template";
    }
}
