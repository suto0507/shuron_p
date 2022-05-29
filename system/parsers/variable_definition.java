package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class variable_definition implements Parser<String>{
	String st;
	public modifiers modifiers;
	public variable_decls variable_decls;
	public String class_type_name;
	
	
	public String parse(Source s,Parser_status ps)throws Exception{
		this.st = "";
		this.modifiers = new modifiers();
		this.st = this.st + modifiers.parse(s, ps);
		this.st = this.st + new spaces().parse(s, ps);
		this.variable_decls = new variable_decls();
		this.st = this.st + variable_decls.parse(s, ps);
		this.st = this.st + new spaces().parse(s, ps);
		this.st = this.st + new string(";").parse(s, ps);
		
		this.class_type_name = ps.class_type_name;
		
		return st;
	}

}
