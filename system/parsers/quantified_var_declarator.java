package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class quantified_var_declarator implements Parser<String>{
	String ident;
	
	public String parse(Source s,Parser_status ps)throws Exception{
		ident = new ident().parse(s, ps);
		return ident;
		
	}
}
