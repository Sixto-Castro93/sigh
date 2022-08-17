package norswap.sigh.ast;

import norswap.autumn.positions.Span;
import norswap.utils.Util;

public class ModifierNode extends TypeNode{
    public final String name;

    public ModifierNode (Span span, Object name) {
        super(span);
        this.name = Util.cast(name, String.class);
    }

    public String toString(){return name;}

    @Override public String contents () {
        return name;
    }
}
