package system.parsers;

import java.util.ArrayList;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.Status;

import system.Check_status;
import system.Field;
import system.Pair;
import system.Parser;
import system.Parser_status;
import system.Source;
import system.Variable;

public class assignment_expr implements Parser<String>{
	implies_expr implies_expr;
	postfix_expr postfix_expr;
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		Source s_backup = s.clone();
		try{
			String st2 = "";
			postfix_expr pe = new postfix_expr();
			st2 = st2 + pe.parse(s, ps);
			st2 = st2 + new spaces().parse(s, ps);
			st2 = st2 + new assignment_op().parse(s, ps);
			st2 = st2 + new spaces().parse(s, ps);
			implies_expr ie = new implies_expr();
			st2 = st2 + ie.parse(s, ps);
			this.postfix_expr = pe;
			this.implies_expr = ie;
			st = st + st2;
		}catch (Exception e){
			s.revert(s_backup);
			this.implies_expr = new implies_expr();
			st = st + this.implies_expr.parse(s, ps);
		}
		
		
		return st;
	}
	
	public Expr check(Check_status cs) throws Exception{
		Field v = null;
		Expr assign_expr = null;//左辺のExpr
		if(this.postfix_expr != null){
			v = this.postfix_expr.check_assign(cs);
			//代入していいかどうかの検証
			if(v.is_this_field()){//フィールドかどうか？
				//assignしていいか
				
				BoolExpr ex;
				
				
				ex = v.assign_index_expr(v.index, cs);
				
				
				//何でも代入していい
				ex = cs.ctx.mkOr(ex, cs.assinable_cnst_all);
				
				//コンストラクタ
				if(!(cs.in_constructor&&v.class_object.equals(cs.this_field, cs))){
					System.out.println("check assign");
					cs.assert_constraint(ex);
				}
				
				//finalかどうか
				if(v.modifiers!=null && v.modifiers.is_final){
					if(cs.in_constructor&&v.class_object.equals(cs.this_field, cs)&&v.final_initialized==false){
						v.final_initialized = true;
					}else{
						throw new Exception("Cannot be assigned to " + v.field_name);
					}
				}
			}
			
			//左辺のExpr
			assign_expr = v.get_full_Expr_assign((ArrayList<IntExpr>)v.index.clone(), cs);
			if(v.index.size() > 0){//配列の要素にアクセスする場合、関係ない部分は変わっていないことの制約
				IntExpr[] tmps = new IntExpr[v.index.size()];
				ArrayList<IntExpr> tmp_list = new ArrayList<IntExpr>();
				BoolExpr index_cnst_expr = null;
				for(int i = 0; i < v.index.size(); i++){
					//長さに関する制約
					if(v.index.size() > v.class_object_dims_sum() && v.class_object_dims_sum() <= i){//配列の要素に代入するとき、次元の違うところは長さが変わらない
						
						int array_dim = v.dims_sum() - i;
						String array_type;
						if(v.type.equals("int")){
							array_type = "int";
						}else if(v.type.equals("boolean")){
							array_type = "boolean";
						}else{
							array_type = "ref";
						}
						if(i == v.class_object_dims_sum()){
							Expr ex = null;
							Expr ex_assign = null;
							
							ex = v.get_full_Expr(new ArrayList<IntExpr>(v.index.subList(0, i)), cs);
							ex_assign = v.get_full_Expr_assign(new ArrayList<IntExpr>(v.index.subList(0, i)), cs);
							
							IntExpr length = (IntExpr) cs.ctx.mkSelect(cs.ctx.mkArrayConst("length_" + array_dim + "d_" + array_type, ex.getSort(), cs.ctx.mkIntSort()), ex);
							IntExpr length_assign = (IntExpr) cs.ctx.mkSelect(cs.ctx.mkArrayConst("length_" + array_dim + "d_" + array_type, ex_assign.getSort(), cs.ctx.mkIntSort()), ex_assign);
							
							cs.add_constraint(cs.ctx.mkEq(length, length_assign));
						}else{
							ArrayList<IntExpr> index_tmp_list = new ArrayList<IntExpr>();
							index_tmp_list.addAll(v.index.subList(0, v.class_object_dims_sum()));
							IntExpr[] index_tmps = new IntExpr[i - v.class_object_dims_sum()];
							for(int j = 0; j < tmp_list.subList(v.class_object_dims_sum(), i).size(); j++){
								IntExpr tmp = tmp_list.subList(v.class_object_dims_sum(), i).get(j);
								index_tmp_list.add(tmp);
								index_tmps[j] = tmp;
							}
							Expr ex = v.get_full_Expr((ArrayList<IntExpr>) index_tmp_list.clone(), cs);
							Expr ex_assign = v.get_full_Expr_assign((ArrayList<IntExpr>) index_tmp_list.clone(), cs);
							
							IntExpr length = (IntExpr) cs.ctx.mkSelect(cs.ctx.mkArrayConst("length_" + array_dim + "d_" + array_type, ex.getSort(), cs.ctx.mkIntSort()), ex);
							IntExpr length_assign = (IntExpr) cs.ctx.mkSelect(cs.ctx.mkArrayConst("length_" + array_dim + "d_" + array_type, ex_assign.getSort(), cs.ctx.mkIntSort()), ex_assign);
							
							cs.add_constraint(cs.ctx.mkForall(index_tmps, cs.ctx.mkEq(length, length_assign), 1, null, null, null, null));
						}	
					}
					
					IntExpr index = cs.ctx.mkIntConst("tmpIndex" + cs.Check_status_share.get_tmp_num());
					tmp_list.add(index);
					tmps[i] = index;
					if(index_cnst_expr == null){
						index_cnst_expr = cs.ctx.mkEq(v.index.get(i), index);
					}else{
						index_cnst_expr = cs.ctx.mkAnd(index_cnst_expr, cs.ctx.mkEq(v.index.get(i), index));
					}
					
					
					
				}
				Expr tmp_expr = v.get_full_Expr((ArrayList<IntExpr>) tmp_list.clone(), cs);
				Expr tmp_expr_assign = v.get_full_Expr_assign((ArrayList<IntExpr>) tmp_list.clone(), cs);
				
				BoolExpr expr = cs.ctx.mkImplies(cs.ctx.mkNot(index_cnst_expr), cs.ctx.mkEq(tmp_expr, tmp_expr_assign));
				cs.add_constraint(cs.ctx.mkForall(tmps, expr, 1, null, null, null, null));
				
			}
		}
		Expr implies_tmp = this.implies_expr.check(cs);
		if(this.postfix_expr != null){
			//cs.add_assign(postfix_tmp, implies_tmp);
			
			BoolExpr expr = cs.ctx.mkEq(assign_expr, implies_tmp);
			cs.add_constraint(expr);
			//refinement_type
			Field refined_Field = cs.refined_Field;
			Expr refined_Expr = cs.refined_Expr;
			String refinement_type_value = cs.refinement_type_value;
			boolean in_refinement_predicate = cs.in_refinement_predicate;
			Expr refined_class_Expr = cs.refined_class_Expr;
			Field refined_class_Field = cs.refined_class_Field;
			
			//篩型の処理のための事前準備
			if(v instanceof Field){
				cs.refined_class_Expr = v.class_object_expr;
				cs.refined_class_Field = v.class_object;
			}
			if(v.refinement_type_clause!=null && !(cs.in_constructor&&v.class_object.equals(cs.this_field, cs))){
				if(v.refinement_type_clause.refinement_type!=null){
					v.refinement_type_clause.refinement_type.assert_refinement(cs, v, assign_expr);
				}else if(v.refinement_type_clause.ident!=null){
					refinement_type rt = cs.search_refinement_type(v.class_object.type, v.refinement_type_clause.ident);
					if(rt!=null){
						rt.assert_refinement(cs, v, assign_expr);
					}
				}
			}
			cs.refined_Expr = refined_Expr;
			cs.refined_Field = refined_Field;
			cs.refinement_type_value = refinement_type_value;
			cs.in_refinement_predicate = in_refinement_predicate;
			
			cs.refined_class_Expr = refined_class_Expr;
			cs.refined_class_Field = refined_class_Field;
			cs.refinement_deep--;
			
			
			v.temp_num++;
			
			
			return assign_expr;
		}else{
			return implies_tmp;
		}
	}
	

		
}

