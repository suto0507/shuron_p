package system.parsers;

import com.microsoft.z3.Expr;

import system.Check_return;
import system.Check_status;
import system.Parser;
import system.Parser_status;
import system.Source;

public class expression implements Parser<String>{
	assignment_expr assignment_expr;
	String st;
	public String parse(Source s,Parser_status ps)throws Exception{
		this.assignment_expr = new assignment_expr();
		st = this.assignment_expr.parse(s,ps);
		return st;
	}
	
	public Check_return check(Check_status cs) throws Exception{
		System.out.println("/////// " + st);
		return this.assignment_expr.check(cs);
	}
	


}
