package system;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;

public class F_Assign{
	public Field field;//代入できるフィールド
	public List<Pair<BoolExpr,Pair<Expr, List<IntExpr>>>> cnst_array;//配列の要素に代入する条件とそのインデックス
	
	public F_Assign(Field f, List<Pair<BoolExpr,Pair<Expr, List<IntExpr>>>> b_is){
		field = f;
		cnst_array = b_is;
	}
	
	
	public void assign_fresh_value(Check_status cs) throws Exception{
		for(Pair<BoolExpr,Pair<Expr, List<IntExpr>>> ca: cnst_array){
			if(ca.snd.snd.size()>0){
				Expr expr;
				if(field instanceof Variable){
					expr = field.get_Expr(cs);
				}else{
					expr = cs.ctx.mkSelect(field.get_Expr(cs), ca.snd.fst);
				}
				for(int i = 0; i < ca.snd.snd.size()-1; i++){
					expr = cs.array_arrayref.index_access_array(expr, ca.snd.snd.get(i), cs);
				}
				Array array = null;
				if(ca.snd.snd.size()<field.dims){
					array = cs.array_arrayref;
				}else{
					if(field.type.equals("int")){
						array = cs.array_int;
					}else if(field.type.equals("boolean")){
						array = cs.array_boolean;
					}else{
						array = cs.array_ref;
					}
				}
				Expr pre_value = array.index_access_array(expr,  ca.snd.snd.get(ca.snd.snd.size()-1), cs);
				Expr fresh_value = cs.ctx.mkITE(ca.fst, cs.ctx.mkConst("freshValue_" + cs.Check_status_share.get_tmp_num(), array.elements_Sort), pre_value);
				array.update_array(expr, ca.snd.snd.get(ca.snd.snd.size()-1), fresh_value, cs);
				
				//配列への代入でも、modelフィールドは変わる
				for(Model_Field mf : field.model_fields){
					mf.tmp_plus_with_data_group(ca.snd.fst, cs);
				}
			}else{
				Expr expr;
				if(field instanceof Variable){
					Expr pre_expr = field.get_Expr(cs);
					expr = cs.ctx.mkITE(ca.fst, field.get_fresh_value(cs), pre_expr);
				}else{
					Expr pre_expr = cs.ctx.mkSelect(field.get_Expr(cs), ca.snd.fst);
					expr = cs.ctx.mkStore(field.get_Expr(cs), ca.snd.fst, cs.ctx.mkITE(ca.fst, field.get_fresh_value(cs), pre_expr));
				}
				cs.add_constraint(cs.ctx.mkEq(field.get_Expr_assign(cs), expr));
				field.tmp_plus_with_data_group(ca.snd.fst, cs);
			}
		}
		
	}
}
