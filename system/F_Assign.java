package system;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;

public class F_Assign{
	public Field field;//代入できるフィールド
	public List<Pair<BoolExpr,List<Pair<Expr, List<IntExpr>>>>> cnst_array;//配列の要素に代入する条件とそのインデックス
	
	public F_Assign(Field f, List<Pair<BoolExpr,List<Pair<Expr, List<IntExpr>>>>> b_is){
		field = f;
		cnst_array = b_is;
	}
	
	public BoolExpr assign_index_expr(Expr class_expr, List<IntExpr> index_expr, Check_status cs){
		BoolExpr equal_cnsts = cs.ctx.mkBool(false);
		BoolExpr not_equal_cnsts = cs.ctx.mkBool(true);
		for(Pair<BoolExpr,List<Pair<Expr, List<IntExpr>>>> assinable_cnst_index :cnst_array){
			BoolExpr equal = cs.ctx.mkBool(false);
			for(Pair<Expr, List<IntExpr>> expr_index : assinable_cnst_index.snd){
				if(expr_index.snd.size()==0 && expr_index.snd.size() == index_expr.size()){
					equal = cs.ctx.mkOr(equal, cs.ctx.mkEq(expr_index.fst, class_expr));//class_exprと一致するものだけ考える
				}else if(expr_index.snd.size()>0 && expr_index.snd.size() == index_expr.size()){//同じ次元への代入だけ考える
					BoolExpr index_equal = null;
					for(int i = 0; i<expr_index.snd.size(); i++){
						if(index_equal == null){
							index_equal = cs.ctx.mkEq(index_expr.get(i), expr_index.snd.get(i));
						}else{
							index_equal = cs.ctx.mkAnd(index_equal, cs.ctx.mkEq(index_expr.get(i), expr_index.snd.get(i)));
						}
					}
					index_equal = cs.ctx.mkAnd(index_equal, cs.ctx.mkEq(expr_index.fst, class_expr));//class_exprと一致するものだけ考える
					equal = cs.ctx.mkOr(equal, index_equal);
				}
			}
			BoolExpr not_equal = cs.ctx.mkNot(equal);
			equal_cnsts = cs.ctx.mkOr(equal_cnsts, cs.ctx.mkAnd(equal, assinable_cnst_index.fst));
			not_equal_cnsts = cs.ctx.mkAnd(not_equal_cnsts, cs.ctx.mkImplies(not_equal, cs.ctx.mkNot(assinable_cnst_index.fst)));
			
		}
		
		return cs.ctx.mkAnd(equal_cnsts, not_equal_cnsts);
	}
	
	public void assign_fresh_value(Check_status cs) throws Exception{
		for(Pair<BoolExpr,List<Pair<Expr, List<IntExpr>>>> ca: cnst_array){
			for(Pair<Expr, List<IntExpr>> expr_indexs: ca.snd){
				if(expr_indexs.snd.size()>0){
					Expr expr;
					if(field instanceof Variable){
						expr = field.get_Expr(cs);
					}else{
						expr = cs.ctx.mkSelect(field.get_Expr(cs), expr_indexs.fst);
					}
					for(int i = 0; i < expr_indexs.snd.size()-1; i++){
						expr = cs.array_arrayref.index_access_array(expr, expr_indexs.snd.get(i), cs);
					}
					Array array = null;
					if(expr_indexs.snd.size()<field.dims){
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
					Expr pre_value = array.index_access_array(expr,  expr_indexs.snd.get(expr_indexs.snd.size()-1), cs);
					Expr fresh_value = cs.ctx.mkITE(ca.fst, cs.ctx.mkConst("freshValue_" + cs.Check_status_share.get_tmp_num(), array.elements_Sort), pre_value);
					array.update_array(expr, expr_indexs.snd.get(expr_indexs.snd.size()-1), fresh_value, cs);
					
					//配列への代入でも、modelフィールドは変わる
					for(Model_Field mf : field.model_fields){
						mf.tmp_plus_with_data_group(expr_indexs.fst, cs);
					}
				}else{
					Expr expr;
					if(field instanceof Variable){
						Expr pre_expr = field.get_Expr(cs);
						expr = cs.ctx.mkITE(ca.fst, field.get_fresh_value(cs), pre_expr);
					}else{
						Expr pre_expr = cs.ctx.mkSelect(field.get_Expr(cs), expr_indexs.fst);
						expr = cs.ctx.mkStore(field.get_Expr(cs), expr_indexs.fst, cs.ctx.mkITE(ca.fst, field.get_fresh_value(cs), pre_expr));
					}
					cs.add_constraint(cs.ctx.mkEq(field.get_Expr_assign(cs), expr));
					field.tmp_plus_with_data_group(expr_indexs.fst, cs);
				}
			}
		}
		
	}
}
