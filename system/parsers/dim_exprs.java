package system.parsers;

import java.util.ArrayList;

import com.microsoft.z3.IntExpr;

import system.Check_status;
import system.Parser;
import system.Parser_status;
import system.Source;

public class dim_exprs  implements Parser<String>{
	int dims;
	ArrayList<expression> expressions;
	
	public String parse(Source s,Parser_status ps)throws Exception{
		expressions = new ArrayList<expression>();
		
		String st = new string("[").parse(s, ps);
		st += new spaces().parse(s, ps);
		expression ex = new expression();
		st +=ex.parse(s, ps);
		st += new spaces().parse(s, ps);
		st += new string("]").parse(s, ps);
		dims = 1;
		expressions.add(ex);
		
		Source  s_backup = s.clone();
		try {
			while(true){
				s_backup = s.clone();
				String st2 = new spaces().parse(s, ps);
				st2 += new string("[").parse(s, ps);
				st2 += new spaces().parse(s, ps);
				expression ex2 = new expression();
				st2 +=ex2.parse(s, ps);
				st2 += new spaces().parse(s, ps);
				st2 += new string("]").parse(s, ps);
				dims++;
				expressions.add(ex2);
				st += st2;
			}
		}catch (Exception e2){
			s.revert(s_backup);
		}
		return st;
	}
	
	
}
