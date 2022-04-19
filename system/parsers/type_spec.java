package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class type_spec implements Parser<String>{
	public type type;
	public int dims;
	public refinement_type_clause refinement_type_clause;
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		type = new type();
		st = st + type.parse(s, ps);
		Source s_backup = s.clone();
		try{
			dims = new dims().parse(s, ps);
			st = st + "[]";
		}catch (Exception e){
			s.revert(s_backup);
		}	
		st = st + new spaces().parse(s, ps);
		s_backup = s.clone();
		try{
			refinement_type_clause rt = new refinement_type_clause();
			st = st + new my_jml_anotation(rt).parse(s, ps);
			this.refinement_type_clause = rt;
		}catch (Exception e){
			s.revert(s_backup);
		}	
		
		return st;
	}	
}
