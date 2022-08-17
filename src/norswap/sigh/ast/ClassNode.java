package norswap.sigh.ast;

import norswap.autumn.positions.Span;
import norswap.utils.Util;
import java.util.List;

public class ClassNode extends DeclarationNode{
    public final String name;
    public final List<String> superclasses;
    public final TypeNode modifier;
    public final BlockNode block;


    @SuppressWarnings("unchecked")
    public ClassNode (Span span, Object modifier, Object name, Object superclasses, Object block) {
        super(span);
        this.superclasses = superclasses == null ? null
            : Util.cast(superclasses, List.class);
        //this.superclasses = Util.cast(superclasses, List.class);
        this.modifier = modifier == null ? new SimpleTypeNode(new Span(span.start, span.start),"pub")
            : Util.cast(modifier, TypeNode.class);
        this.name = Util.cast(name, String.class);
        this.block = Util.cast(block, BlockNode.class);

    }

    @Override public String name () {
        return name;
    }

    @Override public String contents () {
        return "class " + name;
    }

    @Override public String declaredThing () {
        return "class";
    }
}
