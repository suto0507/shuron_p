package system.parsers;

import java.util.ArrayList;

import system.Parser;
import system.Parser_status;
import system.Source;

public class variable_definition implements Parser<String>{
	String st;
	public modifiers modifiers;
	public variable_decls variable_decls;
	public String class_type_name;
	public ArrayList<group_name> group_names;
	
	public String parse(Source s,Parser_status ps)throws Exception{
		this.st = "";
		this.modifiers = new modifiers();
		this.st = this.st + modifiers.parse(s, ps);
		this.st = this.st + new spaces().parse(s, ps);
		this.variable_decls = new variable_decls();
		this.st = this.st + variable_decls.parse(s, ps);
		this.st = this.st + new spaces().parse(s, ps);
		st = st + new string(";").parse(s, ps);
		Source s_backup = s.clone();
		try {
			while(true){
				s_backup = s.clone();
				String st2 = new spaces().parse(s, ps);
				jml_data_group_clause jdgc = new jml_data_group_clause();
				st2 = st2 + jdgc.parse(s, ps);
				st = st + st2;
				this.group_names.addAll(jdgc.group_names);
			}
		}catch (Exception e){
			s.revert(s_backup);
		}
		
		this.class_type_name = ps.class_type_name;
		
		return st;
	}

}
