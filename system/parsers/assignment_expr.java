package system.parsers;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.Status;

import system.Check_status;
import system.Field;
import system.Parser;
import system.Parser_status;
import system.Refinement_type;
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
		if(this.postfix_expr != null){
			v = this.postfix_expr.check_assign(cs);
			if(v.is_this_field()){
				//assignしていいか
				if(cs.assignables!=null){
					boolean can_assign = false;
					for(Field assign_field : cs.assignables){
						if(assign_field.equals(v, cs)){
							if(v.index!=null){
								BoolExpr ex = cs.ctx.mkFalse();
								for(IntExpr assign_index : assign_field.assinable_indexs){
									ex = cs.ctx.mkOr(ex, cs.ctx.mkEq(v.index, assign_index));
								}
								if(cs.pathcondition!=null){
									ex = cs.ctx.mkAnd(cs.pathcondition, cs.ctx.mkNot(ex));
								}else{
									ex = cs.ctx.mkNot(ex);
								}
								cs.solver.push();
								cs.solver.add(ex);
								if(cs.solver.check() != Status.SATISFIABLE) {
									can_assign = true;
								}
								cs.solver.pop();
							}else{
								can_assign = true;
							}
						}
					}
					if(cs.in_constructor&&v.class_object.equals(cs.this_field, cs)){
						can_assign = true;
					}
					if(v.modifiers!=null && v.modifiers.is_final){
						if(cs.in_constructor&&v.class_object.equals(cs.this_field, cs)&&v.final_initialized==false){
							can_assign = true;
							v.final_initialized = true;
						}else{
							can_assign = false;
						}
					}
					if(can_assign == false){
						throw new Exception("Cannot be assigned to" + v.field_name);
					}
				}else{//assignableの制約が無くてもfinalの確認はする
					boolean can_assign = true;
					if(v.modifiers!=null && v.modifiers.is_final){
						if(cs.in_constructor&&v.class_object.equals(cs.this_field, cs)&&v.final_initialized==false){
							can_assign = true;
							v.final_initialized = true;
						}else{
							can_assign = false;
						}
					}
					if(can_assign == false){
						throw new Exception("Cannot be assigned to" + v.field_name);
					}
				}
			}
		}
		Expr implies_tmp = this.implies_expr.check(cs);
		if(this.postfix_expr != null){
			Expr postfix_tmp = null;
			if(v instanceof Variable){
				postfix_tmp = v.get_Expr_assign(cs);
			}else{
				postfix_tmp = cs.ctx.mkSelect((ArrayExpr) v.get_Expr_assign(cs),v.class_object_expr);
			}
			//cs.add_assign(postfix_tmp, implies_tmp);
			if(v.index!=null){
				Expr postfix_tmp_now = null;
				if(v instanceof Variable){
					postfix_tmp_now = v.get_Expr(cs);
				}else{
					postfix_tmp_now = cs.ctx.mkSelect((ArrayExpr) v.get_Expr(cs),v.class_object_expr);
				}
				BoolExpr expr = cs.ctx.mkEq(postfix_tmp, cs.ctx.mkStore((ArrayExpr) postfix_tmp_now, v.index, implies_tmp));
				cs.add_constraint(expr);
				//初期化
				v.index = null;
			}else{
				BoolExpr expr = cs.ctx.mkEq(postfix_tmp, implies_tmp);
				cs.add_constraint(expr);
				//refinement_type
				Field refined_Field = cs.refined_Field;
				Expr refined_Expr = cs.refined_Expr;
				String refinement_type_value = cs.refinement_type_value;
				boolean in_refinement_predicate = cs.in_refinement_predicate;
				Expr refined_class_Expr = cs.refined_class_Expr;
				Field refined_class_Field = cs.refined_class_Field;
				IntExpr refined_class_Field_index = cs.refined_class_Field_index;
				
				//篩型の処理のための事前準備
				if(v instanceof Field){
					cs.refined_class_Expr = v.class_object_expr;
					cs.refined_class_Field = v.class_object;
					cs.refined_class_Field_index = v.class_object_index;
				}
				if(v.refinement_type_clause!=null && !(cs.in_constructor&&v.class_object.equals(cs.this_field, cs))){
					if(v.refinement_type_clause.refinement_type!=null){
						v.refinement_type_clause.refinement_type.assert_refinement(cs, v, postfix_tmp);
					}else if(v.refinement_type_clause.ident!=null){
						Refinement_type rt = cs.get_refinement_type(v.refinement_type_clause.ident);
						if(rt!=null){
							rt.assert_refinement(cs, v, postfix_tmp);
						}
					}
				}
				cs.refined_Expr = refined_Expr;
				cs.refined_Field = refined_Field;
				cs.refinement_type_value = refinement_type_value;
				cs.in_refinement_predicate = in_refinement_predicate;
				
				cs.refined_class_Expr = refined_class_Expr;
				cs.refined_class_Field = refined_class_Field;
				cs.refined_class_Field_index = refined_class_Field_index;
				cs.refinement_deep--;
			}
			
			v.temp_num++;
			
			//右がnullなら左もnulになる
			v.length = cs.right_side_status.length;
			
			cs.right_side_status.reflesh();
			
			return postfix_tmp;
		}else{
			return implies_tmp;
		}
	}
	

		
}

