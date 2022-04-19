package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class letter implements Parser<Character>{
	
	public Character parse(Source s,Parser_status ps) throws Exception{
		return new satisfy(c -> c == '_' || c == '$' || 0<=c.compareTo('a') && c.compareTo('z')<=0 || 0<=c.compareTo('A') && c.compareTo('Z')<=0).parse(s,ps);
	}
}