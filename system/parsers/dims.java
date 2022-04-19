package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class dims implements Parser<Integer>{
	public Integer parse(Source s,Parser_status ps)throws Exception{
		new string("[]").parse(s, ps);
		return 1;
	}	
}
