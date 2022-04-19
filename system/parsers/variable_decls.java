package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class variable_decls implements Parser<String>{
	public type_spec type_spec;
	String ident;
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		this.type_spec = new type_spec();
		st = st + this.type_spec.parse(s, ps);
		st = st + new spaces().parse(s, ps);
		this.ident = new ident().parse(s, ps);
		st = st + this.ident;
		return st;
	}
}
