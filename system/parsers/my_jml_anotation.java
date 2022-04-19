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
		Parser_status jml_ps = ps.jml();
		return new string("/*`@").parse(s,ps) + new spaces().parse(s,ps) + p.parse(s,jml_ps) + new spaces().parse(s,ps) + new string("*/").parse(s,ps);
	}
}
