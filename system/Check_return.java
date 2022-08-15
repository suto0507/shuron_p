package system;

import java.util.ArrayList;

import com.microsoft.z3.*;

public class Check_return {
	public Expr expr;
	public Field field;
	public ArrayList<IntExpr> indexs;
	public Check_return(Expr expr, Field field, ArrayList<IntExpr> indexs){
		this.expr = expr;
		this.field = field;
		this.indexs = indexs;
	}
}
