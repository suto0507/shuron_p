package system.parsers;

import com.microsoft.z3.Expr;

import system.Check_return;
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
	
	public Check_return check(Check_status cs) throws Exception{
		return this.expression.check(cs);
	}
	
	public boolean have_index_access(Check_status cs){
		return expression.have_index_access(cs);
	}
	
	

}
