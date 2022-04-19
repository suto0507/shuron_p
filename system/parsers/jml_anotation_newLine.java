package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class jml_anotation_newLine implements Parser<String>{
	Parser p;
	jml_anotation_newLine(Parser p){
		this.p = p;
	}
	public String parse(Source s,Parser_status ps)throws Exception{
		return new jml_anotation(p).parse(s,ps) + new newLines().parse(s,ps);
	}
}
