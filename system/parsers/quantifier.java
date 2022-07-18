package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class quantifier implements Parser<String>{
	String quantifier;
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = new string("\\").parse(s, ps);
		Source s_backup = s.clone();
		try{
			quantifier = new string("forall").parse(s, ps);
			st += quantifier;
		}catch (Exception e){
			s.revert(s_backup);
			quantifier = new string("exists").parse(s, ps);
			st += quantifier;
		}
		
		return st;
	}
}
