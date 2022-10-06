package system.parsers;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;

import system.Check_status;
import system.Parser;
import system.Parser_status;
import system.Source;

public class invariant implements Parser<String>{
	String st;
	boolean is_private;
	predicate predicate;
	
	public String class_type_name;
	
	public String parse(Source s,Parser_status ps)throws Exception{
		this.st = "";
		st = st + new string("invariant").parse(s,ps);
		st = st + new spaces().parse(s,ps);
		this.predicate = new predicate();
		st = st + predicate.parse(s,ps);
		st = st + new spaces().parse(s,ps);
		st = st + new string(";").parse(s,ps);
		
		this.class_type_name = ps.class_type_name;
		
		return st;
	}
	
	void set_is_private(boolean b){
		this.is_private = b;
	}
	
	public Expr check(Check_status cs) throws Exception{
		String pre_class_type_name = cs.this_field.type;
		cs.this_field.type = this.class_type_name;
		
		boolean pre_use_only_helper_method = cs.use_only_helper_method;
		cs.use_only_helper_method = true;
		
		BoolExpr ret_val =  this.predicate.check(cs);
		
		cs.this_field.type = pre_class_type_name;
		cs.use_only_helper_method = pre_use_only_helper_method;
		
		return ret_val;
	}

}
