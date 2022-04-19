package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class space implements Parser<Character>{
	public Character parse(Source s,Parser_status ps)throws Exception{
		return new satisfy(c -> c == ' ' || c == '\t').parse(s,ps);
	}
}