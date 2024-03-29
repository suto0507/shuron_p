package system.parsers;

import com.microsoft.z3.Expr;

import system.Check_status;
import system.Parser;
import system.Parser_status;
import system.Source;

public class ensures_clause implements Parser<String>{
	predicate predicate;
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		st = st + new string("ensures").parse(s, ps);
		st = st + new spaces().parse(s, ps);
		Parser_status ps_en = ps.ensures();
		this.predicate = new predicate();
		st = st + this.predicate.parse(s, ps_en);
		st = st + new spaces().parse(s, ps);
		st = st + new string(";").parse(s, ps);
		return st;
	}
	
	public Expr check(Check_status cs) throws Exception{
		return this.predicate.check(cs);
	}

}