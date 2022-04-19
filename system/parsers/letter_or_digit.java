package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class letter_or_digit implements Parser<Character>{
	public Character parse(Source s,Parser_status ps) throws Exception{
		Character c;
		Source s_backup = s.clone();
		try{
			c = new letter().parse(s,ps);
		}catch (Exception e){
			s.revert(s_backup);
			c = new digit().parse(s,ps);
		}
		return c;
	}
}
