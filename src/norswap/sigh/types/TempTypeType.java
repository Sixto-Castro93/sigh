package norswap.sigh.types;

import norswap.sigh.ast.StructDeclarationNode;
import norswap.sigh.ast.TempTypeNode;

public final class TempTypeType extends Type {
    public final TempTypeNode node;

    public TempTypeType(TempTypeNode node) {
        this.node = node;
    }

    @Override
    public String name() {
        return node.name();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof TempTypeType && this.node == ((TempTypeType) o).node;
    }

    @Override
    public int hashCode() {
        return node.hashCode();
    }
}
