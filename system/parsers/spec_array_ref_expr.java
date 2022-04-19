package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class spec_array_ref_expr implements Parser<String>{
	spec_expression spec_expression;
	public String parse(Source s,Parser_status ps)throws Exception{
		this.spec_expression = new spec_expression();
		return spec_expression.parse(s,ps);
	}

}