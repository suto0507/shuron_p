package system.parsers;

import com.microsoft.z3.Expr;

import system.Check_return;
import system.Check_status;
import system.Parser;
import system.Parser_status;
import system.Source;

public class java_literal implements Parser<String>{
	integer_literal integer_literal;
	boolean_literal boolean_literal;
	public String parse(Source s,Parser_status ps)throws Exception{
		Source s_backup = s.clone();
		String st = "";
		try{
			integer_literal il = new integer_literal();
			st = st + il.parse(s,ps);
			this.integer_literal = il;
		}catch (Exception e){
			s.revert(s_backup);
			boolean_literal bl = new boolean_literal();
			st = st + bl.parse(s,ps);
			this.boolean_literal = bl;
		}
		return st;
	}
	
	public Check_return check(Check_status cs){
		if(this.boolean_literal!=null){
			return new Check_return(this.boolean_literal.check(cs), null, null, null, "boolean", 0);
		}else{
			return new Check_return(this.integer_literal.check(cs), null, null, null, "int", 0);
		}
	}
}
