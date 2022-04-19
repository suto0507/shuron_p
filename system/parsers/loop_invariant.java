package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class loop_invariant implements Parser<String>{
	predicates predicates;
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		st = st + new maintaining_keyword().parse(s,ps);
		st = st + new spaces().parse(s, ps);
		this.predicates = new predicates();
		st = st + this.predicates.parse(s, ps);
		st = st + new spaces().parse(s, ps);
		st = st + new string(";").parse(s, ps);
		
		return st;
	}
}
