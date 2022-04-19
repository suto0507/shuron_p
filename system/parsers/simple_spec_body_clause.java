package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class simple_spec_body_clause implements Parser<String>{
	Parser p;
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		st = st + new spaces().parse(s, ps);
		Source s_backup = s.clone();
		try{
			Parser<String> pt = new ensures_clause();
			st = new jml_anotation_newLine(pt).parse(s, ps);
			p = pt;
		}catch (Exception e){
			s.revert(s_backup);
			Parser<String> pt = new assignable_clause();
			st = new jml_anotation_newLine(pt).parse(s, ps);
			p = pt;
		}
		
		return st;
	}
	
	Parser get_p(){
		return p;
	};

}
