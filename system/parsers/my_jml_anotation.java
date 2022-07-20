package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class my_jml_anotation implements Parser<String>{
	Parser p;
	my_jml_anotation(Parser p){
		this.p = p;
	}
	public String parse(Source s,Parser_status ps)throws Exception{
		ps.in_jml = true;
		String st =  "" + p.parse(s,ps);
		ps.in_jml = false;
		return st;
	}
}
