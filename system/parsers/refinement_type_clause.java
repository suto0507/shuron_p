package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class refinement_type_clause implements Parser<String>{
	
	public String ident;
	public refinement_type refinement_type;
	
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		st = st + new spaces().parse(s, ps);
		st = st + new string("refinement_type").parse(s, ps);
		st = st + new spaces().parse(s, ps);
		Source s_backup = s.clone();
		try{
			this.ident = new ident().parse(s, ps);
			st = st + this.ident;
		}catch (Exception e){
			s.revert(s_backup);
			this.refinement_type = new refinement_type();
			st = st + this.refinement_type.parse(s, ps);
		}
		
		return st;
	}
}
