package system;

import java.util.ArrayList;

import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.Sort;

public class Array {
	//�z���Ref����z������o��
	public Expr array;
	public Sort elements_Sort;

	Array(Sort elements_Sort, Check_status cs){
		this.elements_Sort = elements_Sort;
		String ret = "Array" + "_" + cs.Check_status_share.get_tmp_num();
		array = cs.ctx.mkArrayConst(ret, cs.ctx.mkUninterpretedSort("Array"), cs.ctx.mkArraySort(cs.ctx.mkIntSort(), elements_Sort));
	}
	
	Expr index_access_array(Expr class_expr, IntExpr index, Check_status cs){
		return cs.ctx.mkSelect(cs.ctx.mkSelect(array, class_expr), index);
	}
	
	Expr get_array(Expr class_expr, IntExpr index, Check_status cs){
		return cs.ctx.mkSelect(array, class_expr);
	}
	
	//�z�񎩑̂��ۂ��ƍX�V
	void update_array(Expr class_expr, Expr value, Check_status cs) throws Exception{
		String ret = "Array" + "_" + cs.Check_status_share.get_tmp_num();
		Expr expr = cs.ctx.mkArrayConst(ret, cs.ctx.mkUninterpretedSort("Array"), cs.ctx.mkArraySort(cs.ctx.mkIntSort(), cs.ctx.mkUninterpretedSort("Ref")));
		cs.add_constraint(cs.ctx.mkEq(expr, cs.ctx.mkStore(array, class_expr, value)));
		array = expr;
	}
	
	//�z��̒��g�́A����̗v�f�����X�V
	void update_array(Expr class_expr, IntExpr index, Expr value, Check_status cs) throws Exception{
		String ret = "Array" + "_" + cs.Check_status_share.get_tmp_num();
		Expr expr = cs.ctx.mkArrayConst(ret, cs.ctx.mkUninterpretedSort("Array"), cs.ctx.mkArraySort(cs.ctx.mkIntSort(), cs.ctx.mkUninterpretedSort("Ref")));
		Expr new_array_value = cs.ctx.mkStore(cs.ctx.mkSelect(array, class_expr), index, value);
		cs.add_constraint(cs.ctx.mkEq(expr, cs.ctx.mkStore(array, class_expr, new_array_value)));
		array = expr;
	}
	
}
