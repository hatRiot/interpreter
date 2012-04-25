package src;

import java.util.Stack;
import java.util.ArrayList;

public class Function
{
	private String function;
	private int position;
	private Stack<Token> vars = new Stack<Token>();
	private ArrayList<Token> vars_stat = new ArrayList<Token>();

	public Function ( String f, int p )
	{
		this.function = f;
		this.position = p;
	}

	public void addVar ( Token t )
	{
		vars.push ( t );
		if (!vars_stat.contains(t))
			vars_stat.add ( t );
	}
	
	public ArrayList<Token> getCallVars()
	{
		return vars_stat;
	}

	public Token getVar ()
	{
		if ( vars.size() > 0 )
			return vars.pop();
		return null;
	}
	
	public int getVarCount()
	{
		return vars.size();
	}

	public String getName()
	{
		return function;
	}

	public int getPosition()
	{
		return position;
	}

	public void setPosition(int p)
	{
		this.position = p;
	}
}
