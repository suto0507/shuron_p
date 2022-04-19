package system.parsers;

import com.microsoft.z3.IntNum;

import system.Check_status;
import system.Parser;
import system.Parser_status;
import system.Source;

public class integer_literal implements Parser<String>{
	int integer;
	String st;
	public String parse(Source s,Parser_status ps)throws Exception{
		Source s_backup = s.clone();
		st = "";
		try{
			st = "";
			char c = new cha('0').parse(s, ps);
			this.integer = Character.digit(c,10);
			st = st + c;
		}catch (Exception e){
			s.revert(s_backup);
			st = "";
			char c = new non_zero_digit().parse(s, ps);
			this.integer = Character.digit(c,10);
			st = st + c;
			Source s_backup2 = s.clone();
			try {
				while(true){
					s_backup2 = s.clone();
					c = new digit().parse(s, ps);
					this.integer = this.integer*10 + Character.digit(c,10);
					st = st + c;
				}
			}catch (Exception e2){
				s.revert(s_backup2);
			}	
		}
		return st;

	}
	
	public IntNum check(Check_status cs){
		return cs.ctx.mkInt(this.integer);
	}
	
}
