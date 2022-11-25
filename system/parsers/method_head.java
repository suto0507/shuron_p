package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class method_head implements Parser<String>{
	String ident;
	formals formals;
	
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		this.ident  = new ident().parse(s, ps);
		st = st + this.ident;
		st = st + new spaces().parse(s, ps);
		formals f = new formals();
		st = st + f.parse(s, ps);
		this.formals = f;
		return st;
	}
	
	

}
