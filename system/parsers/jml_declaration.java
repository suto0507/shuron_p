package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class jml_declaration implements Parser<Parser>{
	Parser p;
	
	public Parser parse(Source s,Parser_status ps)throws Exception{
		Source s_backup = s.clone();
		try{
			p = new def_type_clause();
			new my_jml_anotation_newLine(p).parse(s,ps);
		}catch (Exception e){
			s.revert(s_backup);
			jml_declaration_invariant supp = new jml_declaration_invariant();
			new jml_anotation_newLine(supp).parse(s,ps);
			p = supp.return_p();
		}
		return p;
	}
	
	class jml_declaration_invariant implements Parser<String>{
		Parser p;
		
		public String parse(Source s,Parser_status ps)throws Exception{
			String st = "";
			Source s_backup = s.clone();
			boolean is_private = false;
			try{
				new string("private").parse(s,ps);
				new spaces().parse(s,ps);
				is_private = true;
				st = st + "private ";
				
			}catch (Exception e2){
				s.revert(s_backup);
			}
			p = new invariant();
			st = st + p.parse(s,ps);
			((invariant)p).set_is_private(is_private);
			
			return st;
		}
		
		Parser return_p(){
			return p;
		}
	}

}

