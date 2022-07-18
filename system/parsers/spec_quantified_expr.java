package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class spec_quantified_expr implements Parser<String>{
	quantifier quantifier;
	quantified_var_decls quantified_var_decls;
	predicate guard;
	spec_expression body;
	
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		st += new string("(").parse(s, ps);
		st += new spaces().parse(s, ps);
		
		quantifier = new quantifier();
		st += quantifier.parse(s, ps);
		st += new spaces().parse(s, ps);
		
		quantified_var_decls = new quantified_var_decls();
		st += quantified_var_decls.parse(s, ps);
		st += new spaces().parse(s, ps);
		
		Source s_backup = s.clone();
		try{
			String st2 = "";
			predicate predicate = new predicate();
			st2 += predicate.parse(s, ps);
			st2 += new spaces().parse(s, ps);
			st2 += new string(";").parse(s, ps);
			st2 += new spaces().parse(s, ps);
			this.guard = predicate;
			st += st2;
		}catch (Exception e){
			s.revert(s_backup);
		}
		
		body = new spec_expression();
		st += body.parse(s, ps);
		st += new spaces().parse(s, ps);
		
		st += new string(")").parse(s, ps);
		
		return st;
	}
}
