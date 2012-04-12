package src;

/*
 This defines all the keywords/token tags for our tokens.
 */
public enum TokenTag
{
	NONE,	// this really isn't a token or tag, it's used for parsing
	FUNC, ENDFUNC, RETURN, TO,
	READ, WRITE, 
	LPARA, RPARA,
	IF, EQ, NE, GT, GE, LT, LE,
	OP, PLUS, MINUS, MULTIPLY, DIVIDE, MOD,
	TRUE, FALSE, ASSIGN,
	INT, STRING, VARIABLE, COMMENT, LABEL
}
