package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class cha implements Parser<Character>{
	Character ch;
	cha(Character c){
		this.ch = c;
	}
	
	public Character parse(Source s,Parser_status ps) throws Exception{
		return new satisfy(c->c==ch).parse(s,ps);
	}
}