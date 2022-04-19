package system.parsers;

import com.microsoft.z3.Expr;

import system.Check_status;
import system.Parser;
import system.Parser_status;
import system.Source;

public class spec_expression implements Parser<String>{
	expression expression;
	public String parse(Source s,Parser_status ps)throws Exception{
		this.expression = new expression();
		return expression.parse(s,ps);
	}
	
	public Expr check(Check_status cs) throws Exception{
		return this.expression.check(cs);
	}
	


}
