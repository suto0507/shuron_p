package system;

import com.microsoft.z3.Expr;

public class Dummy_Field extends Field {
	//this�Ƃ��̂��߂Ɏg��
	Expr expr;
	
	public Dummy_Field(String class_type_name, Expr expr){
		this.class_type_name = class_type_name;
		this.expr = expr;
		dims = 0;
	}
	
	public Expr get_Expr(Check_status cs) throws Exception{
		return expr;
	}
	
}
