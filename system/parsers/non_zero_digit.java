package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class non_zero_digit implements Parser<Character>{
	
	public Character parse(Source s,Parser_status ps) throws Exception{
		return new satisfy(c -> 0<=c.compareTo('1') && c.compareTo('9')<=0).parse(s,ps);
	}
}
