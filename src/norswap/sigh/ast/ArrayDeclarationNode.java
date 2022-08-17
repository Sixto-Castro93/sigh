package norswap.sigh.ast;

import norswap.autumn.positions.Span;
import norswap.utils.Util;
import java.util.List;

public class ArrayDeclarationNode extends DeclarationNode{
    public final String name;
    public final TypeNode type;
    public final List<ExpressionNode> components;

    @SuppressWarnings("unchecked")
    public ArrayDeclarationNode (Span span, Object type, Object name, Object components) {
        super(span);
        this.type = Util.cast(type, TypeNode.class);
        this.name = Util.cast(name, String.class);
        this.components = Util.cast(components, List.class);
    }

    public void showMeContent(){
        System.out.println("\n");
        System.out.println("type: "+type);
        System.out.println("name: "+name);
        System.out.println("value: "+components);
        System.out.println("\n");
    }

    @Override public String name () {
        return name;
    }

    @Override public String contents () {
        return type.toString() + name;
    }

    @Override public String declaredThing () {
        return "variable";
    }
}
