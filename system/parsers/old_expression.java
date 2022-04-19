package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class old_expression implements Parser<String>{
	spec_expression spec_expression;
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		st = st + new string("\\old").parse(s, ps);
		st = st + new spaces().parse(s, ps);
		st = st + new string("(").parse(s, ps);
		st = st + new spaces().parse(s, ps);
		this.spec_expression = new spec_expression();
		st = this.spec_expression.parse(s, ps);
		st = st + new spaces().parse(s, ps);
		st = st + new string(")").parse(s, ps);
		
		return st;
	}
	

}
