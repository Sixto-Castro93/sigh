package norswap.sigh.types;

public class ModifierType extends Type{
    public static final ModifierType INSTANCE = new ModifierType();
    private ModifierType() {}

    @Override public String name() {
        return "void";
    }
}
