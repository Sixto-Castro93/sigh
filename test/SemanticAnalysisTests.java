import norswap.autumn.AutumnTestFixture;
import norswap.autumn.positions.LineMapString;
import norswap.sigh.SemanticAnalysis;
import norswap.sigh.SighGrammar;
import norswap.sigh.ast.SighNode;
import norswap.uranium.Reactor;
import norswap.uranium.UraniumTestFixture;
import norswap.utils.visitors.Walker;
import org.testng.annotations.Test;

/**
 * NOTE(norswap): These tests were derived from the {@link InterpreterTests} and
 * don't test anything
 * more, but show how to idiomatically test semantic analysis. using
 * {@link UraniumTestFixture}.
 */
public final class SemanticAnalysisTests extends UraniumTestFixture {
        // ---------------------------------------------------------------------------------------------

        private final SighGrammar grammar = new SighGrammar();
        private final AutumnTestFixture autumnFixture = new AutumnTestFixture();

        {
                autumnFixture.rule = grammar.root();
                autumnFixture.runTwice = false;
                autumnFixture.bottomClass = this.getClass();
        }

        private String input;

        @Override
        protected Object parse(String input) {
                this.input = input;
                return autumnFixture.success(input).topValue();
        }

        @Override
        protected String astNodeToString(Object ast) {
                LineMapString map = new LineMapString("<test>", input);
                return ast.toString() + " (" + ((SighNode) ast).span.startString(map) + ")";
        }

        // ---------------------------------------------------------------------------------------------

        @Override
        protected void configureSemanticAnalysis(Reactor reactor, Object ast) {
                Walker<SighNode> walker = SemanticAnalysis.createWalker(reactor);
                walker.walk(((SighNode) ast));
        }

        // ---------------------------------------------------------------------------------------------

        @Test
        public void testLiteralsAndUnary() {
                successInput("return 42");
                successInput("return 42.0");
                successInput("return \"hello\"");
                successInput("return (42)");
                successInput("return [1, 2, 3]");
                successInput("return true");
                successInput("return false");
                successInput("return null");
                successInput("return !false");
                successInput("return !true");
                successInput("return !!true");
        }

        // ---------------------------------------------------------------------------------------------

        @Test
        public void testNumericBinary() {
                successInput("return 1 + 2");
                successInput("return 2 - 1");
                successInput("return 2 * 3");
                successInput("return 2 / 3");
                successInput("return 3 / 2");
                successInput("return 2 % 3");
                successInput("return 3 % 2");

                successInput("return 1.0 + 2.0");
                successInput("return 2.0 - 1.0");
                successInput("return 2.0 * 3.0");
                successInput("return 2.0 / 3.0");
                successInput("return 3.0 / 2.0");
                successInput("return 2.0 % 3.0");
                successInput("return 3.0 % 2.0");

                successInput("return 1 + 2.0");
                successInput("return 2 - 1.0");
                successInput("return 2 * 3.0");
                successInput("return 2 / 3.0");
                successInput("return 3 / 2.0");
                successInput("return 2 % 3.0");
                successInput("return 3 % 2.0");

                successInput("return 1.0 + 2");
                successInput("return 2.0 - 1");
                successInput("return 2.0 * 3");
                successInput("return 2.0 / 3");
                successInput("return 3.0 / 2");
                successInput("return 2.0 % 3");
                successInput("return 3.0 % 2");

                // tests for array programming
                successInput("var x: Int[] = [1, 2]; var y: Int[] = [3, 4]; return x+y");
                successInput("var x: Int[] = [1, 2]; var y: Int[] = [3, 4]; return x-y");
                successInput("var x: Int[] = [1, 2]; var y: Int[] = [3, 4]; return x*y");
                successInput("var x: Int[] = [1, 2]; var y: Int[] = [3, 4]; return x/y");
                successInput("var x: Int[] = [1, 2]; var y: Int = 3; return x+y");
                successInput("var x: Int[] = [1, 2]; var y: Int = 3; return x-y");
                successInput("var x: Int[] = [1, 2]; var y: Int = 3; return x*y");
                successInput("var x: Int[] = [1, 2]; var y: Int = 3; return x/y");

                failureInputWith("var x: Int[] = [1, 2]; var y: String[] = []; return x-y",
                    "Trying to subtract Int with String[]");

                failureInputWith("return 2 + true", "Trying to add Int with Bool");
                failureInputWith("return true + 2", "Trying to add Bool with Int");
                // failureInputWith("return 2 + [1]", "Trying to add Int with Int[]");
                // failureInputWith("return [1] + 2", "Trying to add Int[] with Int");
                // Those inputs are now accepted and return an Int[] of value [3]
        }

        // ---------------------------------------------------------------------------------------------

        @Test
        public void testOtherBinary() {
                successInput("return true && false");
                successInput("return false && true");
                successInput("return true && true");
                successInput("return true || false");
                successInput("return false || true");
                successInput("return false || false");

                failureInputWith("return false || 1",
                    "Attempting to perform binary logic on non-boolean type: Int");
                failureInputWith("return 2 || true",
                    "Attempting to perform binary logic on non-boolean type: Int");

                successInput("return 1 + \"a\"");
                successInput("return \"a\" + 1");
                successInput("return \"a\" + true");

                successInput("return 1 == 1");
                successInput("return 1 == 2");
                successInput("return 1.0 == 1.0");
                successInput("return 1.0 == 2.0");
                successInput("return true == true");
                successInput("return false == false");
                successInput("return true == false");
                successInput("return 1 == 1.0");

                failureInputWith("return true == 1", "Trying to compare incomparable types Bool and Int");
                failureInputWith("return 2 == false", "Trying to compare incomparable types Int and Bool");

                successInput("return \"hi\" == \"hi\"");
                successInput("return [1] == [1]");

                successInput("return 1 != 1");
                successInput("return 1 != 2");
                successInput("return 1.0 != 1.0");
                successInput("return 1.0 != 2.0");
                successInput("return true != true");
                successInput("return false != false");
                successInput("return true != false");
                successInput("return 1 != 1.0");

                failureInputWith("return true != 1", "Trying to compare incomparable types Bool and Int");
                failureInputWith("return 2 != false", "Trying to compare incomparable types Int and Bool");

                successInput("return \"hi\" != \"hi\"");
                successInput("return [1] != [1]");

        }

        // ---------------------------------------------------------------------------------------------

        @Test
        public void testVarDecl() {
                successInput("var x: Int = 1; return x");
                successInput("var x: Float = 2.0; return x");

                successInput("var x: Int = 0; return x = 3");
                successInput("var x: String = \"0\"; return x = \"S\"");

                failureInputWith("var x: Int = true", "expected Int but got Bool");
                failureInputWith("return x + 1", "Could not resolve: x");
                failureInputWith("return x + 1; var x: Int = 2", "Variable used before declaration: x");

                // implicit conversions
                successInput("var x: Float = 1 ; x = 2");
        }

        // ---------------------------------------------------------------------------------------------

        @Test
        public void testRootAndBlock() {
                successInput("return");
                successInput("return 1");
                successInput("return 1; return 2");

                successInput("print(\"a\")");
                successInput("print(\"a\" + 1)");
                successInput("print(\"a\"); print(\"b\")");

                successInput("{ print(\"a\"); print(\"b\") }");

                successInput(
                    "var x: Int = 1;" +
                        "{ print(\"\" + x); var x: Int = 2; print(\"\" + x) }" +
                        "print(\"\" + x)");
        }

        // ---------------------------------------------------------------------------------------------

        @Test
        public void testCalls() {
                successInput(
                    "fun add (a: Int, b: Int): Int { return a + b } " +
                        "return add(4, 7)");

                successInput("template <T: Type> fun add (a: T, b: T): T { return a + b } ");

                // successInput("template <T: Type> fun add (a: T, b: T): T { return a } " +
                // "return add<Float>(2.4, 2.5)");

                // successInput("template <T1: Type, T2: Type> fun add (a: T1, b: T2): T1 {
                // return a } " +
                // "return add<Float, Int>(2.5, 5)");
                // Doesn't work
                successInput(
                    "struct Point { var x: Int; var y: Int }" +
                        "return $Point(1, 2)");

                successInput("var str: String = null; return print(str + 1)");

                //failureInputWith("return print(1)", "argument 0: expected String but got Int");
        }

        // ---------------------------------------------------------------------------------------------

        @Test
        public void testArrayStructAccess() {
                successInput("return [1][0]");
                successInput("return [1.0][0]");
                successInput("return [1, 2][1]");

                failureInputWith("return [1][true]", "Indexing an array using a non-Int-valued expression");

                // TODO make this legal?
                // successInput("[].length", 0L);

                successInput("return [1].length");
                successInput("return [1, 2].length");

                successInput("var array: Int[] = null; return array[0]");
                successInput("var array: Int[] = null; return array.length");

                successInput("var x: Int[] = [0, 1]; x[0] = 3; return x[0]");
                successInput("var x: Int[] = []; x[0] = 3; return x[0]");
                successInput("var x: Int[] = null; x[0] = 3");
                successInput("var x: Int{} = {}; return x");

                successInput(
                    "struct P { var x: Int; var y: Int }" +
                        "return $P(1, 2).y");

                successInput(
                    "struct P { var x: Int; var y: Int }" +
                        "var p: P = null;" +
                        "return p.y");

                successInput(
                    "struct P { var x: Int; var y: Int }" +
                        "var p: P = $P(1, 2);" +
                        "p.y = 42;" +
                        "return p.y");

                successInput(
                    "struct P { var x: Int; var y: Int }" +
                        "var p: P = null;" +
                        "p.y = 42");

                /*failureInputWith(
                                "struct P { var x: Int; var y: Int }" +
                                                "return $P(1, true)",
                                "argument 1: expected Int but got Bool");

                failureInputWith(
                                "struct P { var x: Int; var y: Int }" +
                                                "return $P(1, 2).z",
                                "Trying to access missing field z on struct P");*/
        }

        // ---------------------------------------------------------------------------------------------

        @Test
        public void testIfWhile() {
                successInput("if (true) return 1 else return 2");
                successInput("if (false) return 1 else return 2");
                successInput("if (false) return 1 else if (true) return 2 else return 3 ");
                successInput("if (false) return 1 else if (false) return 2 else return 3 ");

                successInput("var i: Int = 0; while (i < 3) { print(\"\" + i); i = i + 1 } ");

                failureInputWith("if 1 return 1",
                    "If statement with a non-boolean condition of type: Int");
                failureInputWith("while 1 return 1",
                    "While statement with a non-boolean condition of type: Int");
        }

        // ---------------------------------------------------------------------------------------------

        @Test
        public void testInference() {
                successInput("var array: Int[] = []");
                successInput("var array: String[] = []");
                successInput("fun use_array (array: Int[]) {} ; use_array([])");
        }

        // ---------------------------------------------------------------------------------------------

        @Test
        public void testTypeAsValues() {
                successInput("struct S{} ; return \"\"+ S");
                successInput("struct S{} ; var type: Type = S ; return \"\"+ type");
        }

        // ---------------------------------------------------------------------------------------------

        @Test
        public void testUnconditionalReturn() {
                successInput("fun f(): Int { if (true) return 1 else return 2 } ; return f()");

                // TODO: would be nice if this pinpointed the if-statement as missing the
                // return,
                // not the whole function declaration
                failureInputWith("fun f(): Int { if (true) return 1 } ; return f()",
                    "Missing return in function");
        }

        // ---------------------------------------------------------------------------------------------

        @Test
        public void testSets() {
                successInput("var x: Int{} = {1, 5, 4}; return x");
                successInput("var x: Float{} = {1.0, 5, 4.2}; return x");
                successInput("var x: String{} = {\"test\", \"5\", \"aaaa\"}; return x");

                successInput("var x: Int{} = {1, 5, 4}; return addSetInt(x, 6)");
                successInput("var x: Float{} = {1.0, 5, 4.2}; return addSetFloat(x, 6.8)");
                successInput("var x: String{} = {\"test\", \"5\", \"aaaa\"}; return addSetString(x, \"wow, adding!\")");

                successInput("var x: Int{} = {1, 5, 4}; return containsSetInt(x, 7)");
                successInput("var x: Float{} = {1.0, 5, 4.2}; return containsSetFloat(x, 6.88)");
                successInput("var x: String{} = {\"test\", \"5\", \"aaaa\"}; return containsSetString(x, \"wow, containing!\")");

                failureInputWith("var x: String{} = {\"1\", \"5\", 4.2}; return x",
                    "supertype");

                successInput("var x: Int{} = {}; return addSetInt(x, 7)");
                successInput("return addSetInt({2, 3}, 7)");
                failureInput("var x: Int{} = {}; return addSetString(x, \"7\")");
        }

        ///
        ///NEW SEMANTIC ANALYSIS TESTS

        @Test public void testFunctionDeclaration() {
                successInput("pub fun test1(a: Int){" +
                    "}");
                successInput("pvt fun doubleNumber(a: Int): Int {" +
                    "return a*2" +
                    "}");

                successInput("pub fun test2(tab: Int[]): Int {return tab[0]}");
                successInput("pub fun sum(a: Int, b: Int): Int { return a+b } ");
                successInput("pub fun main(){" +
                    "pub fun sum(a: Int, b: Int): Int{" +
                    "return a+b}" +
                    "sum(2,5);"+
                    "}");


        }


        @Test public void testClassDeclaration(){
                successInput("pvt class Example{}");

                String c_example = "" +
                    "pub class Dog{\n" +
                    "   var age: Int = 5\n"+
                    "   pvt fun get_age(): String {\n" +
                    "       return age\n" +
                    "   }\n" +
                    "}\n";

                successInput(c_example);


                successInput("pub class Example{" +
                    "pvt fun duplicateNumber (x: Int): Int { return x*2 }" +
                    "}");


                successInput("pub class Example{" +
                    "pvt class SubExample{" +
                    "print(\"Salut, Bonjour\")}" +
                    "}");

                //Teacher, Person classes not defined
                failureInput("pub class FrenchTeacher from Teacher,Person{}" +
                    "var prof: FrenchTeacher = create Teacher()");


        }


        @Test public void testClassObjCreation(){
                String c_example = "" +
                    "pub class Course{\n" +
                    "   var name: String\n"+
                    "   var hours: Int\n"+
                    "   fun get_hours(): Int {\n" +
                    "       return hours\n" +
                    "}\n"+
                    "}\n"+
                    "var curso: Course = create Course()\n"
                    ;
                successInput(c_example);


                String c_example2 = c_example +""+
                    "var curso2: Course = create Course(10)\n"//Wrong parameter
                    ;
                failureInput(c_example2);



        }


        @Test public void testClassMethodReturn(){
                String c_example = "" +
                    "pub class Course{\n" +
                    "   var name: String\n"+
                    "   var hours: Int\n"+
                    "   fun get_hours(): String {\n" +
                    "       return hours\n" +
                    "}\n"+
                    "}\n"
                    ;
                failureInputWith(c_example, "Incompatible return type");

        }



        @Test public void testClassParamCall(){
                String c_example = "" +
                    "pub class Course{\n" +
                    "   var name: String\n"+
                    "   var hours: Int\n"+
                    "   fun get_hours(): Int {\n" +
                    "       return hours\n" +
                    "   }\n" +
                    "}\n"+
                    "var course: Course = create Course()\n"+
                    "course$hours=100\n"+
                    "course$name=\"Lang&Translators\"\n"
                    ;

                successInput(c_example);

        }


        @Test public void testFailureClassParam(){
                //Failed Scenario
                String c_example = "" +
                    "pub class Course{\n" +
                    "   var name: String\n"+
                    "   var hours: Int\n"+
                    "   fun get_hours(): Int {\n" +
                    "       return hours\n" +
                    "   }\n" +
                    "}\n";
                ;

                failureInput(c_example+ "" +
                    "var curso: Course = create Course()\n"+
                    "var horas: Int = curso.hours\n"// Wrong way to access a parameter of Class type. It should be with '$'
                );


                String c_example2 = c_example+ "" +
                    "var curso2: Course = create Course()\n"+
                    "curso2$hours=\"ML\"\n"// Incorrect assignment: Receives String instead of Int type
                    ;

                failureInput(c_example2);


        }


        @Test public void testClassMethodsCall(){
                String c_example = "" +
                    "pub class Dog{\n" +
                    "   var age: Int\n"+
                    "   fun get_age(): Int {\n" +
                    "       return age\n" +
                    "   }\n" +
                    "}\n"+
                    "var perro: Dog = create Dog()\n"
                    +"var edad: Int = perro$get_age()"
                    ;
                successInput(c_example);


                String c_example2 = "" +
                    "pub class Dog{\n" +
                    "   var name: String\n"+
                    "   var age: Int\n"+
                    "   fun get_age(): Int {\n" +
                    "       return age\n" +
                    "   }\n" +
                    "   fun get_name(): String {\n" +
                    "       return name\n" +
                    "   }\n" +
                    "   fun set_age(new_age: Int) {\n" +
                    "       age=new_age\n" +
                    "   }\n" +
                    "   fun set_name(dog_name: String) {\n" +
                    "       name=dog_name\n" +
                    "   }\n" +
                    "}\n"+
                    "var perro: Dog = create Dog()\n"+
                    "perro$set_age(9)\n"+
                    "perro$set_name(\"Clifford\")\n"+
                    "var nombre: String = perro$get_name()\n"+
                    "var edad: Int = perro$get_age()\n"
                    ;

                successInput(c_example2);

        }


        @Test public void testFailureClassMethodCall(){
                String c_example = "" +
                    "pub class Dog{\n" +
                    "   var age: Int\n"+
                    "   fun get_age(): Int {\n" +
                    "       return age\n" +
                    "   }\n" +
                    "   fun set_age(new_age: Int) {\n" +
                    "       age=new_age\n" +
                    "   }\n" +
                    "}\n"+
                    "var perro: Dog = create Dog()\n"+
                    "var edad: String = perro$get_age()\n"//Not compatible var assignment, it received string instead of Int
                    ;

                failureInput(c_example);


                String c_example2 = "" +
                    "pub class Dog{\n" +
                    "   var name: String\n"+
                    "   fun get_name(): String {\n" +
                    "       return name\n" +
                    "   }\n" +
                    "   fun set_name(dog_name: String) {\n" +
                    "       name=dog_name\n" +
                    "   }\n" +
                    "}\n"+
                    "var perro: Dog = create Dog()\n"+
                    "perro$set_name(9)\n"//Wrong argument type. It should be a string value
                    ;

                failureInput(c_example2);


                String c_example3 = "" +
                    "pub class Dog{\n" +
                    "   var name: String=\"Pelusa\"\n"+
                    "   fun set_name(dog_name: String) {\n" +
                    "       name=dog_name\n" +
                    "   }\n" +
                    "}\n"+
                    "var perro: Dog = create Dog()\n"+
                    "perro$set_age(0)\n"//Not existing method
                    ;

                failureInput(c_example3);

        }



        @Test public void testClassInheritance(){
                String c_example = "" +
                    "pub class Animal{\n" +
                    "   var age: Int\n"+
                    "   fun get_age(): Int {\n" +
                    "       return 5\n" +
                    "   }\n" +
                    "}\n"+
                    "var ani: Animal = create Animal()\n"+
                    "ani$age=8\n"+
                    ""+
                    "pub class Dog from Animal{\n" +
                    "   var name: String\n"+
                    "}\n"+
                    "var perro: Dog = create Dog()\n"+
                    "perro$age=9\n"+
                    "var edad: Int=perro$get_age()\n"+
                    "perro$name=\"Pluto\"\n"
                    ;
                successInput(c_example);


        }


        @Test public void testClassDoubleInheritance(){
                String c_example = "" +
                    "pub class Person{\n" +
                    "   var name: String\n"+
                    "   var age: Int\n"+
                    "   fun get_age(): Int {\n" +
                    "       return 55\n" +
                    "   }\n" +
                    "}\n"+
                    "var p: Person = create Person()\n"+

                    "pub class Professor{\n" +
                    "   var course: String\n"+
                    "}\n"+

                    "pub class FrenchTeacher from Professor,Person{\n" +
                    "   var num_years: Int\n"+
                    "}\n"+

                    "var teacher: Professor = create Professor()\n"+
                    "var proFR: FrenchTeacher = create FrenchTeacher()\n"+
                    "var age: Int = proFR$get_age()\n" +
                    "proFR$name = \"Nick\"\n"+
                    "proFR$age = 39\n"+
                    "proFR$course = \"Francais\"\n"
                    ;
                successInput(c_example);

        }


        @Test public void testClassFailedInheritance(){
                String c_example = "" +
                    "pub class Vehicule{\n" +
                    "   var brand: String\n"+
                    "   fun get_brand(): String {\n" +
                    "       return brand\n" +
                    "   }\n" +
                    "}\n"+

                    "pub class Car from Vehicule{\n" +
                    "   var modelName: String\n"+
                    "   var year: Int\n"+
                    "}\n"+

                    "var veh: Vehicule = create Vehicule()\n"+
                    "var carro: Car = create Car()\n"+

                    "carro$modelName = \"Grand Vitara AZ\"\n"+ //OK
                    "carro$brand=\"Chevrolet\""+ //OK, accessing attribute by inheritance
                    "veh$modelName=\"Mercedes\"\n" //A circular Inheritance is not possible, It's just like always one way (inheritance: Car->Vehicule)
                    ;

                failureInput(c_example);//Only valid: Car -> Vehicule, not the other way (Vehicule -> Car)

        }

        //Polymorphism with more or less argument types (same type)
        @Test public void testPolymorphism(){
                //Polimorfism
                successInput("pub fun sum(a: Int, b: Int): Int { " +
                    "return a+b " +
                    "}" +
                    "pub fun sum(a: Int, b: Int, c: Int): Int {" +
                    " return a+b+c " +
                    "}");


                String c_example2 = "" +
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
                    +"op$multiplicar(2,3)\n"
                    +"op$multiplicar(2,3,4)\n"
                    +"var result: Int = op$multiplicar(2,3,4,5)\n";

                successInput(c_example2);


                //Wrong argument type: Float instead of Int
                String c_example3 = c_example2+
                    "var op: Operation = create Operation()\n"
                    +"op$multiplicar(2,3,4.0,5.5)\n";
                failureInput(c_example3);


                //Wrong argument type: String instead of Int
                String c_example4 = c_example2+
                    "var op2: Operation = create Operation()\n"
                    +"var total: Int = op2$multiply(2,3,4,5,\"6\")";

                failureInput(c_example4);


                //This case fails because there is no function "multiplicar" with 5 args
                String c_example5 = c_example2+
                    "var op3: Operation = create Operation()\n"
                    +"var total: Int = op3$multiply(2,3,4,5,6)";

                failureInput(c_example5);


        }


        //Polymorphism: Different argument Types
        @Test public void testPolymorphism2(){
                //Polimorfism
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
                    +"op$sumar(2,3)\n"
                    +"op$sumar(2.4,5.1)\n"
                    +"var result: String = op$sumar(\"Salut\",\"Bonjour\")\n";

                successInput(c_example);

                //This case fails because there is no any function "sumar" with arg Int and arg String
                String c_example5 = c_example+
                    "var op3: Operation = create Operation()\n"
                    +"var total: Int = op3$sumar(2,\"6\")";

                failureInput(c_example5);

        }


}
