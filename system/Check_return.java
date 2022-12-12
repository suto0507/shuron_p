package system;

import java.util.ArrayList;

import com.microsoft.z3.*;

public class Check_return {
	public Expr expr;
	public Field field;
	public ArrayList<IntExpr> indexs;
	public Expr class_expr;
	public Check_return(Expr expr, Field field, ArrayList<IntExpr> indexs, Expr class_expr){
		this.expr = expr;
		this.field = field;
		this.indexs = indexs;
		this.class_expr = class_expr;
	}
}
