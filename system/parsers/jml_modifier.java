package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class jml_modifier implements Parser<String>{
	
	public String parse(Source s,Parser_status ps)throws Exception{
		return new string("spec_public").parse(s,ps);
	}

}
