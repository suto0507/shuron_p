package system.parsers;

import com.microsoft.z3.Expr;

import system.Check_status;
import system.Parser;
import system.Parser_status;
import system.Source;

public class invariant implements Parser<String>{
	String st;
	boolean is_private;
	predicates predicates;
	
	public String parse(Source s,Parser_status ps)throws Exception{
		this.st = "";
		st = st + new string("invariant").parse(s,ps);
		st = st + new spaces().parse(s,ps);
		this.predicates = new predicates();
		st = st + predicates.parse(s,ps);
		st = st + new spaces().parse(s,ps);
		st = st + new string(";").parse(s,ps);
		
		return st;
	}
	
	void set_is_private(boolean b){
		this.is_private = b;
	}
	
	public Expr check(Check_status cs) throws Exception{
		return this.predicates.check(cs);
	}

}
