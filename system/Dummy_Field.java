package system;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;

public class Dummy_Field extends Field {
	//this‚Æ‚©‚Ì‚½‚ß‚ÉŽg‚¤
	Expr expr;
	
	public Dummy_Field(String type, Expr expr){
		this.type = type;
		this.expr = expr;
		dims = 0;
		this.model_fields = new ArrayList<Model_Field>();
	}
	
	public Expr get_Expr(Check_status cs) throws Exception{
		return expr;
	}
	
}
