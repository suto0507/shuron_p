package system.parsers;

import java.util.List;

import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;

import system.Check_return;
import system.Check_status;
import system.Field;
import system.Pair;
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
	
	public boolean have_index_access(Check_status cs){
		return assignment_expr.have_index_access(cs);
	}
	
	public Check_return loop_assign(Pair<List<Pair<Field,List<List<IntExpr>>>>,Boolean>assigned_fields, Check_status cs) throws Exception{
		return this.assignment_expr.loop_assign(assigned_fields, cs);
	}

}
