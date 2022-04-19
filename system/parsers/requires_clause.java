package system.parsers;

import com.microsoft.z3.Expr;

import system.Check_status;
import system.Parser;
import system.Parser_status;
import system.Source;

public class requires_clause implements Parser<String>{
	predicates predicates;
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		st = st + new string("requires").parse(s, ps);
		st = st + new spaces().parse(s, ps);
		this.predicates = new predicates();
		st = st + this.predicates.parse(s, ps);
		st = st + new spaces().parse(s, ps);
		st = st + new string(";").parse(s, ps);
		return st;
	}
	
	public Expr check(Check_status cs) throws Exception{
		return this.predicates.check(cs);
	}

}