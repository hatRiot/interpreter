package src;

import java.util.*;
import java.io.*;

/*
  The parser is called after lexical analysis, and forms expressions/statements
  out of our tokenized source code.  The final step after this is interpreting
  statements and executing them.  
 */
public class Parser
{
	// this is the list of tokenized...tokens
	ArrayList<Token> tokens;
	
	// this will be set to the position of the main function once
	// parsing is over.  If it's -1, then we don't have a main function
	// and we need to get out!
	int main = -1;

	// this is our current position
	int pos = 0;

	// this is what atomics are put together for
	public ArrayList<Expression> expressions = new ArrayList<Expression>();

	// this is what expressions are executed into
	public ArrayList<Statement> statements = new ArrayList<Statement>();

	// this is a map of our variables, name -> Value.
	// NOTE: Every time something is put/get from this, a toLowerCase should be called.
	// This is because variables are case INsensitive.
	public HashMap<String, Value> variables = new HashMap<String, Value>();

	// this is a map of our labels; label name -> size of stmts
	public HashMap<String, Integer> labels = new HashMap<String, Integer>();

	// map of our functions; func -> position in stmts array
	public HashMap<String, Function> functions = new HashMap<String, Function>();

	// list of all our possible operators
	private ArrayList<Character> operators = new ArrayList<Character>(
			Arrays.asList('(', ')', '+', '-', '*', '%', '/', '=',
						  '<', '>'));

	// when we do function calls, we push the current position onto a stack.
	// This way if we have nested function calls we can unwind the stack properly.
	private Stack<Integer> lastPosition = new Stack<Integer>();

	public Parser ( ArrayList<Token> tmp )
	{
		// initialize the local tokens list with 
		// the passed token list
		this.tokens = tmp;
		
		// build me function templates
		templateFunctions();
		
		try
		{
			// start parsing the list
			parse();
		}
		catch(Exception e)
		{
			System.err.println ( "nectj: You did it wrong!" );
			e.printStackTrace();
		}
		
		//System.out.println ( "\nExecution:\n");
		// 
		// If a main wasn't found while parsing, then don't even execute
		//
		if ( -1 == main ){
			System.out.println ( "nectj: No main found!" );
			return;
		}
		
		// set the current position to the main statement, no matter
		// where it's at in the code
		pos = main;

		//
		// execute the statements!  This needs to be done this way because of the
		// jumps in execution; i.e. if and return and stuff
		//
		while ( pos < tokens.size())
		{
			//printVars();
			int current = pos;
			if ( current >= statements.size() )
				break;
				
			pos++;
			statements.get(current).execute();
		}
	}
	
	private void printVars()
	{
		System.out.println ( " *** VARS");
		Iterator it = variables.entrySet().iterator();
		while ( it.hasNext())
		{
			Map.Entry pairs = (Map.Entry)it.next();
			System.out.println ( pairs.getKey() + " = " + pairs.getValue() );
			//it.remove();
		}
	}
	// 
	// I realized my previous design would break if main() was the first function seen and
	// made calls to other functions, so i had to implement a really hacky workaround.  Basically i 
	// go and look for functions BEFORE parsing, that way i have the templates set up.  When I do, then,
	// see a function, I just need to update its position.  And the calls in main() to methods up aways
	// get their calls updated when i run across the function later on.  So there.
	//
	private void templateFunctions()
	{
		for ( int i = 0; i < tokens.size(); ++i )
		{
			if ( tokens.get(i).getTag() == TokenTag.FUNC )
			{
				Function function;
				function = new Function ( tokens.get(i+1).getValue(),
												-1);
				i++; // shoot over the (
				while ( tokens.get(i).getTag() == TokenTag.VARIABLE)
				{
					function.addVar ( tokens.get(i++) );
				}
				functions.put ( function.getName(), function );
				i++; // shoot over the )
			}
		}
	}
	// this method starts parsing the tokens, and doesn't 
	// stop until I declare it so
	private void parse()
		throws Exception
	{
		// parse until we EOF
		while(pos < tokens.size())
		{
			if (match(TokenTag.WRITE))
			{
				statements.add( new Print ( op() ) );
			}
			else if ( match(TokenTag.READ))
			{
				// consume the opening para
				consume(TokenTag.LPARA);
				// create the read with the variable to store it in
				statements.add ( new Input ( consume(TokenTag.VARIABLE).getValue()));
				// consume the trailing para
				consume(TokenTag.RPARA);
			}
			else if ( match(TokenTag.VARIABLE, TokenTag.ASSIGN))
			{
				// this is the variable name
				String tmp = lookBack(2).getValue();
				// this builds the value to store there
				Expression value = op();
				// if the value is a variable, it could be a func call
				if ( value instanceof Variable ){
					Variable v = (Variable)value;
					if ( functions.containsKey(v.getVar())){
						// it's a func call, match it!
						matchFunctionCall();
					}
				}
				// add the statement and it's value (exp or func)
				statements.add ( new Assignment(tmp, value) );
			}
			else if ( match(TokenTag.VARIABLE, TokenTag.LPARA))
			{
				// match the function call
				matchFunctionCall();
			}
			else if ( match(TokenTag.LABEL))
			{
				// set a flag for our pos
				labels.put(lookBack(1).getValue().toLowerCase(), statements.size());
			}
			else if ( match(TokenTag.IF))
			{
				Expression condition = op();
				consume(TokenTag.TO);
				String tmp = consume(TokenTag.VARIABLE).getValue();
				statements.add(new IfStatement(condition, tmp.toLowerCase()));
			}
			else if ( match(TokenTag.FUNC) )
			{
				// if we already have the 'template' for the function, then we really just need to
				// update its position
				if ( functions.containsKey(consume(TokenTag.FUNC).getValue()))
				{
					consume(TokenTag.LPARA);
					// get the function template
					Function f = functions.get(lookBack(2).getValue());
					// set the position of it
					f.setPosition ( statements.size() );
					// update
					functions.put(f.getName(), f );
					// update main pos
					if ( f.getName().equals("main"))
						main = statements.size();
					// get variables and stuff
					while ( match(TokenTag.VARIABLE)) 
					{
						f.addVar( lookBack(1) );
					}

					consume(TokenTag.RPARA);
					// so now we ALSO must update the function calls
					updateFunctionCall(f.getName(), statements.size());
				}
				else
				{
					Function function;
					function = new Function ( consume(TokenTag.FUNC).getValue(),
										  statements.size() );
					// main function position
					if ( lookBack(1).getValue().equals("main"))
						main = statements.size();
					consume(TokenTag.LPARA);
					// match variables only; we don't accept ints
					while ( match(TokenTag.VARIABLE))
					{
						function.addVar ( lookBack(1) );
					}
					// add the func and purge the rpara
					functions.put ( function.getName(), function );
					consume(TokenTag.RPARA);
				}
			}
			else if ( match(TokenTag.ENDFUNC) )
			{
				statements.add(new EndFunction());
			}
			// IDK WHAT THIS IS
			else
				break;
		}	
	}

	// I have to do some black magic because i was unaware of some stipulations up front
	// so this is a little hacky workdaround; this updates FunctionCalls in the statement list
	// with proper function positions
	private void updateFunctionCall(String f, int p)
	{
		for ( int i = 0; i < statements.size(); ++i )
		{
			if ( statements.get(i) instanceof FunctionCall)
			{
				FunctionCall func = (FunctionCall)statements.get(i);
				if ( func.getFunc().equals(f)){
					statements.remove(i);
					func.updatePosition ( p );
					statements.add(i, func);
				}
			}
		}
	}
	// match a function call.  This is here because there are two separate ways
	// we can call a function.  A) just by itself, func(), or as an assignment, S = func().
	private void matchFunctionCall ( )
		throws Exception
	{
		FunctionCall function = new FunctionCall();

		// if the next tag is a lpara, consume it
		if ( lookAhead(0).getTag() == TokenTag.LPARA)
			consume(TokenTag.LPARA);

		// function call
		if ( functions.containsKey(lookBack(2).getValue()))
		{
			// get the function name
			Function f = functions.get(lookBack(2).getValue());
			// generate the new function with its position and value
			function = new FunctionCall (lookBack(2).getValue(),
										f.getPosition());
		}
		// match vars that should be passed to the function
		while(match(TokenTag.VARIABLE) || match(TokenTag.INT)) 
		{
			// add the var to the functions var stack 
			function.addVar ( lookBack(1) );
		}
		// add the function and consume the rpara
		statements.add ( function );
		consume(TokenTag.RPARA);
	}

	// try and match the next token with the passed tag
	// if it's a match, then we move the parsing position forward
	// one idx
	private boolean match (TokenTag tag )
	{
		if ( tokens.get(pos).getTag() != tag )
			return false;
		
		pos++;
		return true;
	}
	
	//
	// match Terminator 1 to Terminator 2.
	// Nobody likes James Cameron, but unfortuantely he made 
	// two decent films so I'm forced to return true here
	// some time.  
	//
	private boolean match ( TokenTag t1, TokenTag t2)
	{
		if ( lookAhead(0).getTag() != t1 ) 
			return false;
		if ( lookAhead(1).getTag() != t2 )
			return false;

		pos += 2;
		return true;
	}

	// this should be called after match, as it moves our pos forward.
	// We don't actually remove tokens because we may need to goto somewhere
	// far away!
	private Token consume ( TokenTag tag )
		throws Exception
	{
		if ( tokens.get(pos).getTag() != tag )
			throw new Exception();
		return tokens.get(pos++);
	}

	// returns the token at the particular spot
	private Token lookAhead(int offset)
	{
		if ( (pos + offset) >= tokens.size() ){
			System.out.println ( "NOOOOOOOOOOOOOOO" );
			return null;
		}

		return tokens.get(pos + offset);
	}
	
	//
	// They always told me to move forward and never look back.
	//
	// They lied.
	private Token lookBack(int offset)
	{
		return tokens.get(pos - offset);
	}

	
	// match operators and consume 'em
	private boolean matchOp()
	{
		// if we still have tokens to match...
		if ( pos < tokens.size() )
		{
			// see if it's one of our operators
			if ( match(TokenTag.PLUS) || match(TokenTag.MINUS) ||
				 match(TokenTag.MULTIPLY) || match(TokenTag.DIVIDE) ||
				 match(TokenTag.MOD) || match(TokenTag.ASSIGN))
			{
				return true;	
			}
		}
		return false;
	}

	// get us one of our operators!  stat!
	private Expression op()
		throws Exception
	{
		Expression expression = getVar();
		// while there are still ops to consume; (match operators)
		while ( matchOp() )
		{
			// get what we're supposed to be doing
			char op = lookBack(1).getValue().charAt(0);
			// get the right side
			Expression right = getVar();
			// build it
			expression = new Operator(expression, op, right);
		}
		return expression;
	}

	// this returns an expression of whatever it's supposed to be, i.e.
	// we're matching a '4' in '4 + 4'
	private Expression getVar()
		throws Exception
	{
		// they can be one of three things; an int, a string, or a variable
		if ( match(TokenTag.VARIABLE) )
		{
			return new Variable(lookBack(1).getValue());
		}
		else if ( match(TokenTag.INT))
		{
			return new IntType(Integer.parseInt(lookBack(1).getValue()));
		}
		else if ( match(TokenTag.STRING))
		{
			return new StringType(lookBack(1).getValue());
		}
		else if ( match(TokenTag.EQ) || match(TokenTag.LT) || match(TokenTag.NE) ||
				  match(TokenTag.GT) || match(TokenTag.GE) || match(TokenTag.LE))
		{
			// if it's one of the above evaluators, then we consume the opening para,
			// consume both the left and right expressions, and finally the trailing para.
			// We then create a new func evaluation statement.
			consume(TokenTag.LPARA);
			Expression left = getVar();
			Expression right = getVar();
			consume(TokenTag.RPARA);
			return new FuncEval(left, right, lookBack(5).getTag() );
		}
		else if (match(TokenTag.LPARA))
		{
			// if we meet a left para, then we have some more parsing to do.
			// this lets us nest things for precendence
			Expression expression = op();
			consume(TokenTag.RPARA);
			return expression;
		}
		// these last two are just our boolean operators.  If TRUE is matched,
		// then we return 1 because that's what executes the to statement, otherwise
		// 0 and we don't execute
		else if (match(TokenTag.TRUE))
		{
			return new IntType(1);
		}
		else if ( match(TokenTag.FALSE))
		{
			return new IntType(0);
		}

		throw new Exception();
	}
// ******************** Expressions
	// These are the groupings of atomics, i.e. strings or ints.  These are given to the Statements
	// for executing in some manner; so, Write("Lol hai!"), the expression would be Lol hai! and the
	// statement would be Write()
	public interface Expression
	{
		// this returns us a Value, which can be of either type String or int
		Value evaluate();
	}

	// this handles, well, variables!  We support strings and ints
	public class Variable implements Expression
	{
		private String var;
		public Variable(String var)
		{
			this.var = var;
		}

		public Value evaluate()
		{
			// if the variable is in our variable hashmap, return it
			if(variables.containsKey(var.toLowerCase()))
			{
				return variables.get(var.toLowerCase());
			}
			// otherwise gen a new one
			return new IntType(0);
		}
		public String getVar()
		{
			return var;
		}
	}
	
	// this defines an operator lhs/rhs and what it evals to (add/sub/mult/etc)
	public class Operator implements Expression
	{
		private Expression left;
		private Expression right;
		private char op;

		// an operator is bound by a left and a right side, whether it's
		// an single integer or a nested expression. i.e. ((4 + 3) + 4)
		public Operator(Expression left, char op, Expression right)
		{
			this.left = left;
			this.right = right;
			this.op = op;
		}

		public Value evaluate()
		{
			Value lval = left.evaluate();
			Value rval = right.evaluate();

			switch(op)
			{
				// if it's an equals sign, then we need to check for equality between
				// two numbers or two strings
				case '=':
					if ( lval instanceof IntType)
						return new IntType((lval.toInt() == rval.toInt()) ? 1 : 0);
					else
						return new IntType(lval.toString().equals(rval.toString()) ? 1 : 0);
				// divide!  they should be ints and if they aren't an exception is thrown
				case '/':
					return new IntType(lval.toInt() / rval.toInt());
				// multiply!  
				case '*':
					return new IntType(lval.toInt() * rval.toInt());
				// subtraction!
				case '-':
					return new IntType(lval.toInt() - rval.toInt());
				// addition!  This could be concat too, so //TODO
				case '+':
					return new IntType(lval.toInt() + rval.toInt());
			}
			// IDK WHAT THIS IS MAN
			//throw new Exception();
			return null;
		}
	}
	
	// this evaluates the built in eval statements, i.e. LT/GT/GE/etc
	public class FuncEval implements Expression
	{
		private Expression left;
		private Expression right;
		private TokenTag tag;
		public FuncEval (Expression left, Expression right, TokenTag tag)
		{
			this.left = left;
			this.right = right;
			this.tag = tag;
		}

		public Value evaluate()
		{
			Value lval = left.evaluate();
			Value rval = right.evaluate();
			//
			// we want to return 1 if it's accurate, or 0 if it's not.
			//
			switch ( tag )
			{
				case EQ:
					if ( lval.toInt() == rval.toInt() )
						return new IntType(1);
					else
						return new IntType(0);

				case NE:
					if ( lval.toInt() != rval.toInt() )
						return new IntType(1);
					else
						return new IntType(0);

				case LT:
					if ( lval.toInt() < rval.toInt() )
						return new IntType(1);
					else
						return new IntType(0);
				case LE:
					if ( lval.toInt() <= rval.toInt() )
						return new IntType(1);
					else
						return new IntType(0);

				case GT:
					if ( lval.toInt() > rval.toInt() )
						return new IntType(1);
					else
						return new IntType(0);

				case GE:
					if ( lval.toInt() >= rval.toInt() )
						return new IntType(1);
					else
						return new IntType(0);
			}
			return null;
		}
	}
// ******************** Statements
	// This is an interface for Statements, or productions I guess?  This actually handles the
	// expressions and what we should do with them. (print, assign, etc.)
	public interface Statement
	{
		void execute();
	}	

	// this handles our 'Write' call
	public class Print implements Statement
	{
		// this could be just a string or a variable or something!
		private Expression expression;
		public Print ( Expression e ) 
		{
			this.expression = e;
		}
	
		public void execute()
		{
			// output!
			System.out.println ( expression.evaluate().toString() ); 
		}
	}

	// this handles our 'Read' call
	class Input implements Statement
	{
		private String value;
		public Input ( String v )
		{
			this.value = v;
		}

		public void execute ()
		{
			String input = "";

			try
			{
				// get input from the user and store it
				BufferedReader reader = new BufferedReader (
							new InputStreamReader ( System.in ));
				input = reader.readLine();

				int tmp = Integer.parseInt(input);
				variables.put(value.toLowerCase(), new IntType(tmp));
			}
			catch(Exception e)
			{
				// it cannot be parsed! 
				variables.put(value.toLowerCase(), new StringType(input));
			}
		}
	}			

	// assigns a variable to the result of a rhs expression
	class Assignment implements Statement
	{
		private String variable_name;
		private Expression expression;
		public Assignment ( String v, Expression value )
		{
			this.variable_name = v;
			this.expression = value;
		}

		public void execute()
		{
			// evaluate the expression and assign it to the lhs
			variables.put(variable_name.toLowerCase(), expression.evaluate() );
		}
	}
	
	// our simple if statement
	class IfStatement implements Statement
	{
		private Expression condition;
		private String gLabel;

		public IfStatement(Expression cond, String tmp )
		{
			this.condition = cond;
			this.gLabel = tmp;
		}

		public void execute()
		{
			// if it's a valid label...
			if ( labels.containsKey(gLabel.toLowerCase()))
			{
				// capture the rval and check if we need to move
				int val = condition.evaluate().toInt();
				if ( 0 != val )
				{
					pos = labels.get(gLabel.toLowerCase());
				}
			}
		}
	}

	// this is a function call.  i.e. call().  
	// This pushes the current position onto the stack, then 
	// moves to the position of the function being called.
	public class FunctionCall implements Statement
	{
		private String func;
		private int p;
		private Stack<Token> call_vars = new Stack<Token>();
		private ArrayList<Token> call_vars_stat = new ArrayList<Token>();

		public FunctionCall() {}

		public FunctionCall ( String f, int p )
		{
			this.func = f;
			this.p = p;
		}
		
		public void addVar ( Token t )
		{
			call_vars.push ( t );
			call_vars_stat.add ( t );
		}
		
		// update the position of the function call
		public void updatePosition ( int p )
		{
			this.p = p;
		}
		
		public String getFunc(){
			return func;
		}
		
		public void execute()
		{
			// this'll happen if we're calling functions in a loop; repopulate the stack!
			if ( call_vars.empty() )
			{
				for ( Token t : call_vars_stat )
					call_vars.push(t);
			}
			
			// repopulate the receiving function's var stack!
			Function f = functions.get(func);
			if ( f.getVarCount() <= 0 )
			{
				ArrayList<Token> tmp = f.getCallVars();
				for ( Token t : tmp )
					f.addVar ( t );
			}

			// pop vars off the called function's object stack and assign them to the caller's
			// passed tokens.  so test(4, 5) calling Func test ( m, n ), m = 4 and n = 5
			while ( !call_vars.empty() && call_vars.peek() != null )
			{
				// pop a token off the calling var stack 
				Token call_var = call_vars.pop(); 
				// pop a token off the functions var stack
				Token func_var = (functions.get(func)).getVar();
				// if it's an int, then we create a new variable with the function stack's
				// next token
				if ( call_var.getTag() == TokenTag.INT )
				{
					variables.put(func_var.getValue().toLowerCase(), 
										new IntType(Integer.parseInt(call_var.getValue())));	
				}
				// if it's a variable, then we want to grab it's value and create a new one
				// with the function's var
				else if ( call_var.getTag() == TokenTag.VARIABLE )
				{
					IntType tmp = new IntType ( 
									variables.get(call_var.getValue().toLowerCase()).toInt() );
					variables.put(func_var.getValue().toLowerCase(), tmp);
				}
			}

			// push our current pos onto the stack and move to the functions
			// calling position
			lastPosition.push(pos);
			pos = p;
		}
	}

	// This executes an end function statement, which basically
	// pops the last position off the stack and loads it into our
	// current position
	public class EndFunction implements Statement
	{
		public EndFunction() {}

		public void execute()
		{
			// if there's stuff on the stack, pop it, otherwise
			// we're back in main
			if ( lastPosition.size() > 0 )
				pos = lastPosition.pop();
			else
				return;
		}
	}
// **********************

// *************** Value types
// These define the data types, but we only have two so it should be easy
	public interface Value extends Expression
	{
		String toString();
		int toInt();
	}

	// this defines our int datatype
	class IntType implements Value
	{
		private int value;
		public IntType ( int v )
		{
			this.value = v;
		}

		public String toString()
		{
			return Integer.toString(value);
		}
		public int toInt()
		{
			return value;
		}
		public Value evaluate()
		{		
			return this;
		}
	}

	// this defines our string datatype
	class StringType implements Value
	{
		private String value;
		public StringType ( String v )
		{
			this.value = v;
		}
		public String toString()
		{
			return value;
		}
		public int toInt()
		{
			return Integer.parseInt(value);
		}
		public Value evaluate()
		{
			return this;
		}
	}
// *****************************************
} //public class Parser
