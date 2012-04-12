package src;

import java.io.*;
import java.util.*;

// entry
public class nectj
{
	public static void main ( String args[] )
			throws FileNotFoundException,IOException, Exception
	{
		// if they didn't even give us a source file
		if ( args.length == 0 )
			return;
		
		// get the file contents and pass it off 
		// to the lexer
		String text = readFile(args[0]);
		TheScan scanner = new TheScan(text);
	}
	
	// Read the file in 
	private static String readFile ( String file )
		throws IOException
	{
		BufferedReader br;
		StringBuilder data = new StringBuilder();

		if ( file != null )
		{
			br = new BufferedReader(new FileReader(file));
			String line;
			// read in a line at a time, and tack on a \n for delimiting!
			while ( (line = br.readLine()) != null)
				data.append(line + "\n");
		}

		return data.toString();
	}
}
