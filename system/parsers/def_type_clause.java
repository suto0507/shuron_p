package system.parsers;

import system.Check_status;
import system.Parser;
import system.Parser_status;
import system.Source;

public class def_type_clause implements Parser<String>{
	String st;
	String ident;
	refinement_type refinement_type;
	
	public String parse(Source s,Parser_status ps)throws Exception{
		this.st = "";
		this.st = this.st + new string("def_type").parse(s,ps);
		this.st = this.st + new spaces().parse(s,ps);
		this.ident = new ident().parse(s,ps);
		this.st = this.st + this.ident;
		this.st = this.st + new spaces().parse(s,ps);
		this.st = this.st + new string("=").parse(s, ps);
		this.st = this.st + new spaces().parse(s,ps);
		this.refinement_type = new refinement_type();
		this.st = this.st + this.refinement_type.parse(s,ps);
		this.st = this.st + new spaces().parse(s,ps);
		this.st = this.st + new string(";").parse(s,ps);
		
		return this.st;
	}
	
	public void check(Check_status cs){
		cs.add_refinement_type(this.refinement_type.ident, this.refinement_type.predicates, this.refinement_type.type.type, ident);
	}

}
