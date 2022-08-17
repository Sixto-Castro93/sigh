package norswap.sigh.types;
import norswap.sigh.ast.ClassNode;


public class ClassType extends Type{
    public final ClassNode node;

    public ClassType (ClassNode node) {
        this.node = node;
    }

    @Override public String name() {
        return node.name();
    }

    @Override public boolean equals (Object o) {
        return  o instanceof ClassType && this.node == ((ClassType) o).node || this == o;
    }

    @Override public int hashCode () {
        return node.hashCode();
    }

}
