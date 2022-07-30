package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class assignment_op implements Parser<String>{
	public String parse(Source s,Parser_status ps)throws Exception{
		if(ps.in_jml){
			throw new Exception("can't use = in jml");
		}
		return new string("=").parse(s, ps);
	}
}