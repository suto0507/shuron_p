package system.parsers;

import com.microsoft.z3.BoolExpr;

import system.Check_status;
import system.Parser;
import system.Parser_status;
import system.Source;

public class assert_statement implements Parser<String>{
	predicates predicates;
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		st = st + new string("assert").parse(s, ps);
		st = st + new spaces().parse(s, ps);
		this.predicates = new predicates();
		st = st + this.predicates.parse(s, ps);
		st = st + new spaces().parse(s, ps);
		st = st +new string(";").parse(s, ps);
		return st;
	}
	
	public void check(Check_status cs) throws Exception{
		BoolExpr expr = this.predicates.check(cs);
		cs.assert_constraint(expr);
	}
}
