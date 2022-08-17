package norswap.sigh.interpreter;

import norswap.sigh.SemanticAnalysis;
import norswap.sigh.ast.BlockNode;
import norswap.sigh.ast.ClassNode;
import norswap.sigh.ast.DeclarationNode;
import norswap.sigh.ast.SimpleTypeNode;
import norswap.sigh.ast.StatementNode;
import java.util.ArrayList;
import java.util.List;

/**
 * Class representing structure constructors in the interpreter, simply wrapping the declaration
 * node. Such a wrapper is necessary, because the node is already used to represent the structure
 * type.
 */
public final class ClassConstructor
{
    public final ClassNode declaration;

    public ClassConstructor (ClassNode declaration) {
        this.declaration = declaration;
    }

    @Override public int hashCode () {
        return 31 * declaration.hashCode() + 1;
    }

    @Override public boolean equals (Object other) {
        return other instanceof ClassConstructor && ((ClassConstructor) other).declaration == declaration;
    }
}
