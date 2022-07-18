package system.parsers;

import java.util.ArrayList;
import java.util.List;

import system.Parser;
import system.Parser_status;
import system.Source;

public class quantified_var_decls implements Parser<String>{
	type_spec type_spec;
	List<quantified_var_declarator> quantified_var_declarators;
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		
		quantified_var_declarators = new ArrayList<quantified_var_declarator>();
		
		type_spec = new type_spec();
		st += type_spec.parse(s, ps);
		
		st += new spaces().parse(s, ps);
		
		quantified_var_declarator qvd = new quantified_var_declarator();
		st += qvd.parse(s, ps);
		quantified_var_declarators.add(qvd);
		
		Source s_backup = s.clone();
		try {
			while(true){
				String st2 = "";
				s_backup = s.clone();
				
				st2 += new spaces().parse(s, ps);
				st2 += new string(",").parse(s, ps);
				st2 += new spaces().parse(s, ps);
				
				qvd = new quantified_var_declarator();
				st2 += qvd.parse(s, ps);
				quantified_var_declarators.add(qvd);
				
				st += st2;
			}
		}catch (Exception e2){
			s.revert(s_backup);
		}
		
		return st;
	}
}
