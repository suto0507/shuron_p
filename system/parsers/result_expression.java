package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class result_expression implements Parser<String>{
	public String parse(Source s,Parser_status ps)throws Exception{
		return new string("\\result").parse(s, ps);
	}
	

}
