package system.parsers;

import java.util.ArrayList;

import com.microsoft.z3.IntExpr;

import system.Check_status;
import system.Parser;
import system.Parser_status;
import system.Source;

public class array_decl implements Parser<String>{
	int dims;
	ArrayList<expression> expressions;
	
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		dim_exprs de = new dim_exprs();
		st += de.parse(s, ps);
		this.dims = de.dims;
		this.expressions = de.expressions;
		
		Source  s_backup = s.clone();
		try {
			String st2 = new spaces().parse(s, ps);
			dims d = new dims();
			st2 += d.parse(s, ps);
			this.dims += d.dims;
			st += st2;
		}catch (Exception e2){
			s.revert(s_backup);
		}
		return st;
	}
	
	public ArrayList<IntExpr> check(Check_status cs) throws Exception{
		ArrayList<IntExpr> exprs = new ArrayList<IntExpr>();
		for(expression e : expressions){
			IntExpr ex = (IntExpr)e.check(cs);
			exprs.add(ex);
		}
		
		return exprs;
	}
}
