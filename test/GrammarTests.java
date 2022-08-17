
import norswap.autumn.AutumnTestFixture;
import norswap.sigh.SighGrammar;
import norswap.sigh.ast.*;

import org.testng.annotations.Test;

import static java.util.Arrays.asList;
import static norswap.sigh.ast.BinaryOperator.*;

public class GrammarTests extends AutumnTestFixture {
        // ---------------------------------------------------------------------------------------------

        private final SighGrammar grammar = new SighGrammar();
        private final Class<?> grammarClass = grammar.getClass();

        // ---------------------------------------------------------------------------------------------

        private static IntLiteralNode intlit (long i) {
                return new IntLiteralNode(null, i);
        }

        private static FloatLiteralNode floatlit (double d) {
                return new FloatLiteralNode(null, d);
        }

        private static StringLiteralNode stringlit (String s) {
                return new StringLiteralNode(null, s);
        }

        // ---------------------------------------------------------------------------------------------

        @Test
        public void testLiteralsAndUnary () {
                rule = grammar.expression;

                successExpect("42", intlit(42));
                successExpect("42.0", floatlit(42d));
                successExpect("\"hello\"", new StringLiteralNode(null, "hello"));
                successExpect("(42)", new ParenthesizedNode(null, intlit(42)));
                successExpect("[1, 2, 3]", new ArrayLiteralNode(null, asList(intlit(1), intlit(2), intlit(3))));
                successExpect("{1, 2, 3}", new SetLiteralNode(null, asList(intlit(1), intlit(2), intlit(3))));
                successExpect("true", new ReferenceNode(null, "true"));
                successExpect("false", new ReferenceNode(null, "false"));
                successExpect("null", new ReferenceNode(null, "null"));
                successExpect("!false",
                    new UnaryExpressionNode(null, UnaryOperator.NOT, new ReferenceNode(null, "false")));
        }

        // ---------------------------------------------------------------------------------------------

        @Test
        public void testNumericBinary () {
                successExpect("1 + 2", new BinaryExpressionNode(null, intlit(1), ADD, intlit(2)));
                successExpect("2 - 1", new BinaryExpressionNode(null, intlit(2), SUBTRACT, intlit(1)));
                successExpect("2 * 3", new BinaryExpressionNode(null, intlit(2), MULTIPLY, intlit(3)));
                successExpect("2 / 3", new BinaryExpressionNode(null, intlit(2), DIVIDE, intlit(3)));
                successExpect("2 % 3", new BinaryExpressionNode(null, intlit(2), REMAINDER, intlit(3)));

                successExpect("1.0 + 2.0", new BinaryExpressionNode(null, floatlit(1), ADD, floatlit(2)));
                successExpect("2.0 - 1.0", new BinaryExpressionNode(null, floatlit(2), SUBTRACT, floatlit(1)));
                successExpect("2.0 * 3.0", new BinaryExpressionNode(null, floatlit(2), MULTIPLY, floatlit(3)));
                successExpect("2.0 / 3.0", new BinaryExpressionNode(null, floatlit(2), DIVIDE, floatlit(3)));
                successExpect("2.0 % 3.0", new BinaryExpressionNode(null, floatlit(2), REMAINDER, floatlit(3)));

                successExpect("2 * (4-1) * 4.0 / 6 % (2+1)", new BinaryExpressionNode(null,
                    new BinaryExpressionNode(null,
                        new BinaryExpressionNode(null,
                            new BinaryExpressionNode(null,
                                intlit(2),
                                MULTIPLY,
                                new ParenthesizedNode(null,
                                    new BinaryExpressionNode(
                                        null,
                                        intlit(4),
                                        SUBTRACT,
                                        intlit(1)))),
                            MULTIPLY,
                            floatlit(4d)),
                        DIVIDE,
                        intlit(6)),
                    REMAINDER,
                    new ParenthesizedNode(null, new BinaryExpressionNode(null,
                        intlit(2),
                        ADD,
                        intlit(1)))));
        }

        // ---------------------------------------------------------------------------------------------

        @Test
        public void testArrayStructAccess () {
                rule = grammar.expression;
                successExpect("[1][0]", new ArrayAccessNode(null,
                    new ArrayLiteralNode(null, asList(intlit(1))), intlit(0)));
                successExpect("[1].length", new FieldAccessNode(null,
                    new ArrayLiteralNode(null, asList(intlit(1))), "length"));
                successExpect("p.x", new FieldAccessNode(null, new ReferenceNode(null, "p"), "x"));
        }

        // ---------------------------------------------------------------------------------------------

        @Test
        public void testDeclarations () {
                rule = grammar.statement;

                successExpect("var x: Int = 1", new VarDeclarationNode(null,
                    "x", new SimpleTypeNode(null, "Int"), intlit(1)));

                successExpect("var x: Int{} = {1, 2, 3}", new VarDeclarationNode(null,
                    "x", new SetTypeNode(null, new SimpleTypeNode(null, "Int")),
                    new SetLiteralNode(null, asList(intlit(1), intlit(2), intlit(3)))));

                successExpect("struct P {}", new StructDeclarationNode(null, "P", asList()));

                successExpect("struct P { var x: Int; var y: Int }",
                    new StructDeclarationNode(null, "P", asList(
                        new FieldDeclarationNode(null, "x", new SimpleTypeNode(null, "Int")),
                        new FieldDeclarationNode(null, "y", new SimpleTypeNode(null, "Int")))));

                successExpect("fun f (x: Int): Int { return 1 }",
                    new FunDeclarationNode(null, "f",
                        asList(new ParameterNode(null, "x", new SimpleTypeNode(null, "Int"))),
                        new SimpleTypeNode(null, "Int"),
                        new BlockNode(null, asList(new ReturnNode(null, intlit(1))))));

                successExpect("template <T: Type> fun f (x: T): T { return 1 }",
                    new TempDeclarationNode(null,
                        asList(new TempTypeNode(null, "T", new SimpleTypeNode(null, "Type"))),
                        "f",
                        asList(new ParameterNode(null, "x", new SimpleTypeNode(null, "T"))),
                        new SimpleTypeNode(null, "T"),
                        new BlockNode(null, asList(new ReturnNode(null, intlit(1))))));

                successExpect("var x: Int = f<Int>(3)", new VarDeclarationNode(null,
                    "x", new SimpleTypeNode(null, "Int"),
                    new TempCallNode(null, new ReferenceNode(null, "f"),
                        asList(new SimpleTypeNode(null, "Int")),
                        asList(new IntLiteralNode(null, 3)))));
        }

        // ---------------------------------------------------------------------------------------------

        @Test
        public void testStatements () {
                rule = grammar.statement;

                successExpect("return", new ReturnNode(null, null));
                successExpect("return 1", new ReturnNode(null, intlit(1)));
                successExpect("print(1)", new ExpressionStatementNode(null,
                    new FunCallNode(null, new ReferenceNode(null, "print"), asList(intlit(1)))));
                successExpect("addSetInt({2, 3}, 1)", new ExpressionStatementNode(null,
                    new FunCallNode(null, new ReferenceNode(null, "addSetInt"), asList(
                        new SetLiteralNode(null, asList(intlit(2), intlit(3))), intlit(1)))));
                successExpect("containsSetFloat({4.5, 6.2}, 3.4)", new ExpressionStatementNode(null,
                    new FunCallNode(null, new ReferenceNode(null, "containsSetFloat"), asList(
                        new SetLiteralNode(null, asList(floatlit(4.5), floatlit(6.2))),
                        floatlit(3.4)))));
                successExpect("print(1)", new ExpressionStatementNode(null,
                    new FunCallNode(null, new ReferenceNode(null, "print"), asList(intlit(1)))));
                successExpect("{ return }", new BlockNode(null, asList(new ReturnNode(null, null))));

                successExpect("if true return 1 else return 2", new IfNode(null, new ReferenceNode(null, "true"),
                    new ReturnNode(null, intlit(1)),
                    new ReturnNode(null, intlit(2))));

                successExpect("if false return 1 else if true return 2 else return 3 ",
                    new IfNode(null, new ReferenceNode(null, "false"),
                        new ReturnNode(null, intlit(1)),
                        new IfNode(null, new ReferenceNode(null, "true"),
                            new ReturnNode(null, intlit(2)),
                            new ReturnNode(null, intlit(3)))));

                successExpect("while 1 < 2 { return } ", new WhileNode(null,
                    new BinaryExpressionNode(null, intlit(1), LOWER, intlit(2)),
                    new BlockNode(null, asList(new ReturnNode(null, null)))));
        }

        /////
        ////NEW GRAMMAR TESTS
        @Test
        public void testFunDeclarationModifiers () {
                rule = grammar.statement;
                successExpect("pub fun f (x: Float): Float { return 2.5 }",
                    new GenericFunDeclarationNode(null, new SimpleTypeNode(null, "pub"),
                        "f",
                        asList(new ParameterNode(null, "x", new SimpleTypeNode(null, "Float"))),
                        new SimpleTypeNode(null, "Float"),
                        new BlockNode(null, asList(new ReturnNode(null, floatlit(2.5))))));


                successExpect("pvt fun f (x: Int): Int { return 2 }",
                    new GenericFunDeclarationNode(null, new SimpleTypeNode(null, "pvt"),
                        "f",
                        asList(new ParameterNode(null, "x", new SimpleTypeNode(null, "Int"))),
                        new SimpleTypeNode(null, "Int"),
                        new BlockNode(null, asList(new ReturnNode(null, intlit(2))))));


                successExpect("pub fun example(){}",
                    new GenericFunDeclarationNode(null,
                        new SimpleTypeNode(null,"pub"),
                        "example",
                        asList(),
                        new SimpleTypeNode(null, "Void"),
                        new BlockNode(null, asList())));

        }

        @Test
        public void objectCreation () {
                rule = grammar.object_constructor;
                System.out.println("constructor: "+new ClassConstructorNode(null,
                    new ReferenceNode(null, "Dog")));
                ClassConstructorNode call = new ClassConstructorNode(null,
                    new ReferenceNode(null, "Dog")
                );
                successExpect("create Dog",call);

        }

        @Test
        public void classDeclaration () {
                rule = grammar.statement;
                String input = "" +
                    "pub class Dog{\n" +
                    "   var age: Int\n"+
                    "   fun get_age(): Int {\n" +
                    "       return age\n" +
                    "}\n"+
                    "}\n"
                    ;

                successExpect(input,
                    new ClassNode(null, new SimpleTypeNode(null, "pub"), "Dog", null,
                        new BlockNode(null,
                            asList(
                                new FieldDeclarationNode(null, "age", new SimpleTypeNode(null, "Int")),
                                new FunDeclarationNode(null, "get_age", asList(), new SimpleTypeNode(null, "Int"),
                                    new BlockNode(null, asList(new ReturnNode(null, new ReferenceNode(null, "age")))))
                            ))));
        }


        @Test public void testClassAccessParameter() {
                rule = grammar.expression;
                ClassFieldAccessNode call = new ClassFieldAccessNode(null,
                    new ReferenceNode(null, "pitbull"), "age");

                successExpect("pitbull$age",call);
        }


        @Test
        public void testClassDeclarationNodeInheritance () {
                rule = grammar.statement;

                ClassNode cl = new ClassNode(null,
                    new SimpleTypeNode(null, "pub"), "Car",
                    asList(
                        "Vehicule"),
                    new BlockNode(null, asList(
                        new VarDeclarationNode(null,
                            "modelName", new SimpleTypeNode(null, "String"), stringlit("Chevrolet"))
                    )
                    ));
                successExpect("pub class Car from Vehicule {var modelName: String = \"Chevrolet\"}", cl);


                ClassNode c_node = new ClassNode(null,
                    new SimpleTypeNode(null, "pub"), "Dog",
                    asList(
                        "Canine","Animal"),
                    new BlockNode(null, asList(
                        new GenericFunDeclarationNode(null, new SimpleTypeNode(null, "pvt"),
                            "f",
                            asList(new ParameterNode(null, "x", new SimpleTypeNode(null, "Int"))),
                            new SimpleTypeNode(null, "Int"),
                            new BlockNode(null, asList(new ReturnNode(null, intlit(2)))))
                    )
                    ));
                successExpect("pub class Dog from Canine,Animal{pvt fun f (x: Int): Int { return 2 }}", c_node);

        }


        @Test
        public void testClassParamMeth () {
                String c_example = "" +
                    "pub class Dog{\n" +
                    "   var name: String\n"+
                    "   var age: Int\n"+
                    "   pvt fun get_age(): Int {\n" +
                    "       return age\n" +
                    "   }\n" +
                    "   pvt fun get_name(): String {\n" +
                    "       return name\n" +
                    "   }\n" +
                    "   pvt fun set_age(current_age: Int) {\n" +
                    "       age=current_age\n" +
                    "   }\n" +
                    "   pvt fun set_name(dog_name: String) {\n" +
                    "       name=dog_name\n" +
                    "   }\n" +
                    "}\n";

                rule = grammar.statement;
                ClassNode c_node = new ClassNode(null,
                    new SimpleTypeNode(null, "pub"), "Dog", null,
                    new BlockNode(null, asList(
                        new FieldDeclarationNode(null, "name", new SimpleTypeNode(null, "String")),
                        new FieldDeclarationNode(null, "age", new SimpleTypeNode(null, "Int")),
                        new GenericFunDeclarationNode(null,
                            new SimpleTypeNode(null, "pvt"),
                            "get_age",
                            asList(),
                            new SimpleTypeNode(null, "Int"),
                            new BlockNode(null, asList(new ReturnNode(null,
                                new ReferenceNode(null, "age"))))),
                        new GenericFunDeclarationNode(null,
                            new SimpleTypeNode(null, "pvt"),
                            "get_name",
                            asList(),
                            new SimpleTypeNode(null, "String"),
                            new BlockNode(null, asList(new ReturnNode(null,
                                new ReferenceNode(null, "name"))))),
                        new GenericFunDeclarationNode(null,
                            new SimpleTypeNode(null,"pvt"),
                            "set_age",
                            asList(new ParameterNode(null, "current_age", new SimpleTypeNode(null, "Int"))),
                            new SimpleTypeNode(null, "Void"),
                            new BlockNode(null, asList(new ExpressionStatementNode(null,
                                new AssignmentNode(null, new ReferenceNode(null, "age"),
                                    new ReferenceNode(null, "current_age")))))),
                        new GenericFunDeclarationNode(null,
                            new SimpleTypeNode(null,"pvt"),
                            "set_name",
                            asList(new ParameterNode(null, "dog_name", new SimpleTypeNode(null, "String"))),
                            new SimpleTypeNode(null, "Void"),
                            new BlockNode(null, asList(new ExpressionStatementNode(null,
                                new AssignmentNode(null, new ReferenceNode(null, "name"),
                                    new ReferenceNode(null, "dog_name"))))))

                    )

                    ));

                successExpect(c_example, c_node);
        }


        @Test
        public void testPolimorfism(){
                String c_example = "" +
                    "pub class Operation{\n" +
                    "   fun sumarBothEnds(a: Int, b: Int): Int {\n" +
                    "       return a + b\n" +
                    "   }\n" +
                    "   fun sumarBothEnds(a: Int, b: Int, c:Int): Int {\n" +
                    "       return a + c\n" +
                    "   }\n" +
                    "}\n";

                rule = grammar.statement;

                ClassNode c_node = new ClassNode(null,
                    new SimpleTypeNode(null, "pub"), "Operation", null,
                    new BlockNode(null, asList(
                        new FunDeclarationNode(null,
                            "sumarBothEnds",
                            asList(new ParameterNode(null, "a", new SimpleTypeNode(null, "Int")),
                                new ParameterNode(null, "b", new SimpleTypeNode(null, "Int"))),
                            new SimpleTypeNode(null, "Int"),
                            new BlockNode(null, asList(new ReturnNode(null,
                                new BinaryExpressionNode(null, new ReferenceNode(null, "a"),
                                    ADD, new ReferenceNode(null, "b")))))),
                        new FunDeclarationNode(null,
                            "sumarBothEnds",
                            asList(new ParameterNode(null, "a", new SimpleTypeNode(null, "Int")),
                                new ParameterNode(null, "b", new SimpleTypeNode(null, "Int")),
                                new ParameterNode(null, "c", new SimpleTypeNode(null, "Int"))),
                            new SimpleTypeNode(null, "Int"),
                            new BlockNode(null, asList(new ReturnNode(null,
                                new BinaryExpressionNode(null,
                                    new ReferenceNode(null, "a"),
                                    ADD, new ReferenceNode(null, "c")))))

                        )
                    )

                    ));

                successExpect(c_example, c_node);

        }


}