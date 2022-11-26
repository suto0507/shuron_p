package system.parsers;

import system.Check_status;
import system.Parser;
import system.Parser_status;
import system.Source;

public class represents_clause implements Parser<String>{
	String st;
	public boolean is_private;
	//store_ref_expression store_ref_expression;
	public String ident;
	public spec_expression spec_expression;
	
	public String class_type_name;
	
	public String parse(Source s,Parser_status ps)throws Exception{
		this.st = "";
		st = st + new string("represents").parse(s,ps);
		st = st + new spaces().parse(s,ps);
		//this.store_ref_expression = new store_ref_expression(); 
		//st = st + store_ref_expression.parse(s,ps);
		this.ident = new ident().parse(s, ps);
		st = st + this.ident;
		st = st + new spaces().parse(s,ps);
		st = st + new string("=").parse(s, ps);
		st = st + new spaces().parse(s,ps);
		this.spec_expression = new spec_expression();
		st = st + spec_expression.parse(s,ps);
		st = st + new spaces().parse(s,ps);
		st = st + new string(";").parse(s,ps);
		
		this.class_type_name = ps.class_type_name;
		
		return st;
	}
}
