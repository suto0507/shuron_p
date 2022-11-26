package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class field implements Parser<Parser>{
	Parser p;
	
	public Parser parse(Source s,Parser_status ps)throws Exception{
		new spaces().parse(s,ps);
		
		Source s_backup = s.clone();
		try{
			jml_declaration jd = new jml_declaration();
			p = jd.parse(s,ps);
		}catch (Exception e){
			s.revert(s_backup);
			member_decl md = new member_decl();
			p = md.parse(s,ps);
		}
		
		new newLines().parse(s,ps);
		
		return p;
	}

}
