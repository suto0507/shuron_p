package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class dims implements Parser<String>{
	int dims;
	
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = new string("[]").parse(s, ps);
		dims = 1;
		
		Source  s_backup = s.clone();
		try {
			while(true){
				st += new string("[]").parse(s, ps);
				dims++;
			}
		}catch (Exception e2){
			s.revert(s_backup);
		}
		return st;
	}	
}
