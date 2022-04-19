package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class member_decl implements Parser<Parser>{
	Parser p;
	
	public Parser parse(Source s,Parser_status ps)throws Exception{
		Source s_backup = s.clone();
		try{
			p = new method_decl();
			((method_decl)p).parse(s,ps);
		}catch (Exception e){
			s.revert(s_backup);
			p = new variable_definition();
			((variable_definition)p).parse(s,ps);
		}
		
		return p;
	}

}