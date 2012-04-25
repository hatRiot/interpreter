package src;

import java.util.*;
import java.util.regex.*;
import java.io.*;

/*
 This class implements our lexer, or scanner, or tokenizer, or whatever.
 Basically it iterates through the source code character by character and creates
 valid tokens out of it. After the lexical analysis is done, the parser takes
 control and starts taking care of statements. 
 */
public class TheScan
{
	// global tokenized list
	private ArrayList<Token> tokens = new ArrayList<Token>();

	// list of all our possible operators
	private ArrayList<Character> operators = new ArrayList<Character>(
			Arrays.asList('(', ')', '+', '-', '*', '%', '/', '=',
						  '<', '>'));

	// untokenized data
	private String data; 

	public TheScan ( String text )
		throws IOException, Exception
	{
		// store a copy of the text
		this.data = text;

		// start the interpreter 
		interpret();
	}
	
	// i guess this is the 'main'; it inits tokenization and executes
	// statements
	private void interpret()
	{
		// tokenize the file 
		tokenize();
		
		// dbg
		//printer();

		// call the parser and pass it the tokenized list
		Parser parser = new Parser(tokens);
	}

	// This method creates a new token based on 
	// the current operator
	private void addOperator ( char c )
	{
		// initialize the token tag with a dummy var
		TokenTag tag = TokenTag.NONE;
		
		switch ( c )
		{
			case '(':
				tag = TokenTag.LPARA;
				break;
			case ')':
				tag = TokenTag.RPARA;
				break;
			case '+':
				tag = TokenTag.PLUS;
				break;
			case '-':
				tag = TokenTag.MINUS;
				break;
			case '*':
				tag = TokenTag.MULTIPLY;
				break;
			case '/':
				tag = TokenTag.DIVIDE;
				break;
			case '%':
				tag = TokenTag.MOD;
				break;
			case '<':
				tag = TokenTag.LT;
			case '>':
				tag = TokenTag.GT;
				break;
			case '=':
				tag = TokenTag.ASSIGN;
				break;
			default:
				return;
		}
		// add it to the token list
		tokens.add(new Token(Character.toString(c), tag));
	}
	
	// this takes the local content string and tokenizes
	// it into a list, which we use later in the AST
	private void tokenize()
	{
		// initialize the tag we're working on parsing
		TokenTag tag = TokenTag.NONE;

		// the current 'token' we're working on parsing
		String current = "";

		// start scanning through all the plaintext, one
		// character a time and tokenize stuff we see/need
		for ( int i = 0; i < data.length(); ++i )
		{
			// read the character
			char c = data.charAt(i);

			// if it's maybe a comment
			if ( c == '/') 
			{
				current += c;
				// is the next one a /?
				if ( (c = data.charAt(++i)) == '/' )
				{
					current = "";
					// consume dat comment
					while ( ( c = data.charAt(i++)) != '\n' ) {}
					tag = TokenTag.NONE;
				}
				// just kidding, it's division!
				else{
					tag = TokenTag.OP;
					c = current.charAt(0);
				}
				--i; //rewind
			}
			// oh hey it's a label
			else if ( c == ':' )
			{
				// if the previous value was a VARIABLE, retag it as a LABEL
				if ( tokens.get(tokens.size() - 1).getTag() == TokenTag.VARIABLE ){
					Token tmp = tokens.get(tokens.size() - 1);
					tokens.remove(tokens.size() - 1);
					tmp.setTag(TokenTag.LABEL);
					tokens.add(tmp);
				}
			}
			// if it's a string (for output, say)
			else if ( c == '"' )
			{
				tag = TokenTag.STRING;
			}
			// figure out what it is and set the current tag
			// if it's an operator, go figure out what it is
			else if ( operators.contains(c))
			{
				tag = TokenTag.OP;
			}
			// if it's a digit and we're not parsing anything in particular atm
			else if ( tag == TokenTag.NONE && Character.isDigit(c))
			{
				tag = TokenTag.INT;
			}
			// if it's a letter (vars, for instances) OR we're building a variable and
			// we run across a digit (for instance, N1)
			else if ( Character.isLetter(c) ||
							(tag == TokenTag.VARIABLE && Character.isDigit(c)))
			{
				current += c;
				tag = TokenTag.VARIABLE;
			}
			//System.out.println ( "preprocessing " + c + " with tag " + tag);
			// switch on the current tag and do some parsin'
			switch ( tag )
			{
				case OP:
					addOperator(c);
					current = "";
					tag = TokenTag.NONE;
					break;
				case INT:
					if ( Character.isDigit(c))
					{
						// consume them digits
						while ( Character.isDigit(c)) {
							current += c;
							c = data.charAt(++i);
						}
						
						// create the integer
						tokens.add(new Token (current, TokenTag.INT));
						current = "";
						tag = TokenTag.NONE;
						i--;	// rewind
					}
					else
					{
						// create the token, clear the current string
						// and reset the parsing tag
						tokens.add ( new Token (current, TokenTag.INT));
						current = "";
						tag = TokenTag.NONE;
						i--; // rewind
					}
					break;
				// handles variables
				case VARIABLE:
					// if the variable is a Func, then we have some stuff to do
					if ( current.equalsIgnoreCase("Func"))
					{
						// add 'Func'
						tokens.add(new Token(current, TokenTag.FUNC));
						current = "";
						// consume the space between Func and the name
						i++;
						// consume the function name
						while ( (c = data.charAt(++i)) != '(')
							current += c;
						// create the function name
						tokens.add(new Token(current, TokenTag.FUNC));
						current = "";
						tag = TokenTag.NONE;
						i--;
					}
					else if ( current.equalsIgnoreCase("Endfunc") )
					{
						tokens.add(new Token(current, TokenTag.ENDFUNC));
						current = "";
						tag = TokenTag.NONE;
					}
					else if ( current.equalsIgnoreCase("Write"))
					{
						//add 'Write' token
						tokens.add(new Token(current, TokenTag.WRITE));
						current = "";
						tag = TokenTag.NONE;
					}
					else if ( current.equalsIgnoreCase("Read"))
					{
						// add 'Read' token
						tokens.add(new Token(current, TokenTag.READ));
						current = "";
						tag = TokenTag.NONE;
					}
					else if ( current.equalsIgnoreCase("IF"))
					{
						tokens.add(new Token(current, TokenTag.IF));
						current = "";
						tag = TokenTag.NONE;
					}
					else if ( current.equalsIgnoreCase("EQ"))
					{
						tokens.add(new Token(current, TokenTag.EQ));
						current = "";
						tag = TokenTag.NONE;
					}
					else if ( current.equals("to"))
					{
						tokens.add(new Token(current, TokenTag.TO));
						current = "";
						tag = TokenTag.NONE;
					}
					else if ( current.equalsIgnoreCase("GE"))
					{
						tokens.add(new Token(current, TokenTag.GE));
						current = "";
						tag = TokenTag.NONE;
					}
					else if ( current.equalsIgnoreCase("LT"))
					{
						tokens.add(new Token(current, TokenTag.LT));
						current = "";
						tag = TokenTag.NONE;
					}
					else if ( current.equalsIgnoreCase("NE"))
					{
						tokens.add(new Token(current, TokenTag.NE));
						current = "";
						tag = TokenTag.NONE;
					}
					else if ( current.equalsIgnoreCase("GT"))
					{
						tokens.add(new Token(current, TokenTag.GT));
						current = "";
						tag = TokenTag.NONE;
					}
					else if ( current.equalsIgnoreCase("LE" ))
					{
						tokens.add(new Token(current, TokenTag.LE));
						current = "";
						tag = TokenTag.NONE;
					}
					else if ( current.equalsIgnoreCase("TRUE"))
					{
						tokens.add(new Token(current, TokenTag.TRUE));
						current = "";
						tag = TokenTag.NONE;
					}
					else if ( current.equalsIgnoreCase("FALSE"))
					{
						tokens.add(new Token(current, TokenTag.FALSE));
						current = "";
						tag = TokenTag.NONE;
					}
					else if ( current.equalsIgnoreCase("Endfunc"))
					{
						// add 'Endfunc' token
						tokens.add(new Token(current, TokenTag.ENDFUNC));
						current = "";
						tag = TokenTag.NONE;
					}
					else
					{
						// this is for if we're reading just a variable
						char tmpc = data.charAt(++i);
						if ( Character.isLetter(tmpc) ||
							 Character.isDigit(tmpc)){
							i--;
							continue;
						}
					
						// if it's a space or something else, create the variable
						tokens.add(new Token(current, TokenTag.VARIABLE));
						current = "";
						tag = TokenTag.NONE;
						i--;
					}
					break;
				case FUNC:
					if ( Character.isLetter(c))
						current += c;
					// read that func on out
					else if ( c == '\n')
					{
						tokens.add(new Token(current, TokenTag.FUNC));
						current = "";
						tag = TokenTag.NONE;
					}
					break;
				case STRING:
					// get everything up to the ending "
					while ( (c = data.charAt(++i)) != '"') 
						current += c ;

					// tokenize
					tokens.add(new Token(current, TokenTag.STRING));
					current = "";
					tag = TokenTag.NONE;
					break;
			}
		}
	}

	// util: prints stuff
	private void printer()
	{
		System.out.println ( "\t**FILE TOKENS: " );
		for ( Token s : tokens )
			System.out.println ( s.getValue().toString() + " : " + s.getTag() );
	}
}
