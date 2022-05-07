package system.parsers;

import java.util.ArrayList;
import java.util.List;

import system.Pair;
import system.Parser_status;
import system.Source;

public class param_override_list {
	List<Pair<String, type_or_refinement_type>> param_overrides;
	String st;
	
	public String parse(Source s,Parser_status ps)throws Exception{
		param_overrides = new ArrayList<Pair<String, type_or_refinement_type>>();
		
		type_or_refinement_type type_or_refinement_type = new type_or_refinement_type();
		this.st = this.st + type_or_refinement_type.parse(s,ps);
		this.st = this.st + new spaces().parse(s,ps);
		String ident = new ident().parse(s, ps);
		this.st = this.st + ident;
		param_overrides.add(new Pair<String, type_or_refinement_type>(ident, type_or_refinement_type));
		
		Source s_backup = s.clone();
		try{
			while(true){
				s_backup = s.clone();
				String st2 = "";
				
				st2 = st2 + new spaces().parse(s,ps);
				st2 = st2 + new string(",").parse(s,ps);
				st2 = st2 + new spaces().parse(s,ps);
				
				type_or_refinement_type = new type_or_refinement_type();
				st2 = st2 + type_or_refinement_type.parse(s,ps);
				st2 = st2 + new spaces().parse(s,ps);
				ident = new ident().parse(s, ps);
				st2 = st2 + ident;
				param_overrides.add(new Pair<String, type_or_refinement_type>(ident, type_or_refinement_type));
				
				st = st + st2;
			}
		}catch (Exception e){
			s.revert(s_backup);
		}
		
		return this.st;
	}
	
}
