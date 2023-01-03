package system;

import java.util.ArrayList;

import com.microsoft.z3.*;

public class Check_return {
	public Expr expr;
	public Field field;
	public ArrayList<IntExpr> indexs;
	public Expr class_expr;
	public Type_info type_info;
	public Check_return(Expr expr, Field field, ArrayList<IntExpr> indexs, Expr class_expr, String type, int type_dims){
		this.expr = expr;
		this.field = field;
		this.indexs = indexs;
		this.class_expr = class_expr;
		this.type_info = new Type_info(type, type_dims);
	}
	
	public Check_return(Expr expr, Field field, ArrayList<IntExpr> indexs, Expr class_expr, Type_info type_info){
		this.expr = expr;
		this.field = field;
		this.indexs = indexs;
		this.class_expr = class_expr;
		this.type_info = type_info;
	}
}

