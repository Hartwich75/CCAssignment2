import java.util.HashMap;
import java.util.Map.Entry;
import java.util.List;
import java.util.ArrayList;

public abstract class AST{
    public void error(String msg){
	System.err.println(msg);
	System.exit(-1);
    }
};

/* Expressions are similar to arithmetic expressions in the impl
   language: the atomic expressions are just Signal (similar to
   variables in expressions) and they can be composed to larger
   expressions with And (Conjunction), Or (Disjunction), and Not
   (Negation). Moreover, an expression can be using any of the
   functions defined in the definitions. */

abstract class Expr extends AST{

    public abstract boolean eval (Environment env);

}

/**
 * Conjunction of two expressions
 * Example: Signal1 * Signal2
 */
class Conjunction extends Expr{
    Expr e1, e2;

    Conjunction(Expr e1, Expr e2) {
        this.e1 = e1;
        this.e2 = e2;
    }

    @Override
    public boolean eval(Environment env) {
        return e1.eval(env) && e2.eval(env);
    }
}

/**
 * Disjunction of two expressions
 * Example: Signal1 + Signal2
 */
class Disjunction extends Expr{
    Expr e1,e2;

    Disjunction(Expr e1,Expr e2){
        this.e1=e1;
        this.e2=e2;
    }

    @Override
    public boolean eval(Environment env) {
        return e1.eval(env) || e2.eval(env);
    }
}

/**
 * Negation (NOT) of an expression
 * Example: /Signal
 * Eval returns true when the expression is false and vice versa
 */
class Negation extends Expr{
    // Example: /Signal
    Expr e;
    Negation(Expr e){
        this.e=e;
    }

    @Override
    public boolean eval(Environment env) {
        return !e.eval(env);
    }
}

/**
 * Use of a defined function
 */
class UseDef extends Expr{
    // Using any of the functions defined by "def"
    // e.g. xor(Signal1,/Signal2) 
    String f;  // the name of the function, e.g. "xor" 
    List<Expr> args;  // arguments, e.g. [Signal1, /Signal2]
    UseDef(String f, List<Expr> args){
	this.f = f;
    this.args = args;
    }

    /**
     *
     * @param env environment to evaluate the function in
     * @return true if the function was evaluated successfully, false otherwise
     */
    @Override
    public boolean eval(Environment env) {
        // Retrieve the function definition
        Def functionDef = env.getDef(f);
        if (functionDef == null) {
            error("Function " + f + " is not defined.");
            return false; // This line is unreachable due to error(), but required for compilation
        }

        // Create a new duplicated environment for the function
        Environment functionEnv = env.createScope();

        // Bind function arguments to actual parameter values
        List<String> paramNames = functionDef.args;
        if (paramNames.size() != args.size()) {
            error("Argument count mismatch for function " + f);
        }

        for (int i = 0; i < paramNames.size(); i++) {
            Boolean argValue = args.get(i).eval(env); // Evaluate the actual parameter
            functionEnv.setSignalValue(paramNames.get(i), argValue); // Bind argument in the new environment
        }

        // Evaluate the function body in the new environment
        return functionDef.e.eval(functionEnv);
    }
}

class Signal extends Expr{
    String varname; // a signal is just identified by a name 
    Signal(String varname){
        this.varname = varname;
    }

    @Override
    public boolean eval(Environment env) {
        Boolean value = env.getSignalValue(varname);
        if (value == null) {
            error("Signal " + varname + " is not defined.");
            return false; // Unreachable, as error exits, but needed for compilation
        }
        return value;
    }
}

class Def extends AST{
    // Definition of a function
    // Example: def xor(A,B) = A * /B + /A * B
    String f; // function name, e.g. "xor"
    List<String> args;  // formal arguments, e.g. [A,B]
    Expr e;  // body of the definition, e.g. A * /B + /A * B
    Def(String f, List<String> args, Expr e){
	this.f=f; this.args=args; this.e=e;
    }
}

// An Update is any of the lines " signal = expression "
// in the update section

class Update extends AST{
    // Example Signal1 = /Signal2 
    String name;  // Signal being updated, e.g. "Signal1"
    Expr e;  // The value it receives, e.g., "/Signal2"
    Update(String name, Expr e){
        this.e=e; this.name=name;
    }

    public void eval(Environment env){
        Boolean result = e.eval(env);
        env.setSignalValue(name, result);
        //System.out.printf("%s = %d\n", name, result ? 1 : 0);
    }

}

/* A Trace is a signal and an array of Booleans, for instance each
   line of the .simulate section that specifies the traces for the
   input signals of the circuit. It is suggested to use this class
   also for the output signals of the circuit in the second
   assignment.
*/

class Trace extends AST{
    // Example Signal = 0101010
    String signal;
    Boolean[] values;
    Trace(String signal, Boolean[] values){
	this.signal=signal;
	this.values=values;
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(signal + " = ");
        for (Boolean value : values) {
            sb.append(value ? "1" : "0");
        }
        return sb.toString();
    }

}

/* The main data structure of this simulator: the entire circuit with
   its inputs, outputs, latches, definitions and updates. Additionally
   for each input signal, it has a Trace as simulation input.
   
   There are two variables that are not part of the abstract syntax
   and thus not initialized by the constructor (so far): simoutputs
   and simlength. It is suggested to use these two variables for
   assignment 2 as follows: 
 
   1. all siminputs should have the same length (this is part of the
   checks that you should implement). set simlength to this length: it
   is the number of simulation cycles that the interpreter should run.

   2. use the simoutputs to store the value of the output signals in
   each simulation cycle, so they can be displayed at the end. These
   traces should also finally have the length simlength.
*/

class Circuit extends AST{
    String name;  
    List<String> inputs; 
    List<String> outputs;
    List<String>  latches;
    List<Def> definitions;
    List<Update> updates;
    List<Trace>  siminputs;
    List<Trace>  simoutputs;
    int simlength;
    Circuit(String name,
	    List<String> inputs,
	    List<String> outputs,
	    List<String>  latches,
	    List<Def> definitions,
	    List<Update> updates,
	    List<Trace>  siminputs){
	this.name=name;
	this.inputs=inputs;
	this.outputs=outputs;
	this.latches=latches;
	this.definitions=definitions;
	this.updates=updates;
	this.siminputs=siminputs;
    this.simlength = siminputs.getFirst().values.length;
    }

    public void latchesInit(Environment env){
        for (String latch : latches) {
            env.setSignalValue(latch + "'", false);
        }
    }

    public void latchesUpdate(Environment env) {
        for (String latch : latches) {
            Boolean value = env.getSignalValue(latch);
            env.setSignalValue(latch + "'", value); // Set latch output to current input value
        }
    }

    public void initialize(Environment env) {
        // Add definitions to the environment
        for (Def def : definitions) {
            env.addFunctionDefinition(def.f, def);
        }

        // Initialize simoutputs for inputs and outputs
        simoutputs = new ArrayList<>();
        for (String input : inputs) {
            Trace trace = new Trace(input, new Boolean[simlength]);
            simoutputs.add(trace);
        }
        for (String output : outputs) {
            Trace trace = new Trace(output, new Boolean[simlength]);
            simoutputs.add(trace);
        }

        // Initialize input signals from siminputs
        for (Trace inputTrace : siminputs) {
            Boolean[] values = inputTrace.values;
            if (values.length == 0) {
                error("Input signal " + inputTrace.signal + " has no values in siminputs.");
            }
            env.setSignalValue(inputTrace.signal, values[0]);
        }

        // Initialize latches
        latchesInit(env);

        // Perform the first cycle of updates
        for (Update update : updates) {
            update.eval(env);
        }

        // Store the first cycle values in simoutputs
        storeTraces(env, 0);
    }

    public void nextCycle(Environment env, int cycle) {
        // Update input signals for the current cycle
        for (Trace inputTrace : siminputs) {
            Boolean[] values = inputTrace.values;
            if (cycle >= values.length) {
                error("Input signal " + inputTrace.signal + " is undefined at cycle " + cycle);
            }
            env.setSignalValue(inputTrace.signal, values[cycle]);
        }

        // Update latches
        latchesUpdate(env);

        // Perform updates for all signals
        for (Update update : updates) {
            update.eval(env);
        }

        // Store signal values in simoutputs for this cycle
        storeTraces(env, cycle);
    }
    private void storeTraces(Environment env, int cycle) {
        for (Trace trace : simoutputs) {
            Boolean value = env.getSignalValue(trace.signal);
            if (value == null) {
                error("Signal " + trace.signal + " is not defined in the environment.");
            }
            trace.values[cycle] = value;
        }
    }

    private void printTraces() {
        for (Trace trace : simoutputs) {
            StringBuilder sb = new StringBuilder();
            for (Boolean value : trace.values) {
                sb.append(value ? "1" : "0");
            }
            sb.append(" ").append(trace.signal);
            System.out.println(sb.toString());
        }
    }

    public void runSimulator(Environment env) {
        initialize(env);

        for (int i = 1; i < simlength; i++) {
            nextCycle(env, i);
            System.out.printf("Clock Cycle: %d\n", i);
            System.out.println(env.toString());
        }

        // Print the final formatted traces
        printTraces();
    }

}
