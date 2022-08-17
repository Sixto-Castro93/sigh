package norswap.sigh.types;

public class SetType extends Type {
    public final Type componentType;

    public SetType(Type componentType) {
        this.componentType = componentType;
    }

    @Override
    public String name() {
        return componentType.toString() + "{}";
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof SetType && componentType.equals(o);
    }

    @Override
    public int hashCode() {
        return componentType.hashCode();
    }

}
