package norswap.sigh.scopes;

import norswap.sigh.ast.RootNode;
import norswap.sigh.types.*;
import norswap.sigh.ast.*;
import norswap.uranium.Reactor;

import static norswap.sigh.scopes.DeclarationKind.*;

/**
 * The lexical scope of a file in Sigh. It is notably responsible for
 * introducing the default
 * declarations made by the language.
 */
public final class RootScope extends Scope {
        // ---------------------------------------------------------------------------------------------

        private SyntheticDeclarationNode decl(String name, DeclarationKind kind) {
                SyntheticDeclarationNode decl = new SyntheticDeclarationNode(name, kind);
                declare(name, decl);
                return decl;
        }

        // ---------------------------------------------------------------------------------------------

        // root scope types
        public final SyntheticDeclarationNode Bool = decl("Bool", TYPE);
        public final SyntheticDeclarationNode Int = decl("Int", TYPE);
        public final SyntheticDeclarationNode Float = decl("Float", TYPE);
        public final SyntheticDeclarationNode String = decl("String", TYPE);
        public final SyntheticDeclarationNode Void = decl("Void", TYPE);
        public final SyntheticDeclarationNode Type = decl("Type", TYPE);

        public final SyntheticDeclarationNode _void   = decl("void", TYPE);
        public final SyntheticDeclarationNode Pub = decl("pub", TYPE);
        public final SyntheticDeclarationNode Pvt = decl("pvt", TYPE);


        // root scope variables
        public final SyntheticDeclarationNode _true = decl("true", VARIABLE);
        public final SyntheticDeclarationNode _false = decl("false", VARIABLE);
        public final SyntheticDeclarationNode _null = decl("null", VARIABLE);

        // root scope functions
        public final SyntheticDeclarationNode print = decl("print", FUNCTION);

        public final SyntheticDeclarationNode addSetInt = decl("addSetInt", FUNCTION);
        public final SyntheticDeclarationNode containsSetInt = decl("containsSetInt", FUNCTION);

        public final SyntheticDeclarationNode addSetFloat = decl("addSetFloat", FUNCTION);
        public final SyntheticDeclarationNode containsSetFloat = decl("containsSetFloat", FUNCTION);

        public final SyntheticDeclarationNode addSetString = decl("addSetString", FUNCTION);
        public final SyntheticDeclarationNode containsSetString = decl("containsSetString", FUNCTION);

        // ---------------------------------------------------------------------------------------------

        public RootScope(RootNode node, Reactor reactor) {
                super(node, null);

                reactor.set(Bool, "type", TypeType.INSTANCE);
                reactor.set(Int, "type", TypeType.INSTANCE);
                reactor.set(Float, "type", TypeType.INSTANCE);
                reactor.set(String, "type", TypeType.INSTANCE);
                reactor.set(Void, "type", TypeType.INSTANCE);
                reactor.set(Type, "type", TypeType.INSTANCE);

                reactor.set(_void,   "type",    TypeType.INSTANCE);
                reactor.set(Pub, "type",       TypeType.INSTANCE);
                reactor.set(Pvt,  "type",      TypeType.INSTANCE);


                reactor.set(Bool, "declared", BoolType.INSTANCE);
                reactor.set(Int, "declared", IntType.INSTANCE);
                reactor.set(Float, "declared", FloatType.INSTANCE);
                reactor.set(String, "declared", StringType.INSTANCE);
                reactor.set(Void, "declared", VoidType.INSTANCE);
                reactor.set(Type, "declared", TypeType.INSTANCE);

                reactor.set(_void,   "declared",    VoidType.INSTANCE);
                reactor.set(Pub, "declared",       ModifierType.INSTANCE);
                reactor.set(Pvt,  "declared",       ModifierType.INSTANCE);

                reactor.set(_true, "type", BoolType.INSTANCE);
                reactor.set(_false, "type", BoolType.INSTANCE);
                reactor.set(_null, "type", NullType.INSTANCE);

                reactor.set(print, "type", new FunType(StringType.INSTANCE, StringType.INSTANCE));

                reactor.set(addSetInt, "type", new FunType(new SetType(IntType.INSTANCE),
                    new SetType(IntType.INSTANCE), IntType.INSTANCE));
                reactor.set(containsSetInt, "type", new FunType(BoolType.INSTANCE,
                    new SetType(IntType.INSTANCE), IntType.INSTANCE));

                reactor.set(addSetFloat, "type", new FunType(new SetType(FloatType.INSTANCE),
                    new SetType(FloatType.INSTANCE), FloatType.INSTANCE));
                reactor.set(containsSetFloat, "type", new FunType(BoolType.INSTANCE,
                    new SetType(FloatType.INSTANCE), FloatType.INSTANCE));

                reactor.set(addSetString, "type", new FunType(new SetType(StringType.INSTANCE),
                    new SetType(StringType.INSTANCE), StringType.INSTANCE));
                reactor.set(containsSetString, "type", new FunType(BoolType.INSTANCE,
                    new SetType(StringType.INSTANCE), StringType.INSTANCE));

                // We tried instanciating only one function, but we couldn't do it. We created
                // many functions according to the set type.

        }

        // ---------------------------------------------------------------------------------------------
}
