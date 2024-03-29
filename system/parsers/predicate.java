package system.parsers;

import com.microsoft.z3.BoolExpr;

import system.Check_status;
import system.Parser;
import system.Parser_status;
import system.Source;

public class predicate implements Parser<String>{
	spec_expression spec_expression;
	
	public String parse(Source s,Parser_status ps)throws Exception{
		this.spec_expression = new spec_expression();
		return spec_expression.parse(s,ps);
	}
	
	public BoolExpr check(Check_status cs) throws Exception{
		boolean pre_in_jml_predicate = cs.in_jml_predicate;
		cs.in_jml_predicate = true;
		
		BoolExpr expr = (BoolExpr)this.spec_expression.check(cs).expr;
		
		cs.in_jml_predicate = pre_in_jml_predicate;
		
		return expr;
	}
	
	
	public boolean have_index_access(Check_status cs){
		return spec_expression.have_index_access(cs);
	}
	
	

}

