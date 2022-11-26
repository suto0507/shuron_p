package system;

import java.util.ArrayList;

import com.microsoft.z3.Expr;

public class Quantifier_Variable extends Variable{
	Expr quantifier_expr;
	Quantifier_Variable(String field_name, Expr expr){
		this.field_name = field_name;
		this.quantifier_expr = expr;
		
		this.model_fields = new ArrayList<Model_Field>();
	}
	
	@Override
	public Expr get_Expr(Check_status cs) throws Exception{
		return quantifier_expr;
	}
}