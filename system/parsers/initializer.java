package system.parsers;

import java.util.List;

import com.microsoft.z3.IntExpr;

import system.Check_return;
import system.Check_status;
import system.Field;
import system.Pair;
import system.Parser;
import system.Parser_status;
import system.Source;

public class initializer implements Parser<String>{
	public expression expression;
	String ident;
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		expression expression = new expression();
		st = st + expression.parse(s, ps);
		this.expression = expression;
		return st;
	}
	
	public Check_return check(Check_status cs) throws Exception{
		return this.expression.check(cs);
	}
	
	public boolean have_index_access(Check_status cs){
		return expression.have_index_access(cs);
	}
	
	public Check_return loop_assign(Pair<List<Pair<Field,List<List<IntExpr>>>>,Boolean>assigned_fields, Check_status cs) throws Exception{
		return this.expression.loop_assign(assigned_fields, cs);
	}
}
