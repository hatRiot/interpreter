package src;

import java.util.Stack;

public class Function
{
	private String function;
	private int position;
	private Stack<Token> vars = new Stack<Token>();

	public Function ( String f, int p )
	{
		this.function = f;
		this.position = p;
	}

	public void addVar ( Token t )
	{
		vars.push ( t );
	}

	public Token getVar ()
	{
		if ( vars.size() > 0 )
			return vars.pop();
		return null;
	}

	public String getName()
	{
		return function;
	}

	public int getPosition()
	{
		return position;
	}
}
