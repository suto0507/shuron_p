package system;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;

public class F_Assign{
	public Field field;//代入できるフィールド
	public List<Pair<BoolExpr,List<List<IntExpr>>>> cnst_array;//配列の要素に代入する条件とそのインデックス
	
	public F_Assign(Field f, List<Pair<BoolExpr,List<List<IntExpr>>>> b_is){
		field = f;
		cnst_array = b_is;
	}
	
	public BoolExpr assign_index_expr(List<IntExpr> index_expr, Check_status cs){
		BoolExpr equal_cnsts = cs.ctx.mkBool(false);
		BoolExpr not_equal_cnsts = cs.ctx.mkBool(true);
		for(Pair<BoolExpr,List<List<IntExpr>>> assinable_cnst_index :cnst_array){
			if(index_expr.size() == 0){
				for(List<IntExpr> index : assinable_cnst_index.snd){
					if(index.size()==0){//配列へのインデックスの参照がない場合
						equal_cnsts = cs.ctx.mkOr(assinable_cnst_index.fst);
						not_equal_cnsts = cs.ctx.mkAnd(cs.ctx.mkNot(assinable_cnst_index.fst));
					}
				}
			}else{
				BoolExpr equal = cs.ctx.mkBool(false);
				for(List<IntExpr> index : assinable_cnst_index.snd){
					if(index.size()>0 && index.size() == index_expr.size()){//同じ次元への代入だけ考える
						BoolExpr index_equal = null;
						for(int i = 0; i<index.size(); i++){
							if(index_equal == null){
								index_equal = cs.ctx.mkEq(index_expr.get(i), index.get(i));
							}else{
								index_equal = cs.ctx.mkAnd(index_equal, cs.ctx.mkEq(index_expr.get(i), index.get(i)));
							}
						}
						equal = cs.ctx.mkOr(equal, index_equal);
					}
				}
				BoolExpr not_equal = cs.ctx.mkNot(equal);
				equal_cnsts = cs.ctx.mkOr(equal_cnsts, cs.ctx.mkAnd(equal, assinable_cnst_index.fst));
				not_equal_cnsts = cs.ctx.mkAnd(not_equal_cnsts, cs.ctx.mkImplies(not_equal, cs.ctx.mkNot(assinable_cnst_index.fst)));
			}
		}
		
		return cs.ctx.mkAnd(equal_cnsts, not_equal_cnsts);
	}
	
	public void assign_fresh_value(Check_status cs) throws Exception{
		Expr expr = null;
		for(Pair<BoolExpr,List<List<IntExpr>>> ca: cnst_array){
			for(List<IntExpr> indexs: ca.snd){
				Expr full_expr = field.get_full_Expr((ArrayList<IntExpr>) ((ArrayList<IntExpr>) indexs).clone(), cs);
				expr = cs.ctx.mkITE(ca.fst, cs.ctx.mkConst("freshValue_" + cs.Check_status_share.get_tmp_num(), full_expr.getSort()), full_expr);//条件を満たす場合には新しい値を入れる
				for(int i = indexs.size()-1; i >= field.class_object_dims_sum(); i--){
					IntExpr index = indexs.get(i);
					ArrayList<IntExpr> indexs_sub = new ArrayList<IntExpr>(indexs.subList(0, i));
					ArrayExpr array = (ArrayExpr) field.get_full_Expr((ArrayList<IntExpr>) indexs_sub.clone(), cs);
					ArrayExpr array_assign = (ArrayExpr) field.get_full_Expr_assign((ArrayList<IntExpr>) indexs_sub.clone(), cs);
					expr = cs.ctx.mkStore(array, index, expr);
					
					//長さに関する制約
					int array_dim = field.dims_sum() - i;
					String array_type;
					if(this.field.equals("int")){
						array_type = "int";
					}else if(this.field.equals("boolean")){
						array_type = "boolean";
					}else{
						array_type = "ref";
					}
					IntExpr length = (IntExpr) cs.ctx.mkSelect(cs.ctx.mkArrayConst("length_" + array_dim + "d_" + array_type, array.getSort(), cs.ctx.mkIntSort()), array);
					IntExpr length_assign = (IntExpr) cs.ctx.mkSelect(cs.ctx.mkArrayConst("length_" + array_dim + "d_" + array_type, array_assign.getSort(), cs.ctx.mkIntSort()), array_assign);
					
					cs.add_constraint(cs.ctx.mkEq(length, length_assign));
				}
				
				Expr class_object_expr = field.class_object.get_full_Expr((ArrayList<IntExpr>) ((ArrayList<IntExpr>) indexs).clone(), cs);
				
				BoolExpr cnst = null;
				if(field instanceof Variable){
					cnst = cs.ctx.mkEq(field.get_Expr_assign(cs),  expr);
				}else{
					cnst = cs.ctx.mkEq(field.get_Expr_assign(cs), cs.ctx.mkStore(field.get_Expr(cs), class_object_expr, expr));
				}
				
				cs.add_constraint(cnst);
				field.tmp_plus_with_data_group(class_object_expr, ca.fst, cs);
			}
		}
		
	}
}
