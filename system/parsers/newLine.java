package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class newLine implements Parser<Character>{
	public Character parse(Source s,Parser_status ps)throws Exception{
		new spaces().parse(s,ps);
		return new satisfy(c -> c == '\n').parse(s,ps);
	}
}
