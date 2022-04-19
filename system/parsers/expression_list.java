package system.parsers;

import java.util.ArrayList;
import java.util.List;

import system.Check_status;
import system.Parser;
import system.Parser_status;
import system.Source;

public class expression_list implements Parser<String>{
	List<expression> expressions;
	public String parse(Source s,Parser_status ps)throws Exception{
		this.expressions = new ArrayList<expression>();
		
		String st = "";
		expression ex = new expression();
		st = st + ex.parse(s, ps);
		this.expressions.add(ex);
		
		Source s_backup = s.clone();
		try {
			while(true){
				s_backup = s.clone();
				String st2 = new spaces().parse(s, ps);
				st2 = st2 + new string(",").parse(s, ps);
				st2 = st2 + new spaces().parse(s, ps);
				ex = new expression();
				st2 = st2 + ex.parse(s, ps);
				st = st + st2;
				this.expressions.add(ex);
			}
		}catch (Exception e){
			s.revert(s_backup);
		}	
		
		return st;
	}
	
	public void check(Check_status cs) throws Exception{
		for(expression ex : expressions){
			ex.check(cs);
		}
	}

}
