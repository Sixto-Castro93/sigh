package norswap.sigh.ast;

import norswap.autumn.positions.Span;
import norswap.utils.Util;

public final class SetTypeNode extends TypeNode {
    public final TypeNode componentType;

    public SetTypeNode(Span span, Object componentType) {
        super(span);
        this.componentType = Util.cast(componentType, TypeNode.class);
    }

    @Override
    public String contents() {
        return componentType.contents() + "{}";
    }
}
