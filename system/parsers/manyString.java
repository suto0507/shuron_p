package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class manyString implements Parser<String>{
	Parser p;
	manyString(Parser p){
		this.p = p;
	}
	
	public String parse(Source s,Parser_status ps){
		String st = "";
		Source s_backup = s.clone();
		try {
			while(true){
				s_backup = s.clone();
				st = st + p.parse(s,ps);
			}
		}catch (Exception e){
			s.revert(s_backup);
		}
		return st;
	}
}