package src;

/*
 This class is the Token object, and is created/used by the scanner.  
 */
public class Token
{
	private String value;
	private TokenTag tag;

	public Token ( String text, TokenTag tag ) 
	{
		this.tag = tag;
		this.value = text;
	}

	public TokenTag getTag()
	{
		return tag;
	}

	public void setTag(TokenTag t)
	{
		this.tag = t;
	}

	public String getValue()
	{
		return value;
	}
}
