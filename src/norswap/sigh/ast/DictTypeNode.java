package norswap.sigh.ast;

import norswap.autumn.positions.Span;
import norswap.utils.Util;

public final class DictTypeNode extends TypeNode {
    public final TypeNode componentType;
    public final String key;
    public final String value;

    public DictTypeNode (Span span, Object componentType, String key, String value) {
        super(span);
        this.componentType = Util.cast(componentType, TypeNode.class);
        this.key = key;
        this.value = value;
    }

    @Override
    public String contents() {
        //return componentType.contents() + "{}";
        return componentType.contents()+"{"+key+":"+value+"}";
    }
}
