import norswap.autumn.AutumnTestFixture;
import norswap.autumn.Grammar;
import norswap.autumn.Grammar.rule;
import norswap.autumn.ParseResult;
import norswap.autumn.positions.LineMapString;
import norswap.sigh.SemanticAnalysis;
import norswap.sigh.SighGrammar;
import norswap.sigh.ast.FieldDeclarationNode;
import norswap.sigh.ast.SighNode;
import norswap.sigh.ast.SimpleTypeNode;
import norswap.sigh.interpreter.Interpreter;
import norswap.sigh.interpreter.InterpreterException;
import norswap.sigh.interpreter.Null;
import norswap.uranium.Reactor;
import norswap.uranium.SemanticError;
import norswap.utils.IO;
import norswap.utils.TestFixture;
import norswap.utils.data.wrappers.Pair;
import norswap.utils.visitors.Walker;
import org.testng.annotations.Test;
import java.util.HashMap;
import java.util.Set;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertThrows;

public final class InterpreterTests extends TestFixture {

    // TODO peeling

    // ---------------------------------------------------------------------------------------------

    private final SighGrammar grammar = new SighGrammar();
    private final AutumnTestFixture autumnFixture = new AutumnTestFixture();

    {
        autumnFixture.runTwice = false;
        autumnFixture.bottomClass = this.getClass();
    }

    // ---------------------------------------------------------------------------------------------

    private Grammar.rule rule;

    // ---------------------------------------------------------------------------------------------

    private void check(String input, Object expectedReturn) {
        assertNotNull(rule, "You forgot to initialize the rule field.");
        check(rule, input, expectedReturn, null);
    }

    // ---------------------------------------------------------------------------------------------

    private void check(String input, Object expectedReturn, String expectedOutput) {
        assertNotNull(rule, "You forgot to initialize the rule field.");
        check(rule, input, expectedReturn, expectedOutput);
    }

    // ---------------------------------------------------------------------------------------------

    private void check(rule rule, String input, Object expectedReturn, String expectedOutput) {
        // TODO
        // (1) write proper parsing tests
        // (2) write some kind of automated runner, and use it here

        autumnFixture.rule = rule;
        ParseResult parseResult = autumnFixture.success(input);
        SighNode root = parseResult.topValue();

        Reactor reactor = new Reactor();
        Walker<SighNode> walker = SemanticAnalysis.createWalker(reactor);
        Interpreter interpreter = new Interpreter(reactor);
        walker.walk(root);
        reactor.run();
        Set<SemanticError> errors = reactor.errors();

        if (!errors.isEmpty()) {
            LineMapString map = new LineMapString("<test>", input);
            String report = reactor
                .reportErrors(it -> it.toString() + " (" + ((SighNode) it).span.startString(map) + ")");
            // String tree = AttributeTreeFormatter.format(root, reactor,
            // new ReflectiveFieldWalker<>(SighNode.class, PRE_VISIT, POST_VISIT));
            // System.err.println(tree);
            throw new AssertionError(report);
        }

        Pair<String, Object> result = IO.captureStdout(() -> interpreter.interpret(root));
        assertEquals(result.b, expectedReturn);
        if (expectedOutput != null)
            assertEquals(result.a, expectedOutput);
    }

    // ---------------------------------------------------------------------------------------------

    private void checkExpr(String input, Object expectedReturn, String expectedOutput) {
        rule = grammar.root;
        check("return " + input, expectedReturn, expectedOutput);
    }

    // ---------------------------------------------------------------------------------------------

    private void checkExpr(String input, Object expectedReturn) {
        rule = grammar.root;
        check("return " + input, expectedReturn);
    }

    // ---------------------------------------------------------------------------------------------

    private void checkThrows(String input, Class<? extends Throwable> expected) {
        assertThrows(expected, () -> check(input, null));
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testLiteralsAndUnary() {
        checkExpr("42", 42L);
        checkExpr("42.0", 42.0d);
        checkExpr("\"hello\"", "hello");
        checkExpr("(42)", 42L);
        checkExpr("[1, 2, 3]", new Object[] { 1L, 2L, 3L });
        checkExpr("true", true);
        checkExpr("false", false);
        checkExpr("null", Null.INSTANCE);
        checkExpr("!false", true);
        checkExpr("!true", false);
        checkExpr("!!true", true);
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testNumericBinary() {
        checkExpr("1 + 2", 3L);
        checkExpr("2 - 1", 1L);
        checkExpr("2 * 3", 6L);
        checkExpr("2 / 3", 0L);
        checkExpr("3 / 2", 1L);
        checkExpr("2 % 3", 2L);
        checkExpr("3 % 2", 1L);

        checkExpr("1.0 + 2.0", 3.0d);
        checkExpr("2.0 - 1.0", 1.0d);
        checkExpr("2.0 * 3.0", 6.0d);
        checkExpr("2.0 / 3.0", 2d / 3d);
        checkExpr("3.0 / 2.0", 3d / 2d);
        checkExpr("2.0 % 3.0", 2.0d);
        checkExpr("3.0 % 2.0", 1.0d);

        checkExpr("1 + 2.0", 3.0d);
        checkExpr("2 - 1.0", 1.0d);
        checkExpr("2 * 3.0", 6.0d);
        checkExpr("2 / 3.0", 2d / 3d);
        checkExpr("3 / 2.0", 3d / 2d);
        checkExpr("2 % 3.0", 2.0d);
        checkExpr("3 % 2.0", 1.0d);

        checkExpr("1.0 + 2", 3.0d);
        checkExpr("2.0 - 1", 1.0d);
        checkExpr("2.0 * 3", 6.0d);
        checkExpr("2.0 / 3", 2d / 3d);
        checkExpr("3.0 / 2", 3d / 2d);
        checkExpr("2.0 % 3", 2.0d);
        checkExpr("3.0 % 2", 1.0d);

        checkExpr("2 * (4-1) * 4.0 / 6 % (2+1)", 1.0d);

        // tests for array programming
        checkExpr("[1, 2] + [3, 4]", new Object[] { 4L, 6L });
        checkExpr("[1, 2] - [3, 4]", new Object[] { -2L, -2L });
        checkExpr("[1, 2] * [3, 4]", new Object[] { 3L, 8L });
        checkExpr("[47, 9] / [3, 4]", new Object[] { 15L, 2L });
        checkExpr("[1.5, 2] + [3, 4]", new Object[] { 4.5d, 6.0d });
        checkExpr("[1, 2] - [3, 4]", new Object[] { -2L, -2L });

    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testOtherBinary() {
        checkExpr("true  && true", true);
        checkExpr("true  || true", true);
        checkExpr("true  || false", true);
        checkExpr("false || true", true);
        checkExpr("false && true", false);
        checkExpr("true  && false", false);
        checkExpr("false && false", false);
        checkExpr("false || false", false);

        checkExpr("1 + \"a\"", "1a");
        checkExpr("\"a\" + 1", "a1");
        checkExpr("\"a\" + true", "atrue");

        checkExpr("1 == 1", true);
        checkExpr("1 == 2", false);
        checkExpr("1.0 == 1.0", true);
        checkExpr("1.0 == 2.0", false);
        checkExpr("true == true", true);
        checkExpr("false == false", true);
        checkExpr("true == false", false);
        checkExpr("1 == 1.0", true);
        checkExpr("[1] == [1]", false);

        checkExpr("1 != 1", false);
        checkExpr("1 != 2", true);
        checkExpr("1.0 != 1.0", false);
        checkExpr("1.0 != 2.0", true);
        checkExpr("true != true", false);
        checkExpr("false != false", false);
        checkExpr("true != false", true);
        checkExpr("1 != 1.0", false);

        checkExpr("\"hi\" != \"hi2\"", true);
        checkExpr("[1] != [1]", true);

        // test short circuit
        checkExpr("true || print(\"x\") == \"y\"", true, "");
        checkExpr("false && print(\"x\") == \"y\"", false, "");
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testVarDecl() {
        check("var x: Int = 1; return x", 1L);
        check("var x: Float = 2.0; return x", 2d);

        check("var x: Int = 0; return x = 3", 3L);
        check("var x: String = \"0\"; return x = \"S\"", "S");

        // implicit conversions
        check("var x: Float = 1; x = 2; return x", 2.0d);
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testRootAndBlock() {
        rule = grammar.root;
        check("return", null);
        check("return 1", 1L);
        check("return 1; return 2", 1L);

        check("print(\"a\")", null, "a\n");
        check("print(\"a\" + 1)", null, "a1\n");
        check("print(\"a\"); print(\"b\")", null, "a\nb\n");

        check("{ print(\"a\"); print(\"b\") }", null, "a\nb\n");

        check(
            "var x: Int = 1;" +
                "{ print(\"\" + x); var x: Int = 2; print(\"\" + x) }" +
                "print(\"\" + x)",
            null, "1\n2\n1\n");
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testCalls() {
        check(
            "fun add (a: Int, b: Int): Int { return a + b } " +
                "return add(4, 7)",
            11L);

        HashMap<String, Object> point = new HashMap<>();
        point.put("x", 1L);
        point.put("y", 2L);

        check(
            "struct Point { var x: Int; var y: Int }" +
                "return $Point(1, 2)",
            point);

        check("var str: String = null; return print(str + 1)", "null1", "null1\n");
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testArrayStructAccess() {
        checkExpr("[1][0]", 1L);
        checkExpr("[1.0][0]", 1d);
        checkExpr("[1, 2][1]", 2L);

        // TODO check that this fails (& maybe improve so that it generates a better
        // message?)
        // or change to make it legal (introduce a top type, and make it a top type
        // array if thre
        // is no inference context available)
        // checkExpr("[].length", 0L);
        checkExpr("[1].length", 1L);
        checkExpr("[1, 2].length", 2L);

        checkThrows("var array: Int[] = null; return array[0]", NullPointerException.class);
        checkThrows("var array: Int[] = null; return array.length", NullPointerException.class);

        check("var x: Int[] = [0, 1]; x[0] = 3; return x[0]", 3L);
        checkThrows("var x: Int[] = []; x[0] = 3; return x[0]",
            ArrayIndexOutOfBoundsException.class);
        checkThrows("var x: Int[] = null; x[0] = 3",
            NullPointerException.class);

        check(
            "struct P { var x: Int; var y: Int }" +
                "return $P(1, 2).y",
            2L);

        checkThrows(
            "struct P { var x: Int; var y: Int }" +
                "var p: P = null;" +
                "return p.y",
            NullPointerException.class);

        check(
            "struct P { var x: Int; var y: Int }" +
                "var p: P = $P(1, 2);" +
                "p.y = 42;" +
                "return p.y",
            42L);

        checkThrows(
            "struct P { var x: Int; var y: Int }" +
                "var p: P = null;" +
                "p.y = 42",
            NullPointerException.class);
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testIfWhile() {
        check("if (true) return 1 else return 2", 1L);
        check("if (false) return 1 else return 2", 2L);
        check("if (false) return 1 else if (true) return 2 else return 3 ", 2L);
        check("if (false) return 1 else if (false) return 2 else return 3 ", 3L);

        check("var i: Int = 0; while (i < 3) { print(\"\" + i); i = i + 1 } ", null, "0\n1\n2\n");
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testInference() {
        check("var array: Int[] = []", null);
        check("var array: String[] = []", null);
        check("fun use_array (array: Int[]) {} ; use_array([])", null);
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testTypeAsValues() {
        check("struct S{} ; return \"\"+ S", "S");
        check("struct S{} ; var type: Type = S ; return \"\"+ type", "S");
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testUnconditionalReturn() {
        check("fun f(): Int { if (true) return 1 else return 2 } ; return f()", 1L);
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testSets() {
        check("var x: Int{} = {1, 5, 4}; return x", new Object[] { 1L, 4L, 5L });
        check("var x: Float{} = {1.2, 22.1, 4.4}; return x", new Object[] { 1.2d, 4.4d, 22.1d });
        check("var x: String{} = {\"test\", \"5\", \"aaaa\"}; return x", new Object[] { "5", "aaaa", "test" });

        check("var x: Int{} = {1, 5, 4}; return addSetInt(x, 3)", new Object[] { 1L, 3L, 4L, 5L });
        check("var x: Float{} = {1.2, 22.1, 4.4}; return addSetFloat(x, 4.5)",
            new Object[] { 1.2d, 4.4d, 4.5d, 22.1d });
        check("var x: String{} = {\"test\", \"5\", \"aaaa\"}; return addSetString(x, \"hello\")",
            new Object[] { "5", "aaaa", "hello", "test" });

        check("var x: Int{} = {1, 5, 4}; return containsSetInt(x, 7)", false);
        check("var x: Float{} = {1.0, 22.1, 4.2}; return containsSetFloat(x, 22.1)", true);
        check("var x: String{} = {\"test\", \"5\", \"aaaa\"}; return containsSetString(x, \"wow, containing!\")",
            false);

    }

    // NOTE(norswap): Not incredibly complete, but should cover the basics.

    ///
    ///NEW INTERPRETER TESTS
    @Test public void testClassDeclaration () {
        rule = grammar.root;

        check("pub class Animal{}; return Animal+\"\"", "Animal");
        check("pub class Chat{}; var type: Type = Chat ; return type+\"\"", "Chat");

    }


    @Test public void testClassAttributes()
    {
        rule = grammar.root;

        String input = "" +
            "pub class Dog{\n" +
            "   var name: String\n" +
            "}\n" +
            "var perro: Dog = create Dog()\n" +
            "perro$name = \"Clifford\"\n" +
            "return perro$name";
        check(input, "Clifford");

        input = "" +
            "pub class Dog{\n" +
            "   var name: String\n" +
            "   var age: Int\n" +
            "}\n" +
            "var perro: Dog = create Dog()\n" +
            "perro$name = \"Pelusa\"\n" +
            "perro$age  = 10\n" +
            "return perro$name+ \" a \" +perro$age+ \" ans\"";
        check(input, "Pelusa a 10 ans");

    }


    @Test public void testClassMethCall()
    {
        rule = grammar.root;

        String input = "" +
            "pub class Teacher{\n" +
            "   var name: String\n" +
            "   var age: Int\n" +
            "   fun get_age(): Int {\n" +
            "       return age\n" +
            "   }\n" +
            "   fun set_age(newAge: Int) {\n" +
            "       age = newAge\n" +
            "   }\n" +
            "}" +
            "var profesor: Teacher = create Teacher()\n";

        check(input + "return profesor$age",
            new FieldDeclarationNode(null, "age", new SimpleTypeNode(null, "Int")));
        check(input + "return profesor$get_age()",
            new FieldDeclarationNode(null, "age", new SimpleTypeNode(null, "Int")));


        String input2 = "" +
            "pub class Teacher{\n" +
            "   var name: String\n" +
            "   var age: Int\n" +
            "   fun get_age(): Int {\n" +
            "       return 40\n" +
            "   }\n" +
            "   fun get_name(): String {\n" +
            "       return \"Jean\"\n" +
            "   }\n" +
            "}" +
            "var profesor: Teacher = create Teacher()\n";

        check(input2 + "return profesor$get_name()",
            "Jean");
        check(input2 + "return profesor$get_age()",
            40L);


    }


    @Test public void testClassMethCall2() {
        rule = grammar.root;

        String input = "" +
            "pub class Teacher {\n" +
            "   var age: Int\n" +
            "   var name: String\n" +
            "   var course: String\n" +
            "   fun set_course(newCourse: String) {\n" +
            "       course = newCourse\n" +
            "   }\n" +
            "}\n" +

            "pub class FrenchTeacher {\n" +
            "   var age: Int\n" +
            "   var name: String\n" +
            "   var course: String\n" +
            "   fun get_course(): String {\n" +
            "       return \"Francais\"\n" +
            "   }\n" +
            "}\n" +
            "var prof: Teacher = create Teacher()\n" +
            "var prof_FR: FrenchTeacher = create FrenchTeacher()\n";


        //Not scope for variable course
        checkThrows(input+"" +
                //"profesor$age=5\n"+
                "prof$set_course(\"Francais\")\n"+
                "return prof$course",
            InterpreterException.class);


        //By assigning value to prof$course, it works well
        check(input + ""+
                "prof$course = prof_FR$get_course()\n" +
                "return prof$course",
            "Francais");




    }


    @Test public void testClassInheritance() {
        rule = grammar.root;

        String c_example = "" +
            "pub class Person{\n" +
            "   var name: String\n"+
            "   var age: Int\n"+
            "   fun get_age(): Int {\n" +
            "       return 55\n" +
            "   }\n" +
            "   fun get_name(): String {\n" +
            "       return name\n" +
            "   }\n" +
            "}\n"+

            "pub class Professor{\n" +
            "   var course: String\n"+
            "}\n"+

            "pub class FrenchTeacher from Professor,Person{\n" +
            "   var num_years: Int\n"+
            "}\n"+

            "var p: Person = create Person()\n"+
            "var teacher: Professor = create Professor()\n"+
            "var proFR: FrenchTeacher = create FrenchTeacher()\n"+
            "proFR$name = \"Nick\"\n"+
            "var age: Int = proFR$get_age()\n" +
            "proFR$course = \"Francais\"\n";


        check(c_example + "return proFR$name",
            "Nick");
        check(c_example + "return age",
            55L);
        check(c_example + "return proFR$get_age()",
            55L);
        check(c_example + "return proFR$course",
            "Francais");


        //Null Pointer
        String c_example2 = c_example+
            "var teacher2: Professor = null\n"+
            "return teacher2$course";
        checkThrows(c_example2, NullPointerException.class);


        //teacher object doesn't have a name attribute
        checkThrows(c_example + "return teacher$name",
            AssertionError.class);


    }


    @Test public void testClassPolimorfism() {
        rule = grammar.root;

        String c_example = "" +
            "pub class Operation{\n" +
            "   fun multiplicar(a: Int, b:Int): Int {\n" +
            "       return a*b\n" +
            "   }\n" +
            "   fun multiplicar(a: Int, b:Int, c:Int): Int {\n" +
            "       return a*b*c\n" +
            "   }\n" +
            "   fun multiplicar(a: Int, b:Int, c:Int, d:Int): Int {\n" +
            "       return a*b*c*d\n" +
            "   }\n" +
            "}\n"+
            "var op: Operation = create Operation()\n"
            ;

        check(c_example+"return op$multiplicar(2,3)",
            6L);

        check(c_example+"return op$multiplicar(2,5,4)",
            40L);

        check(c_example+"return op$multiplicar(6,5,2,4)",
            240L);

        //There is no function multiplicar defined with 5 parameters
        checkThrows(c_example+"return op$multiplicar(2,3,5,6,7)",
            AssertionError.class);

    }


    @Test public void testClassPolimorfism2() {
        rule = grammar.root;

        String c_example = "" +
            "pub class Operation{\n" +
            "   fun sumar(a: Int, b:Int): Int {\n" +
            "       return a+b\n" +
            "   }\n" +
            "   fun sumar(a: Float, b:Float): Float {\n" +
            "       return a+b\n" +
            "   }\n" +
            "   fun sumar(a: String, b:String): String {\n" +
            "       return a+\" \"+b\n" +
            "   }\n" +
            "}\n"+
            "var op: Operation = create Operation()\n"
            ;

        check(c_example+"return op$sumar(4,5)",
            9L);

        check(c_example+"return op$sumar(15.5,4.5)",
            20.0);

        check(c_example+"return op$sumar(\"Hablo\",\"espagnol\")",
            "Hablo espagnol");

        //There is no function "sumar" defined with args: Int and String
        checkThrows(c_example+"return op$sumar(2,\"5\")",
            AssertionError.class);

    }


}