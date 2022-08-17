package norswap.sigh.types;

import norswap.utils.NArrays;
import java.util.Arrays;

public final class TempType extends Type {

    public final Type[] passedTpsTypes;
    public final Type returnType;
    public final Type[] paramsTypes;
    public final int typesQtt;

    public TempType(Type returnType, int typesQtt, Type... passedTpsAndParamTypes) {
        this.returnType = returnType;
        this.typesQtt = typesQtt;
        this.passedTpsTypes = new Type[typesQtt];
        this.paramsTypes = new Type[passedTpsAndParamTypes.length - typesQtt];

        for (int i = 0; i < typesQtt; ++i) {
            System.out.print(passedTpsAndParamTypes[i]);
            this.passedTpsTypes[i] = passedTpsAndParamTypes[i];
        }

        for (int i = 0; i < (passedTpsAndParamTypes.length - typesQtt); ++i) {
            this.paramsTypes[i] = passedTpsAndParamTypes[i + typesQtt];
        }
    }

    @Override
    public String name() {
        String[] passedTps = NArrays.map(passedTpsTypes, new String[0], Type::name);
        return String.format("(%s)", String.join(",", passedTps));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TempType))
            return false;
        TempType other = (TempType) o;

        return Arrays.equals(passedTpsTypes, other.passedTpsTypes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(passedTpsTypes);
    }
}
