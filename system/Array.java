package system;

import java.util.ArrayList;

import com.microsoft.z3.ArrayExpr;
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
	
	public Array clone(Check_status cs){
		Array new_array = new Array(this.elements_Sort, cs);
		new_array.array = this.array;
		return new_array;
	}
	
	public Expr index_access_array(Expr ref_expr, IntExpr index, Check_status cs) throws Exception{
		Expr ret =  cs.ctx.mkSelect(cs.ctx.mkSelect(array, ref_expr), index);
		if(ret.getSort().equals(cs.ctx.mkUninterpretedSort("ArrayRef"))){
			ArrayExpr dim_expr = cs.ctx.mkArrayConst("dim_array", cs.ctx.mkUninterpretedSort("ArrayRef"), cs.ctx.mkIntSort());
			cs.add_constraint(cs.ctx.mkEq(cs.ctx.mkSelect(dim_expr, ret), cs.ctx.mkSub(cs.ctx.mkSelect(dim_expr, ref_expr), cs.ctx.mkInt(1))));
			ArrayExpr type_expr = cs.ctx.mkArrayConst("type_array", cs.ctx.mkUninterpretedSort("ArrayRef"), cs.ctx.mkStringSort());
			cs.add_constraint(cs.ctx.mkEq(cs.ctx.mkSelect(type_expr, ret), cs.ctx.mkSelect(type_expr, ref_expr)));
		}
		
		return ret;
	}
	
	public Expr get_array(Expr ref_expr, IntExpr index, Check_status cs){
		return cs.ctx.mkSelect(array, ref_expr);
	}
	
	//配列自体を丸ごと更新
	public void update_array(Expr ref_expr, Expr value, Check_status cs) throws Exception{
		String ret = "Array" + "_" + cs.Check_status_share.get_tmp_num();
		Expr expr = cs.ctx.mkArrayConst(ret, cs.ctx.mkUninterpretedSort("ArrayRef"), cs.ctx.mkArraySort(cs.ctx.mkIntSort(), elements_Sort));
		cs.add_constraint(cs.ctx.mkEq(expr, cs.ctx.mkStore(array, ref_expr, value)));
		array = expr;
	}
	
	//配列の中身の、特定の要素だけ更新
	public void update_array(Expr ref_expr, IntExpr index, Expr value, Check_status cs) throws Exception{
		String ret = "Array" + "_" + cs.Check_status_share.get_tmp_num();
		Expr expr = cs.ctx.mkArrayConst(ret, cs.ctx.mkUninterpretedSort("ArrayRef"), cs.ctx.mkArraySort(cs.ctx.mkIntSort(), elements_Sort));
		Expr new_array_value = cs.ctx.mkStore(cs.ctx.mkSelect(array, ref_expr), index, value);
		cs.add_constraint(cs.ctx.mkEq(expr, cs.ctx.mkStore(array, ref_expr, new_array_value)));
		array = expr;
	}
	
	//分岐後のマージ
	public void merge_array(BoolExpr condition, Array array1, Array array2, Check_status cs) throws Exception{
		Expr new_array = cs.ctx.mkITE(condition, array1.array, array2.array);
		
		String ret = "Array" + "_" + cs.Check_status_share.get_tmp_num();
		array = cs.ctx.mkArrayConst(ret, cs.ctx.mkUninterpretedSort("ArrayRef"), cs.ctx.mkArraySort(cs.ctx.mkIntSort(), elements_Sort));
		
		cs.add_constraint(cs.ctx.mkEq(array, new_array));
	}
	
	public void refresh(BoolExpr condition, Check_status cs) throws Exception{
		Expr pre_array = array;
		
		String fresh_ret = "fresh_array" + "_" + cs.Check_status_share.get_tmp_num();
		Expr fresh_array = cs.ctx.mkArrayConst(fresh_ret, cs.ctx.mkUninterpretedSort("ArrayRef"), cs.ctx.mkArraySort(cs.ctx.mkIntSort(), elements_Sort));
		
		String ret = "Array" + "_" + cs.Check_status_share.get_tmp_num();
		array = cs.ctx.mkArrayConst(ret, cs.ctx.mkUninterpretedSort("ArrayRef"), cs.ctx.mkArraySort(cs.ctx.mkIntSort(), elements_Sort));
		
		cs.add_constraint(cs.ctx.mkEq(array, cs.ctx.mkITE(condition, fresh_array, pre_array)));
	}
	
	public void refresh(Check_status cs) throws Exception{
		String ret = "Array" + "_" + cs.Check_status_share.get_tmp_num();
		array = cs.ctx.mkArrayConst(ret, cs.ctx.mkUninterpretedSort("ArrayRef"), cs.ctx.mkArraySort(cs.ctx.mkIntSort(), elements_Sort));
	}
	
}
