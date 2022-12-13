package system;

import java.util.ArrayList;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;

public class Helper_assigned_field {
	public BoolExpr assigned_pathcondition;
	public Field field;
	public Expr class_object_expr;
	
	public Helper_assigned_field(BoolExpr assigned_pathcondition, Field field, Expr class_object_expr){
		this.assigned_pathcondition = assigned_pathcondition;
		this.field = field;
		this.class_object_expr = class_object_expr;
	}
}
