package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class maintaining_keyword implements Parser<String>{
	public String parse(Source s,Parser_status ps)throws Exception{
		return new string("maintaining").parse(s,ps);
	}
}
