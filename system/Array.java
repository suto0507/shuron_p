package system;

import java.util.ArrayList;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.Sort;

public class Array {
	//配列のRefから配列を取り出す
	public Expr array;
	public Sort elements_Sort;

	public Array(Sort elements_Sort, Check_status cs){
		this.elements_Sort = elements_Sort;
		String ret = "Array" + "_" + cs.Check_status_share.get_tmp_num();
		array = cs.ctx.mkArrayConst(ret, cs.ctx.mkUninterpretedSort("ArrayRef"), cs.ctx.mkArraySort(cs.ctx.mkIntSort(), elements_Sort));
	}
	
	public Expr index_access_array(Expr ref_expr, IntExpr index, Check_status cs){
		return cs.ctx.mkSelect(cs.ctx.mkSelect(array, ref_expr), index);
	}
	
	public Expr get_array(Expr ref_expr, IntExpr index, Check_status cs){
		return cs.ctx.mkSelect(array, ref_expr);
	}
	
	//配列自体を丸ごと更新
	public void update_array(Expr ref_expr, Expr value, Check_status cs) throws Exception{
		String ret = "Array" + "_" + cs.Check_status_share.get_tmp_num();
		Expr expr = cs.ctx.mkArrayConst(ret, cs.ctx.mkUninterpretedSort("ArrayRef"), cs.ctx.mkArraySort(cs.ctx.mkIntSort(), cs.ctx.mkUninterpretedSort("Ref")));
		cs.add_constraint(cs.ctx.mkEq(expr, cs.ctx.mkStore(array, ref_expr, value)));
		array = expr;
	}
	
	//配列の中身の、特定の要素だけ更新
	public void update_array(Expr ref_expr, IntExpr index, Expr value, Check_status cs) throws Exception{
		String ret = "Array" + "_" + cs.Check_status_share.get_tmp_num();
		Expr expr = cs.ctx.mkArrayConst(ret, cs.ctx.mkUninterpretedSort("ArrayRef"), cs.ctx.mkArraySort(cs.ctx.mkIntSort(), cs.ctx.mkUninterpretedSort("Ref")));
		Expr new_array_value = cs.ctx.mkStore(cs.ctx.mkSelect(array, ref_expr), index, value);
		cs.add_constraint(cs.ctx.mkEq(expr, cs.ctx.mkStore(array, ref_expr, new_array_value)));
		array = expr;
	}
	
	public void refresh(BoolExpr condition, Check_status cs) throws Exception{
		Expr pre_array = array;
		
		String fresh_ret = "fresh_array" + "_" + cs.Check_status_share.get_tmp_num();
		Expr fresh_array = cs.ctx.mkArrayConst(fresh_ret, cs.ctx.mkUninterpretedSort("ArrayRef"), cs.ctx.mkArraySort(cs.ctx.mkIntSort(), elements_Sort));
		
		String ret = "Array" + "_" + cs.Check_status_share.get_tmp_num();
		array = cs.ctx.mkArrayConst(ret, cs.ctx.mkUninterpretedSort("ArrayRef"), cs.ctx.mkArraySort(cs.ctx.mkIntSort(), elements_Sort));
		
		cs.add_constraint(cs.ctx.mkEq(array, cs.ctx.mkITE(condition, fresh_array, pre_array)));
	}
	
}
