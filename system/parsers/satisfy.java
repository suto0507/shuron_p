package system.parsers;

import java.util.function.Function;

import system.Parser;
import system.Parser_status;
import system.Source;

public class satisfy implements Parser<Character>{
	Function<Character, Boolean> f;
	satisfy(Function<Character, Boolean> f){
		this.f = f;
	}
	
	public Character parse(Source s,Parser_status ps) throws Exception{
		char ch = s.positionChar();
		if(!f.apply(ch)){
			throw new Exception("not satisfy");
		}
		s.next();
		return ch;
	}
}
