package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class additive_op implements Parser<String>{
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		Source s_backup = s.clone();
		try{
			st = new string("+").parse(s, ps);
		}catch (Exception e){
			s.revert(s_backup);
			st = new string("-").parse(s, ps);
		}
		
		return st;
	}
}
