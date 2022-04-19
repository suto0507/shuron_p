package system.parsers;

import com.microsoft.z3.BoolExpr;

import system.Check_status;
import system.Parser;
import system.Parser_status;
import system.Source;
public class boolean_literal implements Parser<String>{
	boolean bool;
	String st;
	public String parse(Source s,Parser_status ps)throws Exception{
		st = "";
		Source s_backup = s.clone();
		try{
			st = new string("true").parse(s, ps);
			this.bool = true;
		}catch (Exception e){
			s.revert(s_backup);
			st = new string("false").parse(s, ps);
			this.bool = false;
		}
		
		return st;
		
	}
	
	public BoolExpr check(Check_status cs){
		return cs.ctx.mkBool(this.bool);
	}
	
}
