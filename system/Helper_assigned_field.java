package system;

import java.util.ArrayList;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;

public class Helper_assigned_field {
	public BoolExpr assigned_pathcondition;
	public Field field;
	public Expr class_object_expr;
	public ArrayList<IntExpr> indexs;//class_object‚Ì•”•ª‚Ü‚Å
	
	public Helper_assigned_field(BoolExpr assigned_pathcondition, Field field, Expr class_object_expr, ArrayList<IntExpr> indexs){
		this.assigned_pathcondition = assigned_pathcondition;
		this.field = field;
		this.class_object_expr = class_object_expr;
		this.indexs= indexs;
	}
}
