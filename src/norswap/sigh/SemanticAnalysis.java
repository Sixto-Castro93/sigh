package norswap.sigh;

import norswap.sigh.ast.*;
import norswap.sigh.scopes.DeclarationContext;
import norswap.sigh.scopes.DeclarationKind;
import norswap.sigh.scopes.RootScope;
import norswap.sigh.scopes.Scope;
import norswap.sigh.scopes.SyntheticDeclarationNode;
import norswap.sigh.types.*;
import norswap.uranium.Attribute;
import norswap.uranium.Reactor;
import norswap.uranium.Rule;
import norswap.utils.visitors.ReflectiveFieldWalker;
import norswap.utils.visitors.Walker;
import java.util.*;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static norswap.sigh.ast.BinaryOperator.*;
import static norswap.utils.Util.cast;
import static norswap.utils.Vanilla.forEachIndexed;
import static norswap.utils.Vanilla.list;
import static norswap.utils.visitors.WalkVisitType.POST_VISIT;
import static norswap.utils.visitors.WalkVisitType.PRE_VISIT;

/**
 * Holds the logic implementing semantic analyzis for the language, including
 * typing and name
 * resolution.
 *
 * <p>
 * The entry point into this class is {@link #createWalker(Reactor)}.
 *
 * <h2>Big Principles
 * <ul>
 * <li>Every {@link DeclarationNode} instance must have its {@code type}
 * attribute to an
 * instance of {@link Type} which is the type of the value declared (note that
 * for struct
 * declaration, this is always {@link TypeType}.</li>
 *
 * <li>Additionally, {@link StructDeclarationNode} (and default
 * {@link SyntheticDeclarationNode} for types) must have their {@code declared}
 * attribute set to
 * an instance of the type being declared.</li>
 *
 * <li>Every {@link ExpressionNode} instance must have its {@code type}
 * attribute similarly
 * set.</li>
 *
 * <li>Every {@link ReferenceNode} instance must have its {@code decl} attribute
 * set to the the
 * declaration it references and its {@code scope} attribute set to the
 * {@link Scope} in which
 * the declaration it references lives. This speeds up lookups in the
 * interpreter and simplifies the compiler.</li>
 * // reference: when you call a variable or function (or template in our case
 * as well)
 *
 *
 * <li>For the same reasons, {@link VarDeclarationNode} and
 * {@link ParameterNode} should have
 * their {@code scope} attribute set to the scope in which they appear (this
 * also speeds up the
 * interpreter).</li>
 *
 * <li>All statements introducing a new scope must have their {@code scope}
 * attribute set to the
 * corresponding {@link Scope} (only {@link RootNode}, {@link BlockNode} and
 * {@link
 * FunDeclarationNode} (for parameters)). These nodes must also update the
 * {@code scope}
 * field to track the current scope during the walk.</li>
 * // {@link GenericFunDeclarationNode} will do it as well (for parameters),
 * maybe the template declaration will do it too, but we'll see.
 *
 * <li>Every {@link TypeNode} instance must have its {@code value} set to the
 * {@link Type} it
 * denotes.</li>
 *
 * <li>Every {@link ReturnNode}, {@link BlockNode} and {@link IfNode} must have
 * its {@code
 *     returns} attribute set to a boolean to indicate whether its execution
 * causes
 * unconditional exit from the surrounding function or main script.</li>
 *
 * <li>The rules check typing constraints: assignment of values to variables, of
 * arguments to
 * parameters, checking that if/while conditions are booleans, and array indices
 * are
 * integers.</li>
 *
 * <li>The rules also check a number of other constraints: that accessed struct
 * fields exist,
 * that variables are declared before being used, etc...</li>
 * </ul>
 */
public final class SemanticAnalysis {
    // =============================================================================================
    // region [Initialization]
    // =============================================================================================

    private final Reactor R;

    /** Current scope. */
    private Scope scope;

    /**
     * Current context for type inference (currently only to infer the type of empty
     * arrays).
     */
    private SighNode inferenceContext;

    /** Index of the current function argument. */
    private int argumentIndex;
    public static HashMap<String,FunDeclarationNode> functionsDecl;
    // ---------------------------------------------------------------------------------------------

    private SemanticAnalysis(Reactor reactor) {
        this.R = reactor;
        functionsDecl=new HashMap<>();
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Call this method to create a tree walker that will instantiate the typing
     * rules defined
     * in this class when used on an AST, using the given {@code reactor}.
     */
    public static Walker<SighNode> createWalker(Reactor reactor) {
        ReflectiveFieldWalker<SighNode> walker = new ReflectiveFieldWalker<>(
                SighNode.class, PRE_VISIT, POST_VISIT);

        SemanticAnalysis analysis = new SemanticAnalysis(reactor);

        // expressions
        walker.register(IntLiteralNode.class, PRE_VISIT, analysis::intLiteral);
        walker.register(FloatLiteralNode.class, PRE_VISIT, analysis::floatLiteral);
        walker.register(StringLiteralNode.class, PRE_VISIT, analysis::stringLiteral);
        walker.register(ReferenceNode.class, PRE_VISIT, analysis::reference);
        walker.register(ConstructorNode.class, PRE_VISIT, analysis::constructor);
        walker.register(ClassConstructorNode.class,  PRE_VISIT,  analysis::classConstructor);
        walker.register(ArrayLiteralNode.class, PRE_VISIT, analysis::arrayLiteral);
        walker.register(SetLiteralNode.class, PRE_VISIT, analysis::setLiteral);
        walker.register(ParenthesizedNode.class, PRE_VISIT, analysis::parenthesized);
        walker.register(FieldAccessNode.class, PRE_VISIT, analysis::fieldAccess);
        walker.register(ArrayAccessNode.class, PRE_VISIT, analysis::arrayAccess);
        walker.register(FunCallNode.class, PRE_VISIT, analysis::funCall);
        walker.register(UnaryExpressionNode.class, PRE_VISIT, analysis::unaryExpression);
        walker.register(BinaryExpressionNode.class, PRE_VISIT, analysis::binaryExpression);
        walker.register(AssignmentNode.class, PRE_VISIT, analysis::assignment);
        walker.register(TempCallNode.class, PRE_VISIT, analysis::templateCall);

        // types
        walker.register(SimpleTypeNode.class, PRE_VISIT, analysis::simpleType);
        walker.register(ArrayTypeNode.class, PRE_VISIT, analysis::arrayType);
        walker.register(SetTypeNode.class, PRE_VISIT, analysis::setType);
        walker.register(TempTypeNode.class, PRE_VISIT, analysis::templateDeclTypes);

        // declarations & scopes
        walker.register(RootNode.class, PRE_VISIT, analysis::root);
        walker.register(BlockNode.class, PRE_VISIT, analysis::block);
        walker.register(VarDeclarationNode.class, PRE_VISIT, analysis::varDecl);
        walker.register(FieldDeclarationNode.class, PRE_VISIT, analysis::fieldDecl);
        walker.register(ParameterNode.class, PRE_VISIT, analysis::parameter);
        walker.register(FunDeclarationNode.class, PRE_VISIT, analysis::funDecl);
        walker.register(StructDeclarationNode.class, PRE_VISIT, analysis::structDecl);
        walker.register(TempDeclarationNode.class, PRE_VISIT, analysis::templateDecl);
        walker.register(ModifierNode.class, PRE_VISIT, analysis::modifierType);
        walker.register(ClassNode.class, PRE_VISIT, analysis::classGenericDecl);
        walker.register(GenericFunDeclarationNode.class, PRE_VISIT, analysis::funGenericDecl);
        walker.register(ClassFieldAccessNode.class,     PRE_VISIT,  analysis::classElementAccess);

        walker.register(RootNode.class, POST_VISIT, analysis::popScope);
        walker.register(BlockNode.class, POST_VISIT, analysis::popScope);
        walker.register(FunDeclarationNode.class, POST_VISIT, analysis::popScope);
        walker.register(GenericFunDeclarationNode.class, POST_VISIT, analysis::popScope);
        walker.register(ClassNode.class, POST_VISIT, analysis::popScope);

        walker.register(ExpressionStatementNode.class, PRE_VISIT, node -> {
        });
        walker.register(IfNode.class, PRE_VISIT, analysis::ifStmt);
        walker.register(WhileNode.class, PRE_VISIT, analysis::whileStmt);
        walker.register(ReturnNode.class, PRE_VISIT, analysis::returnStmt);

        walker.registerFallback(POST_VISIT, node -> {
        });

        return walker;
    }

    // endregion
    // =============================================================================================
    // region [Expressions]
    // =============================================================================================

    private void intLiteral(IntLiteralNode node) {
        R.set(node, "type", IntType.INSTANCE); // We already know the type, so we can just say it
    } // We set the "type" attribute of the node to IntType

    // ---------------------------------------------------------------------------------------------

    private void floatLiteral(FloatLiteralNode node) {
        R.set(node, "type", FloatType.INSTANCE);
    }

    // ---------------------------------------------------------------------------------------------

    private void stringLiteral(StringLiteralNode node) {
        R.set(node, "type", StringType.INSTANCE);
    }

    // ---------------------------------------------------------------------------------------------

    private void reference(ReferenceNode node) {
        final Scope scope = this.scope; // Capture the current scope

        // Try to lookup immediately. This must succeed for variables, but not
        // necessarily for
        // functions or types. By looking up now, we can report looked up variables
        // later as being used before being defined.

        // Variables must be declared before being used, if the var was declared before
        // used, then it was already registered in the scope
        DeclarationContext maybeCtx = scope.lookup(node.name);

        // maybeCtx because if the var was declared it will find, but if not, then it
        // will be null

        if (maybeCtx != null) {
            R.set(node, "decl", maybeCtx.declaration);
            R.set(node, "scope", maybeCtx.scope); // Set those values

            R.rule(node, "type")
                    .using(maybeCtx.declaration, "type")
                    .by(Rule::copyFirst); // Then it will get the type from the declaration and copy it
            return;
        }

        // Re-lookup after the scopes have been built.
        R.rule(node.attr("decl"), node.attr("scope"))
                .by(r -> {
                    DeclarationContext ctx = scope.lookup(node.name);
                    DeclarationNode decl = ctx == null ? null : ctx.declaration;

                    if (ctx == null) {
                        //System.out.println("Ctx null");
                        r.errorFor("Could not resolve: " + node.name,
                                node, node.attr("decl"), node.attr("scope"), node.attr("type")); // If it didn't find it
                                                                                                 // at all, the variable
                                                                                                 // doesn't exist
                    } else {
                        r.set(node, "scope", ctx.scope);
                        r.set(node, "decl", decl);

                        if (decl instanceof VarDeclarationNode)
                            r.errorFor("Variable used before declaration: " + node.name, // If it found it, the var was
                                                                                         // declared after being used
                                    node, node.attr("type"));
                        else
                            R.rule(node, "type")
                                    .using(decl, "type")
                                    .by(Rule::copyFirst); //
                    }
                });
    }

    // ---------------------------------------------------------------------------------------------

    private void constructor(ConstructorNode node) {
        R.rule()
                .using(node.ref, "decl")
                .by(r -> {
                    DeclarationNode decl = r.get(0);

                    if (!(decl instanceof StructDeclarationNode)) {
                        String description = "Applying the constructor operator ($) to non-struct reference for: "
                                + decl;
                        r.errorFor(description, node, node.attr("type"));
                        return;
                    }

                    StructDeclarationNode structDecl = (StructDeclarationNode) decl;

                    Attribute[] dependencies = new Attribute[structDecl.fields.size() + 1];
                    dependencies[0] = decl.attr("declared");
                    forEachIndexed(structDecl.fields, (i, field) -> dependencies[i + 1] = field.attr("type"));

                    R.rule(node, "type")
                            .using(dependencies)
                            .by(rr -> {
                                Type structType = rr.get(0);
                                Type[] params = IntStream.range(1, dependencies.length).<Type>mapToObj(rr::get)
                                        .toArray(Type[]::new);
                                rr.set(0, new FunType(structType, params));
                            });
                });
    }



    private void classConstructor (ClassConstructorNode node)
    {
        R.rule()
            .using(node.ref, "decl")
            .by(r -> {
                DeclarationNode decl = r.get(0);

                if (!(decl instanceof ClassNode)) {
                    String description =
                        "Applying the class constructor (create) to non-class reference for: "
                            + decl;
                    r.errorFor(description, node, node.attr("type"));
                    return;
                }


                ClassNode classDecl = (ClassNode) decl;
                int nAttr = classDecl.block.statements.size();
                List<StatementNode> result= new ArrayList<>();
                int new_size=nAttr;

                Attribute[] dependencies = new Attribute[new_size + 1];
                dependencies[0] = decl.attr("declared");

                forEachIndexed(classDecl.block.statements, (i, attribute) ->
                    dependencies[i + 1] = attribute.attr("type"));

                R.rule(node, "type")
                    .using(dependencies)
                    .by(rr -> {
                        Type classType = rr.get(0);
                        rr.set(0, new FunType(classType));
                    });
            });

    }
    // ---------------------------------------------------------------------------------------------

    private void arrayLiteral(ArrayLiteralNode node) {
        if (node.components.size() == 0) { // []
            // Empty array: we need a type int to know the desired type.

            final SighNode context = this.inferenceContext;

            if (context instanceof VarDeclarationNode)
                R.rule(node, "type")
                        .using(context, "type")
                        .by(Rule::copyFirst);
            else if (context instanceof FunCallNode) {
                R.rule(node, "type")
                        .using(((FunCallNode) context).function.attr("type"), node.attr("index"))
                        .by(r -> {
                            FunType funType = r.get(0);
                            r.set(0, funType.paramTypes[(int) r.get(1)]);
                        });
            }
            return;
        }

        Attribute[] dependencies = node.components.stream().map(it -> it.attr("type")).toArray(Attribute[]::new);

        R.rule(node, "type")
                .using(dependencies)
                .by(r -> {
                    Type[] types = IntStream.range(0, dependencies.length).<Type>mapToObj(r::get)
                            .distinct().toArray(Type[]::new);

                    int i = 0;
                    Type supertype = null;
                    for (Type type : types) {
                        if (type instanceof VoidType)
                            // We report the error, but compute a type for the array from the other
                            // elements.
                            r.errorFor("Void-valued expression in array literal", node.components.get(i));
                        else if (supertype == null)
                            supertype = type;
                        else {
                            supertype = commonSupertype(supertype, type);
                            if (supertype == null) {
                                r.error("Could not find common supertype in array literal.", node);
                                return;
                            }
                        }
                        ++i;
                    }

                    if (supertype == null)
                        r.error(
                                "Could not find common supertype in array literal: all members have Void type.",
                                node);
                    else
                        r.set(0, new ArrayType(supertype));
                });
    }

    // ---------------------------------------------------------------------------------------------

    private void setLiteral(SetLiteralNode node) {
        if (node.components.size() == 0) { // {}
            // Empty set: we need a type int to know the desired type.

            final SighNode context = this.inferenceContext;

            if (context instanceof VarDeclarationNode)
                R.rule(node, "type")
                        .using(context, "type")
                        .by(Rule::copyFirst);
            else if (context instanceof FunCallNode) {
                R.rule(node, "type")
                        .using(((FunCallNode) context).function.attr("type"), node.attr("index"))
                        .by(r -> {
                            FunType funType = r.get(0);
                            r.set(0, funType.paramTypes[(int) r.get(1)]);
                        });
            }
            return;
        }

        Attribute[] dependencies = node.components.stream().map(it -> it.attr("type")).toArray(Attribute[]::new);

        R.rule(node, "type")
                .using(dependencies)
                .by(r -> {
                    Type[] types = IntStream.range(0, dependencies.length).<Type>mapToObj(r::get)
                            .distinct().toArray(Type[]::new);

                    int i = 0;
                    Type supertype = null;
                    for (Type type : types) {
                        if (type instanceof VoidType)
                            // We report the error, but compute a type for the array from the other
                            // elements.
                            r.errorFor("Void-valued expression in set literal", node.components.get(i));
                        else if (supertype == null)
                            supertype = type;
                        else {
                            supertype = commonSupertype(supertype, type);
                            if (supertype == null) {
                                r.error("Could not find common supertype in set literal.", node);
                                return;
                            }
                        }
                        ++i;
                    }

                    if (supertype == null)
                        r.error(
                                "Could not find common supertype in set literal: all members have Void type.",
                                node);
                    else
                        r.set(0, new SetType(supertype));
                });
    }

    // ---------------------------------------------------------------------------------------------

    private void parenthesized(ParenthesizedNode node) {
        R.rule(node, "type")
                .using(node.expression, "type") // the type of parenthesized expression is the same type of the
                                                // expression inside it
                .by(Rule::copyFirst);
    }

    // ---------------------------------------------------------------------------------------------

    private void fieldAccess(FieldAccessNode node) {
        R.rule()
                .using(node.stem, "type")
                .by(r -> {
                    Type type = r.get(0);

                    if (type instanceof ArrayType) {
                        if (node.fieldName.equals("length"))
                            R.rule(node, "type")
                                    .by(rr -> rr.set(0, IntType.INSTANCE));
                        else
                            r.errorFor("Trying to access a non-length field on an array", node,
                                    node.attr("type"));
                        return;
                    }

                    if (!(type instanceof StructType)) {
                        r.errorFor("Trying to access a field on an expression of type " + type,
                                node,
                                node.attr("type"));
                        return;
                    }

                    StructDeclarationNode decl = ((StructType) type).node;

                    for (DeclarationNode field : decl.fields) {
                        if (!field.name().equals(node.fieldName))
                            continue;

                        R.rule(node, "type")
                                .using(field, "type")
                                .by(Rule::copyFirst);

                        return;
                    }

                    String description = format("Trying to access missing field %s on struct %s",
                            node.fieldName, decl.name);
                    r.errorFor(description, node, node.attr("type"));
                });
    }


    /*
    The classElementAccess method performs some validations such as verifying when trying to access an attribute or function of it or not.
    and creates/defines some rules for each statement type: fieldDecl, varDecl, funDecl, genfunDecl
    */
    private void classElementAccess (ClassFieldAccessNode node)
    {
        R.rule()
            .using(node.stem, "type")
            .by(r -> {
                Type type = r.get(0);

                if (type instanceof ArrayType) {
                    if (node.field.equals("length"))
                        R.rule(node, "type")
                            .by(rr -> rr.set(0, IntType.INSTANCE));
                    else
                        r.errorFor("Trying to access a non-length attribute on an array", node,
                            node.attr("type"));
                    return;
                }


                if (!(type instanceof ClassType)) {
                    r.errorFor("Trying to access a class element on an expression of type " + type,
                        node,
                        node.attr("type"));
                    return;
                }

                ClassNode decl = ((ClassType) type).node;
                int nAttr = decl.block.statements.size();
                int new_size=nAttr;


                for(int i=0; i<new_size; i++){
                    StatementNode item= decl.block.statements.get(i);
                    if(item instanceof FieldDeclarationNode){
                        if (!((FieldDeclarationNode) item).name.equals(node.field)) continue;
                        R.rule(node, "type")
                            .using(item, "type")
                            .by(Rule::copyFirst);
                        return;
                    }
                    if(item instanceof VarDeclarationNode){
                        if (!((VarDeclarationNode) item).name.equals(node.field)) continue;
                        R.rule(node, "type")
                            .using(item, "type")
                            .by(Rule::copyFirst);
                        return;
                    }
                    if(item instanceof FunDeclarationNode){
                        if (!((FunDeclarationNode) item).name.equals(node.field)) continue;
                        R.rule(node, "type")
                            .using(item, "type")
                            .by(Rule::copyFirst);
                        return;
                    }
                    if(item instanceof GenericFunDeclarationNode){
                        if (!((GenericFunDeclarationNode) item).name.equals(node.field)) continue;
                        R.rule(node, "type")
                            .using(item, "type")
                            .by(Rule::copyFirst);
                        return;
                    }
                }
                String description = format("Trying to access missing field %s on class %s",
                    node.field, decl.name);
                r.errorFor(description, node, node.attr("type"));

            });


    }


    // ---------------------------------------------------------------------------------------------

    private void arrayAccess(ArrayAccessNode node) {
        R.rule()
                .using(node.index, "type")
                .by(r -> {
                    Type type = r.get(0); // gets the dependency 0 (index). there's only 1 dependency in this case
                    if (!(type instanceof IntType))
                        r.error("Indexing an array using a non-Int-valued expression", node.index); // First: need to
                                                                                                    // check that type
                                                                                                    // of index is an
                                                                                                    // integer
                });

        R.rule(node, "type") // Second: need to check that we're trying to access an index in an *ARRAY*, so
                             // we check that it's an array
                .using(node.array, "type")
                .by(r -> {
                    Type type = r.get(0);
                    if (type instanceof ArrayType)
                        r.set(0, ((ArrayType) type).componentType);
                    else
                        r.error("Trying to index a non-array expression of type " + type, node);
                });
    }

    // ---------------------------------------------------------------------------------------------
    private void modifierType (ModifierNode node){
        R.rule(node,"value")
            .using(node.name,"value")
            .by(r -> r.set(0, ModifierType.INSTANCE));
    }


    /*
    This method is for declaring a new class in the scope.
    It also takes into account the case of Superclasses so that methods, variables (Statements)
    from a father class, could be accessed by this new class (child).
    This child class can also inherit from a set of parent classes.
    */
    private void classGenericDecl(ClassNode node){
        scope.declare(node.name, node);
        ClassNode cNode;
        cNode=node;

        if(node.superclasses != null){
            List<StatementNode> result= new ArrayList<>();
            result.addAll(node.block.statements);

            for(String superclass : node.superclasses) {
                DeclarationContext ctx = scope.lookup(superclass);
                if(ctx!=null) {
                    DeclarationNode ctx2 = ctx.declaration;
                    result.addAll(((ClassNode)ctx2).block.statements);
                }

            }

            ClassNode new_classNode = new ClassNode(null, new SimpleTypeNode(null, "pub"),
                node.name, node.superclasses,
                new BlockNode(null, result));

            cNode=new_classNode;


        }


        scope.declare(cNode.name, cNode);
        R.set(cNode, "type", TypeType.INSTANCE);
        R.set(cNode, "declared", new ClassType(cNode));

        scope = new Scope(cNode, scope);
        R.set(cNode, "scope", scope);


    }


    private void funGenericDecl (GenericFunDeclarationNode node)
    {
        scope.declare(node.name, node);
        scope = new Scope(node, scope);
        R.set(node, "scope", scope);

        Attribute[] dependencies = new Attribute[node.parameters.size() + 1];
        dependencies[0] = node.returnType.attr("value");
        forEachIndexed(node.parameters, (i, param) ->
            dependencies[i + 1] = param.attr("type"));


        R.rule(node,"value")
            .using(node.modifier,"value")
            .by(r -> r.set(0, ModifierType.INSTANCE));

        R.rule(node, "type")
            .using(dependencies)
            .by (r -> {
                Type[] paramTypes = new Type[node.parameters.size()];
                for (int i = 0; i < paramTypes.length; ++i)
                    paramTypes[i] = r.get(i + 1);
                r.set(0, new FunType(r.get(0), paramTypes));
            });

        R.rule()
            .using(node.block.attr("returns"), node.returnType.attr("value"))
            .by(r -> {
                boolean returns = r.get(0);
                Type returnType = r.get(1);
                if (!returns && !(returnType instanceof VoidType))
                    r.error("Missing return in function.", node);
                // NOTE: The returned value presence & type is checked in returnStmt().
            });

    }


    /*
    This method validates important aspects when calling a function such as: types of arguments, compatibility, number of args, etc.
    The case of Polymorphism (a function with same name, but with more/less arguments) is validated here, for instance,
    when number of arguments don't match, it consults that function in a dictionary (functionsDecl) and get the node if it exists.
    As said before, in that dictionary, the keys are made this way: functionName [Int, Int, Int] and the value is a FunDeclarationNode.
    Then, when the correct functionDeclNode is gotten, the validations are applied on this one.
    Finally, it was also added a 2nd case of Polymorphism when having same name function, but each one with diff types for ex:
    add [Int, Int], add[Float, Float], add[String, String]
    */
    private void funCall(FunCallNode node) {
        this.inferenceContext = node;
        String nameFun =node.function.contents();
        nameFun = nameFun.substring(nameFun.indexOf("$") + 1);

        Attribute[] dependencies = new Attribute[node.arguments.size() + 1];
        dependencies[0] = node.function.attr("type");

        forEachIndexed(node.arguments, (i, arg) -> {
            dependencies[i + 1] = arg.attr("type"); // get all the attributess
            R.set(arg, "index", i);
        });


        String finalNameFun = nameFun;
        R.rule(node, "type")
                .using(dependencies) // First: compute dependencies
                .by(r -> {
                    Type maybeFunType = r.get(0);

                    if (!(maybeFunType instanceof FunType)) {
                        r.error("trying to call a non-function expression: " + node.function, node.function);
                        return;
                    }

                    FunType funType = cast(maybeFunType);
                    r.set(0, funType.returnType);

                    Type[] params = funType.paramTypes;
                    List<ExpressionNode> args = node.arguments;
                    FunDeclarationNode fc;

                    String concatParams= finalNameFun +" [";
                    for(ExpressionNode item: node.arguments)
                        concatParams = concatParams+item.attr("type").node.toString().split("Literal")[0]+", ";
                    concatParams=concatParams+"]";
                    concatParams=concatParams.replace(", ]","]");
                    //System.out.println("concatParams: "+concatParams);


                    //If params size is not equals to args size, it will check the case of Polymorphism (more or less args), or the 2nd case of Polymorp (diff args type including same size for both nodes)
                    if ((params.length != args.size()) || SemanticAnalysis.functionsDecl.containsKey(concatParams)) {
                        fc = SemanticAnalysis.functionsDecl.get(concatParams);

                        if(fc!=null) { //If a funcionDeclNode exists with these parameters/args, it will check that this funDeclnode and FunCallnode match in #args and check their types; otherwise it will throw the respective error.
                            if (fc.parameters.size() != node.arguments.size())
                                r.errorFor(format("wrong number of arguments, expected %d but got %d",
                                        params.length, args.size()),
                                    node);

                            int checkedArgs = Math.min(params.length, args.size());

                            for (int i = 0; i < fc.parameters.size(); i++) {
                                String argType = fc.parameters.get(i).type.contents();
                                String paramType = node.arguments.get(i).toString().split("Literal")[0];
                                if (!argType.equals(paramType))
                                    r.errorFor(format(
                                            "incompatible argument provided for argument %d: expected %s but got %s",
                                            i, paramType, argType),
                                        node.arguments.get(i));
                            }
                        }else
                            r.errorFor(format("wrong number of arguments/wrong args type, expected %d but got %d",
                                    params.length, args.size()),
                                node);

                    }else{//If params and args sizes match
                        int checkedArgs = Math.min(params.length, args.size());
                        for (int i = 0; i < checkedArgs; ++i) {
                            Type argType = r.get(i + 1);
                            Type paramType = funType.paramTypes[i];
                            if (!isAssignableTo(argType, paramType))
                                r.errorFor(format(
                                        "incompatible argument provided for argument %d: expected %s but got %s",
                                        i, paramType, argType),
                                        node.arguments.get(i));
                        }
                    }
                });
    }

    // ---------------------------------------------------------------------------------------------

    private void unaryExpression(UnaryExpressionNode node) {
        assert node.operator == UnaryOperator.NOT; // only one for now
        R.set(node, "type", BoolType.INSTANCE);

        R.rule()
                .using(node.operand, "type")
                .by(r -> {
                    Type opType = r.get(0);
                    if (!(opType instanceof BoolType))
                        r.error("Trying to negate type: " + opType, node);
                });
    }

    // endregion
    // =============================================================================================
    // region [Binary Expressions]
    // =============================================================================================

    private void binaryExpression(BinaryExpressionNode node) {
        R.rule(node, "type")
                .using(node.left.attr("type"), node.right.attr("type"))
                .by(r -> {
                    Type left = r.get(0);
                    Type right = r.get(1);

                    if (node.operator == ADD && (left instanceof StringType || right instanceof StringType))
                        r.set(0, StringType.INSTANCE);
                    else if (isArithmetic(node.operator))
                        binaryArithmetic(r, node, left, right);
                    else if (isComparison(node.operator))
                        binaryComparison(r, node, left, right);
                    else if (isLogic(node.operator))
                        binaryLogic(r, node, left, right);
                    else if (isEquality(node.operator))
                        binaryEquality(r, node, left, right);
                });
    }

    // ---------------------------------------------------------------------------------------------

    private boolean isArithmetic(BinaryOperator op) {
        return op == ADD || op == MULTIPLY || op == SUBTRACT || op == DIVIDE || op == REMAINDER;
    }

    private boolean isComparison(BinaryOperator op) {
        return op == GREATER || op == GREATER_EQUAL || op == LOWER || op == LOWER_EQUAL;
    }

    private boolean isLogic(BinaryOperator op) {
        return op == OR || op == AND;
    }

    private boolean isEquality(BinaryOperator op) {
        return op == EQUALITY || op == NOT_EQUALS;
    }

    // ---------------------------------------------------------------------------------------------

    private void binaryArithmetic(Rule r, BinaryExpressionNode node, Type left, Type right) {
        if (left instanceof ArrayType) { // If its an Array, then check what kind of array
                                         // it is
            ArrayType leftArray = cast(left);

            if (right instanceof IntType) {
                IntType rightInt = cast(right);
            } else if (right instanceof FloatType) {
                FloatType rightInt = cast(right);
            }
            if (leftArray.componentType instanceof IntType) {
                if (right instanceof ArrayType) {
                    ArrayType rightArray = cast(right);
                    if (rightArray.componentType instanceof IntType)
                        r.set(0, new ArrayType(IntType.INSTANCE));
                    else if (rightArray.componentType instanceof FloatType)
                        r.set(0, new ArrayType(FloatType.INSTANCE));
                    else
                        r.error(arithmeticError(node, "Int", right), node);
                } else if (right instanceof IntType) {
                    r.set(0, new ArrayType(IntType.INSTANCE));
                } else if (right instanceof FloatType) {
                    r.set(0, new ArrayType(FloatType.INSTANCE));
                } else
                    r.error(arithmeticError(node, "Int", right), node);
            } else if (leftArray.componentType instanceof FloatType)
                if (right instanceof ArrayType) {
                    ArrayType rightArray = cast(right);
                    if (rightArray.componentType instanceof IntType
                            || rightArray.componentType instanceof FloatType)
                        r.set(0, new ArrayType(FloatType.INSTANCE));
                    else
                        r.error(arithmeticError(node, "Float", right), node);
                } else if (right instanceof IntType || right instanceof FloatType)
                    r.set(0, new ArrayType(FloatType.INSTANCE));
                else
                    r.error(arithmeticError(node, left, right), node);

        } else if (left instanceof IntType)
            if (right instanceof IntType)
                r.set(0, IntType.INSTANCE);
            else if (right instanceof FloatType)
                r.set(0, FloatType.INSTANCE);
            else
                r.error(arithmeticError(node, "Int", right), node);
        else if (left instanceof FloatType)
            if (right instanceof IntType || right instanceof FloatType)
                r.set(0, FloatType.INSTANCE);
            else
                r.error(arithmeticError(node, "Float", right), node);
        else
            r.error(arithmeticError(node, left, right), node);
    }

    // ---------------------------------------------------------------------------------------------

    private static String arithmeticError(BinaryExpressionNode node, Object left, Object right) {
        return format("Trying to %s %s with %s", node.operator.name().toLowerCase(), left, right);
    }

    // ---------------------------------------------------------------------------------------------

    private void binaryComparison(Rule r, BinaryExpressionNode node, Type left, Type right) {
        r.set(0, BoolType.INSTANCE);

        if (!(left instanceof IntType) && !(left instanceof FloatType))
            r.errorFor("Attempting to perform arithmetic comparison on non-numeric type: " + left,
                    node.left);
        if (!(right instanceof IntType) && !(right instanceof FloatType))
            r.errorFor("Attempting to perform arithmetic comparison on non-numeric type: " + right,
                    node.right);
    }

    // ---------------------------------------------------------------------------------------------

    private void binaryEquality(Rule r, BinaryExpressionNode node, Type left, Type right) {
        r.set(0, BoolType.INSTANCE);

        if (!isComparableTo(left, right))
            r.errorFor(format("Trying to compare incomparable types %s and %s", left, right),
                    node);
    }

    // ---------------------------------------------------------------------------------------------

    private void binaryLogic(Rule r, BinaryExpressionNode node, Type left, Type right) {
        r.set(0, BoolType.INSTANCE);

        if (!(left instanceof BoolType))
            r.errorFor("Attempting to perform binary logic on non-boolean type: " + left,
                    node.left);
        if (!(right instanceof BoolType))
            r.errorFor("Attempting to perform binary logic on non-boolean type: " + right,
                    node.right);
    }

    // ---------------------------------------------------------------------------------------------

    private void assignment(AssignmentNode node) {
        R.rule(node, "type")
                .using(node.left.attr("type"), node.right.attr("type")) // get the var being assigned and the value
                .by(r -> {
                    Type left = r.get(0);
                    Type right = r.get(1);

                    r.set(0, r.get(0)); // the type of the assignment is the left-side type

                    if (node.left instanceof ReferenceNode
                            || node.left instanceof FieldAccessNode
                            || node.left instanceof ArrayAccessNode
                            || node.left instanceof ClassFieldAccessNode) {
                        if (!isAssignableTo(right, left))
                            r.errorFor("Trying to assign a value to a non-compatible lvalue.", node);
                    } else
                        r.errorFor("Trying to assign to an non-lvalue expression.", node.left);
                });
    }

    // endregion
    // =============================================================================================
    // region [Types & Typing Utilities]
    // =============================================================================================

    private void simpleType(SimpleTypeNode node) {
        final Scope scope = this.scope;

        R.rule()
                .by(r -> {
                    // type declarations may occur after use
                    DeclarationContext ctx = scope.lookup(node.name);
                    DeclarationNode decl = ctx == null ? null : ctx.declaration;

                    if (ctx == null)
                        r.errorFor("could not resolve: " + node.name,
                                node,
                                node.attr("value"));

                    else if (!isTypeDecl(decl))
                        r.errorFor(format(
                                "%s did not resolve to a type declaration but to a %s declaration",
                                node.name, decl.declaredThing()),
                                node,
                                node.attr("value"));

                    else
                        R.rule(node, "value")
                                .using(decl, "declared")
                                .by(Rule::copyFirst);
                });
    }

    // ---------------------------------------------------------------------------------------------

    private void arrayType(ArrayTypeNode node) {
        R.rule(node, "value")
                .using(node.componentType, "value") // get the type of the component
                .by(r -> r.set(0, new ArrayType(r.get(0)))); // and create a arraytype that "wraps" the type of the
                                                             // component
    }

    // ---------------------------------------------------------------------------------------------

    private void setType(SetTypeNode node) {
        R.rule(node, "value")
                .using(node.componentType, "value")
                .by(r -> r.set(0, new SetType(r.get(0))));

    }

    // ---------------------------------------------------------------------------------------------
    private static boolean isTypeDecl(DeclarationNode decl) {
        if (decl instanceof StructDeclarationNode)
            return true;
        if (decl instanceof ClassNode)
            return true;
        if (decl instanceof TempTypeNode)
            return true;
        if (!(decl instanceof SyntheticDeclarationNode))
            return false;
        SyntheticDeclarationNode synthetic = cast(decl);
        return synthetic.kind() == DeclarationKind.TYPE;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Indicates whether a value of type {@code a} can be assigned to a location
     * (variable,
     * parameter, ...) of type {@code b}.
     */
    private static boolean isAssignableTo(Type a, Type b) {
        if (a instanceof VoidType || b instanceof VoidType)
            return false;

        if (a instanceof IntType && b instanceof FloatType)
            return true;

        if (a instanceof ArrayType)
            return b instanceof ArrayType
                    && isAssignableTo(((ArrayType) a).componentType, ((ArrayType) b).componentType);

        if (a instanceof SetType)
            return b instanceof SetType
                    && isAssignableTo(((SetType) a).componentType, ((SetType) b).componentType);

        return a instanceof NullType && b.isReference() || a.equals(b);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Indicate whether the two types are comparable.
     */
    private static boolean isComparableTo(Type a, Type b) {
        if (a instanceof VoidType || b instanceof VoidType)
            return false;

        return a.isReference() && b.isReference()
                || a.equals(b)
                || a instanceof IntType && b instanceof FloatType
                || a instanceof FloatType && b instanceof IntType;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the common supertype between both types, or {@code null} if no such
     * supertype
     * exists.
     */
    private static Type commonSupertype(Type a, Type b) {
        if (a instanceof VoidType || b instanceof VoidType)
            return null;
        if (isAssignableTo(a, b))
            return b;
        if (isAssignableTo(b, a))
            return a;
        else
            return null;
    }

    // endregion
    // =============================================================================================
    // region [Scopes & Declarations]
    // =============================================================================================

    private void popScope(SighNode node) {
        scope = scope.parent;
    }

    // ---------------------------------------------------------------------------------------------

    private void root(RootNode node) {
        assert scope == null;
        scope = new RootScope(node, R);
        R.set(node, "scope", scope);
    }

    // ---------------------------------------------------------------------------------------------

    private void block(BlockNode node) {
        scope = new Scope(node, scope);
        R.set(node, "scope", scope);

        Attribute[] deps = getReturnsDependencies(node.statements);
        R.rule(node, "returns")
                .using(deps)
                .by(r -> r.set(0, deps.length != 0 && Arrays.stream(deps).anyMatch(r::get)));
    }

    // ---------------------------------------------------------------------------------------------

    private void varDecl(VarDeclarationNode node) {
        this.inferenceContext = node;

        scope.declare(node.name, node);
        R.set(node, "scope", scope);

        R.rule(node, "type")
                .using(node.type, "value") // it's just the "type" that is inside the node, we set it only getting the
                                           // value
                .by(Rule::copyFirst);

        R.rule()
                .using(node.type.attr("value"), node.initializer.attr("type"))
                .by(r -> {
                    Type expected = r.get(0);
                    Type actual = r.get(1);
                    //System.out.println("Enter1: "+node.name+"-> expected: "+expected+", actual: "+actual);

                    if (!isAssignableTo(actual, expected)) {//checking if it's the same type, and case of Polymorphism (diff types)
                        boolean error_assign=true;
                        String nameFun = r.dependencies[1].node+"";
                        if(nameFun.contains("$"))
                            nameFun = nameFun.substring(nameFun.indexOf("$") + 1);
                        if(nameFun.contains("("))
                            nameFun = nameFun.split("\\(")[0];

                        for(String item: functionsDecl.keySet()){
                            if(item.contains(nameFun)) {
                                FunDeclarationNode tmp = functionsDecl.get(item);
                                if (tmp.returnType.contents().equals(expected.toString())) {
                                    error_assign = false;
                                    break;
                                }
                            }

                        }

                        if(error_assign)
                            r.error(format(
                                    "incompatible initializer type provided for variable `%s`: expected %s but got %s",
                                    node.name, expected, actual),
                                node.initializer);

                    }
                });
    }

    // ---------------------------------------------------------------------------------------------

    private void fieldDecl(FieldDeclarationNode node) {
        R.set(node, "scope", scope);
        scope.declare(node.name, node);//added

        R.rule(node, "type")
                .using(node.type, "value")
                .by(Rule::copyFirst);
    }

    // ---------------------------------------------------------------------------------------------

    private void parameter(ParameterNode node) {
        R.set(node, "scope", scope);
        scope.declare(node.name, node); // scope pushed by FunDeclarationNode

        R.rule(node, "type")
                .using(node.type, "value")
                .by(Rule::copyFirst);
    }

    // ---------------------------------------------------------------------------------------------

    private void funDecl(FunDeclarationNode node) {
        scope.declare(node.name, node);
        scope = new Scope(node, scope);
        R.set(node, "scope", scope); // declare a scope for parameters

        String concatFunctionParam = node.name + " ";

        Attribute[] dependencies = new Attribute[node.parameters.size() + 1];
        dependencies[0] = node.returnType.attr("value");
        forEachIndexed(node.parameters, (i, param) ->
            dependencies[i + 1] = param.attr("type"));

        String tipos = "[";
        for(ParameterNode param: node.parameters){
            tipos=tipos+param.type.contents()+", ";
        }
        tipos=tipos+"]";
        tipos=tipos.replace(", ]","]");
        concatFunctionParam=concatFunctionParam+tipos;
        //System.out.println("concatFunctionParam: "+concatFunctionParam);
        functionsDecl.put(concatFunctionParam, node);

        R.rule(node, "type")
                .using(dependencies)
                .by(r -> {
                    Type[] paramTypes = new Type[node.parameters.size()];
                    for (int i = 0; i < paramTypes.length; ++i)
                        paramTypes[i] = r.get(i + 1);

                    r.set(0, new FunType(r.get(0), paramTypes));
                });

        R.rule()
                .using(node.block.attr("returns"), node.returnType.attr("value"))
                .by(r -> {
                    boolean returns = r.get(0);
                    Type returnType = r.get(1);
                    if (!returns && !(returnType instanceof VoidType))
                        r.error("Missing return in function.", node);
                    // NOTE: The returned value presence & type is checked in returnStmt().
                });
    }

    // ---------------------------------------------------------------------------------------------

    private void structDecl(StructDeclarationNode node) {
        scope.declare(node.name, node);
        R.set(node, "type", TypeType.INSTANCE);
        R.set(node, "declared", new StructType(node));
    }

    // TEMPLATES: NEW FEATURE

    private void templateCall(TempCallNode node) {

        // ok, here we have to copy the whole subtree with new nodes, and I have no clue
        // of how to do it
        this.inferenceContext = node;

        Attribute[] dependencies = new Attribute[(node.arguments.size() + node.types.size()) + 1];
        dependencies[0] = node.template.attr("type");
        forEachIndexed(node.arguments, (i, arg) -> {
            dependencies[i + 1] = arg.attr("type"); // get all the attributess
            R.set(arg, "index", i);
        });

        forEachIndexed(node.types, (i, types) -> {
            dependencies[i + node.arguments.size() + 1] = types.attr("value");
            R.set(types, "index", (i + node.arguments.size()));
        });

        R.rule(node, "type")
                .using(dependencies) // First: compute dependencies
                .by(r -> {
                    Type maybeTempType = r.get(0);

                    if (!(maybeTempType instanceof TempType)) {
                        r.error("trying to call a non-template expression: " + node.template,
                                node.template);
                        return;
                    }

                    TempType tempType = cast(maybeTempType);
                    r.set(0, tempType.returnType); // return type will be

                    Type[] params = tempType.paramsTypes;
                    List<ExpressionNode> args = node.arguments;

                    Type[] expectedTypes = tempType.passedTpsTypes;
                    List<TypeNode> passedTypes = node.types;

                    if (params.length != args.size())
                        r.errorFor(format("wrong number of arguments, expected %d but got %d",
                                params.length, args.size()),
                                node);

                    if (expectedTypes.length != passedTypes.size())
                        r.errorFor(format("wrong number of types passed, expected %d but got %d",
                                expectedTypes.length, passedTypes.size()),
                                node);

                    Map<String, String> typesMap = new HashMap<>();

                    /**
                     * int checkedArgs = Math.min(params.length, args.size());
                     *
                     * for (int i = 0; i < checkedArgs; ++i) {
                     * Type argType = r.get(i + 1);
                     * Type paramType = funType.paramTypes[i];
                     * if (!isAssignableTo(argType, paramType))
                     * r.errorFor(format(
                     * "incompatible argument provided for argument %d: expected %s but got %s",
                     * i, paramType, argType),
                     * node.arguments.get(i));
                     * }
                     */

                    int checkedTypes = Math.min(params.length, args.size());

                    // for (int i = 0; i < checkedTypes; ++i) {
                    // typesMap.put()
                    // }
                    // Check types of arguments: runtime maybe?
                    // Idea: Try to associate types to the general "T", and check the types if the
                    // arguments are for that T

                    // I don't need to check if the passed types are really types because it's
                    // already done at SimpleType
                });

    }

    private void templateDeclTypes(TempTypeNode node) {
        // I think this is right. We are declaring the type "T" as a new instance of
        // type, and we'll associate the value of it to the passed (not in this phase, I
        // think it'll be in the interpreter)

        // R.set(node, "scope", scope);
        // scope.declare(node.name, node); // scope pushed by TempDeclarationNode

        // R.rule(node, "type")
        // .using(node.type, "value")
        // .by(Rule::copyFirst);

        // Old code up

        scope.declare(node.name, node);
        R.set(node, "type", TypeType.INSTANCE);
        R.set(node, "declared", new TempTypeType(node));

    }

    /**
     * Hey!
     *
     * So the way this works without templates is that the node for "a" (indeed a
     * ReferenceNode) will have its "type" attribute set to the value of the "type"
     * attribute of the corresponding declaration (which in this case is the
     * parameter declaration "a: T" of type ParameterNode).
     *
     * I'm not sure the exact problem that you're facing, but I see sort of the kind
     * of problem that it is: you essentially need the reactor to retype the whole
     * function for each "instantiation" (i.e. each use with different type) of it.
     *
     * What I would do is something along the following lines:
     * - make sure a template function is not typed by default (you probably already
     * do this? make sure your code compile & runs when a template function is
     * defined but never called!) (Kinda type it but leave the values private,
     * because it doesn't check private values)
     * - at first, ignore the exact notion of "instantiation" (one per type) and
     * just consider that each call is its own instantiation of the function
     * - so for each call, create a copy of the whole function declaration (the
     * whole subtree)- this will require you to write copy logic for all AST
     * nodes... // Copy the tree at each instantiation and then type it there
     * - but I think you can write it with reflection to make it much easier
     * - you can get inspiration from the reflective hashcode/equals I just added
     * here:
     * https://github.com/norswap/sigh/blob/master/src/norswap/sigh/ast/SighNode.java#L66-L135
     * (you want to implement `Cloneable` on `SighNode`, and in the `clone` method
     * recursively call clone on all fields and assign them via reflection)
     * - so, for each call to a template function, you create a copy for the
     * function declaration...
     * - but this time you set the right type for the `a: T` declaration for that
     * particular instantiation
     * - then you just create a rule that will type this copy of the function tree
     * - so now all references in that copy will refer to the `a: T` that has the
     * right type, and they will have their "type" attribute set correctly as a
     * result
     *
     * There you go, I hope it helps!
     */
    private void templateDecl(TempDeclarationNode node) {

        // scope.declare(node.name, node);
        // scope = new Scope(node, scope);
        // R.set(node, "scope", scope); // declare a scope for template types and
        // function parameters
        /*
         * Attribute[] dependencies = new Attribute[node.temp_types.size() +
         * node.parameters.size() + 1];
         * dependencies[0] = node.returnType.attr("value");
         * forEachIndexed(node.temp_types, (i, param) -> dependencies[i + 1] =
         * param.attr("type"));
         * forEachIndexed(node.parameters,
         * (i, param) -> dependencies[i + node.temp_types.size() + 1] =
         * param.attr("type"));
         *
         * R.rule(node, "type")
         * .using(dependencies)
         * .by(r -> {
         * Type[] passedTpsAndParamsTypes = new Type[node.temp_types.size() +
         * node.parameters.size()];
         * for (int i = 0; i < passedTpsAndParamsTypes.length; ++i)
         * passedTpsAndParamsTypes[i] = r.get(i + 1);
         * r.set(0, new TempType(r.get(0), node.temp_types.size(),
         * passedTpsAndParamsTypes));
         * });
         *
         * R.rule()
         * .using(node.block.attr("returns"), node.returnType.attr("value"))
         * .by(r -> {
         * boolean returns = r.get(0);
         * Type returnType = r.get(1);
         * if (!returns && !(returnType instanceof VoidType))
         * r.error("Missing return in function.", node);
         * // NOTE: The returned value presence & type is checked in returnStmt().
         * });
         */
    }

    // private void genFunDecl(GenericFunDeclarationNode node) {
    // scope.declare(node.name, node);
    // scope = new Scope(node, scope);
    // R.set(node, "scope", scope); // declare a scope for parameters

    // Attribute[] dependencies = new Attribute[node.parameters.size() + 1];

    // dependencies[0] = node.returnType.attr("value");
    // forEachIndexed(node.parameters, (i, param) -> dependencies[i + 1] =
    // param.attr("type"));

    // R.rule(node, "type")
    // .using(dependencies)
    // .by(r -> {
    // Type[] paramTypes = new Type[node.parameters.size()];
    // for (int i = 0; i < paramTypes.length; ++i)
    // paramTypes[i] = r.get(i + 1);
    // r.set(0, new FunType(r.get(0), paramTypes));
    // });

    // R.rule()
    // .using(node.block.attr("returns"), node.returnType.attr("value"))
    // .by(r -> {
    // boolean returns = r.get(0);
    // Type returnType = r.get(1);
    // if (!returns && !(returnType instanceof VoidType))
    // r.error("Missing return in function.", node);
    // // NOTE: The returned value presence & type is checked in returnStmt().
    // });
    // }
    // Not needed anymore, since we don't have anymore the genfundecl

    // endregion
    // =============================================================================================
    // region [Other Statements]
    // =============================================================================================

    private void ifStmt(IfNode node) {
        R.rule()
                .using(node.condition, "type")
                .by(r -> {
                    Type type = r.get(0);
                    if (!(type instanceof BoolType)) {
                        r.error("If statement with a non-boolean condition of type: " + type,
                                node.condition);
                    }
                });

        Attribute[] deps = getReturnsDependencies(list(node.trueStatement, node.falseStatement));
        R.rule(node, "returns")
                .using(deps)
                .by(r -> r.set(0, deps.length == 2 && Arrays.stream(deps).allMatch(r::get)));
    }

    // ---------------------------------------------------------------------------------------------

    private void whileStmt(WhileNode node) {
        R.rule()
                .using(node.condition, "type")
                .by(r -> {
                    Type type = r.get(0);
                    if (!(type instanceof BoolType)) {
                        r.error("While statement with a non-boolean condition of type: " + type,
                                node.condition);
                    }
                });
    }

    // ---------------------------------------------------------------------------------------------

    private void returnStmt(ReturnNode node) {
        R.set(node, "returns", true);

        FunDeclarationNode function = currentFunction();
        if (function == null) // top-level return
            return;

        if (node.expression == null)
            R.rule()
                    .using(function.returnType, "value")
                    .by(r -> {
                        Type returnType = r.get(0);
                        if (!(returnType instanceof VoidType))
                            r.error("Return without value in a function with a return type.", node);
                    });
        else
            R.rule()
                    .using(function.returnType.attr("value"), node.expression.attr("type"))
                    .by(r -> {
                        Type formal = r.get(0);
                        Type actual = r.get(1);
                        if (formal instanceof VoidType)
                            r.error("Return with value in a Void function.", node);
                        else if (!isAssignableTo(actual, formal)) {
                            r.errorFor(format(
                                    "Incompatible return type, expected %s but got %s", formal, actual),
                                    node.expression);
                        }
                    });
    }

    // ---------------------------------------------------------------------------------------------

    private FunDeclarationNode currentFunction() {
        Scope scope = this.scope;
        while (scope != null) {
            SighNode node = scope.node;
            if (node instanceof FunDeclarationNode)
                return (FunDeclarationNode) node;
            scope = scope.parent;
        }
        return null;
    }

    // ---------------------------------------------------------------------------------------------

    private boolean isReturnContainer(SighNode node) {
        return node instanceof BlockNode
                || node instanceof IfNode
                || node instanceof ReturnNode;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Get the depedencies necessary to compute the "returns" attribute of the
     * parent.
     */
    private Attribute[] getReturnsDependencies(List<? extends SighNode> children) {
        return children.stream()
                .filter(Objects::nonNull)
                .filter(this::isReturnContainer)
                .map(it -> it.attr("returns"))
                .toArray(Attribute[]::new);
    }

    // endregion
    // =============================================================================================
}