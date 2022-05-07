package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class override_refinement_type_clause {
	String st;
	String ident;
	type_or_refinement_type type_or_refinement_type;
	param_override_list param_override_list;
	
	public String parse(Source s,Parser_status ps)throws Exception{
		this.st = "";
		this.st = this.st + new string("override refinement type").parse(s,ps);
		this.st = this.st + new spaces().parse(s,ps);
		this.type_or_refinement_type = new type_or_refinement_type();
		this.st = this.st + type_or_refinement_type.parse(s,ps);
		this.st = this.st + new spaces().parse(s,ps);
		this.ident = new ident().parse(s, ps);
		this.st = this.st + this.ident;
		
		Source s_backup = s.clone();
		try{
			String st2 = "";
			st2 = st2 + new spaces().parse(s,ps);
			st2 = st2 + new string("(").parse(s,ps);
			st2 = st2 + new spaces().parse(s,ps);
			
			Source s_backup2 = s.clone();
			try{
				String st3 = "";
				param_override_list pol = new param_override_list();
				st3 = st3 + pol.parse(s,ps);
				this.param_override_list = pol;
				st3 = st3 + new spaces().parse(s,ps);
				
				st2 = st2 + st3;
			}catch (Exception e){
				s.revert(s_backup2);
			}
			
			st2 = st2 + new string(")").parse(s,ps);
			st2 = st2 + new spaces().parse(s,ps);
			st2 = st2 + new string(";").parse(s,ps);
			
			st = st + st2;
		}catch (Exception e){
			s.revert(s_backup);
		}
		
		return this.st;
	}
}
