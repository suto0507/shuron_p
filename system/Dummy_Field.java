package system;

import com.microsoft.z3.Expr;

public class Dummy_Field extends Field {
	//this‚Æ‚©‚Ì‚½‚ß‚ÉŽg‚¤
	Expr expr;
	
	public Dummy_Field(String type, Expr expr){
		this.type = type;
		this.expr = expr;
		dims = 0;
	}
	
	public Expr get_Expr(Check_status cs) throws Exception{
		return expr;
	}
	
}
