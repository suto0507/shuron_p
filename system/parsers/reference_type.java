package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class reference_type implements Parser<String>{
	String type;
	public String parse(Source s,Parser_status ps)throws Exception{
		this.type = new ident().parse(s,ps);
		return type;
	}
}
