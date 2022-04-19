package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class my_jml_anotation_newLine implements Parser<String>{
	Parser p;
	my_jml_anotation_newLine(Parser p){
		this.p = p;
	}
	public String parse(Source s,Parser_status ps)throws Exception{
		return new my_jml_anotation(p).parse(s,ps) + new newLines().parse(s,ps);
	}
}