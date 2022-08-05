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
		Expr assign_expr_full = null;//左辺のExprのfullバージョン　篩型をもっていた時のために使う 返り値もこれ
		ArrayList<IntExpr> indexs = null;//左辺のindexs
		Expr assign_tmp_expr = null;
		Expr v_class_object_expr = null;
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
			assign_expr = v.get_Expr_assign(cs);
			assign_expr_full = v.get_full_Expr_assign((ArrayList<IntExpr>) v.index.clone(),cs);
			indexs = v.index;
			assign_tmp_expr = cs.ctx.mkConst("tmpAssignValue" + cs.Check_status_share.get_tmp_num(), v.get_full_Expr((ArrayList<IntExpr>) indexs.clone(), cs).getSort());
			BoolExpr expr = cs.ctx.mkEq(assign_expr, v.assign_value(indexs, assign_tmp_expr, cs));
			cs.add_constraint(expr);
			
			v_class_object_expr = v.class_object_expr;
			
		}
		Expr implies_tmp = this.implies_expr.check(cs);
		if(this.postfix_expr != null){
			//cs.add_assign(postfix_tmp, implies_tmp);
			BoolExpr expr = cs.ctx.mkEq(assign_tmp_expr, implies_tmp);
			cs.add_constraint(expr);
			//refinement_type
			Field refined_Field = cs.refined_Field;
			Expr refined_Expr = cs.refined_Expr;
			String refinement_type_value = cs.refinement_type_value;
			boolean in_refinement_predicate = cs.in_refinement_predicate;
			Expr refined_class_Expr = cs.refined_class_Expr;
			Field refined_class_Field = cs.refined_class_Field;
			
			//篩型の処理のための事前準備
			cs.refined_class_Expr = v_class_object_expr;
			cs.refined_class_Field = v.class_object;
			
			if(v.refinement_type_clause!=null && !(cs.in_constructor&&v.class_object.equals(cs.this_field, cs))){
				if(v.refinement_type_clause.refinement_type!=null){
					v.refinement_type_clause.refinement_type.assert_refinement(cs, v, assign_expr_full);
				}else if(v.refinement_type_clause.ident!=null){
					refinement_type rt = cs.search_refinement_type(v.class_object.type, v.refinement_type_clause.ident);
					if(rt!=null){
						rt.assert_refinement(cs, v, assign_expr_full);
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
			

			
			return assign_expr_full;
		}else{
			return implies_tmp;
		}
	}
	

		
}

