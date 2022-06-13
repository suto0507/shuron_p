package system.parsers;

import java.util.ArrayList;
import java.util.List;

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
import system.parsers.spec_case_seq.F_Assign;

public class new_expr implements Parser<String>{
	type type;
	new_suffix new_suffix;
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		st = st + new string("new").parse(s, ps);
		st = st + new spaces().parse(s, ps);
		this.type = new type();
		st = st + this.type.parse(s, ps);
		st = st + new spaces().parse(s, ps);
		this.new_suffix = new new_suffix();
		st = st + this.new_suffix.parse(s, ps);
		if(ps.in_jml){
			throw new Exception("cant use new in jml");
		}	
		return st;
	}
	
	public Variable check(Check_status cs) throws Exception{
		if(this.new_suffix.expression!=null){
			IntExpr length = (IntExpr) this.new_suffix.expression.check(cs);
			Variable ret = null;
			cs.right_side_status.length = length;
			
			ret = new Variable(cs.Check_status_share.get_tmp_num(), "new_" + this.type.type + "_array_tmp", this.type.type, 1, null, new modifiers(), cs.this_field);

			ret.temp_num++;
			return ret;
		}else if(this.new_suffix.expression_list!=null){
			//String ident = 
			//Field f;
			//Expr ex
			new_suffix ps = this.new_suffix;
			//IntExpr f_index
			
			
			class_declaration cd = cs.Check_status_share.compilation_unit.search_class(this.type.type);
			if(cd == null){
				throw new Exception("can't find class " + this.type.type);
			}
			method_decl md = cs.Check_status_share.compilation_unit.search_method(this.type.type, this.type.type);
			if(md == null){
				throw new Exception("can't find method " + this.type.type);
			}
			cs.in_method_call = true;
			//引数の処理
			cs.called_method_args = new ArrayList<Variable>();
			if(md.formals.param_declarations.size()!=ps.expression_list.expressions.size()){
				throw new Exception("wrong number of arguments");
			}

			
			//返り値
			Variable result = new Variable(cs.Check_status_share.get_tmp_num(), "class_" + this.type.type + "_constructor_tmp", this.type.type, 0, null, md.modifiers, cs.this_field);
			result.temp_num++;
			cs.result = result;

			cs.call_expr = result.get_Expr(cs);
			cs.call_field = result;
			cs.call_field_index = null;
			
			for(int j = 0; j < md.formals.param_declarations.size(); j++){
				param_declaration pd = md.formals.param_declarations.get(j);
				modifiers m = new modifiers();
				m.is_final = pd.is_final;
				Variable v = new Variable(cs.Check_status_share.get_tmp_num(), pd.ident, pd.type_spec.type.type, pd.type_spec.dims, pd.type_spec.refinement_type_clause, m, result);
				cs.called_method_args.add(v);
				v.temp_num = 0;
				//引数に値を紐づける
				cs.add_constraint(cs.ctx.mkEq(v.get_Expr(cs), ps.expression_list.expressions.get(j).check(cs)));
				//篩型
				if(v.refinement_type_clause!=null){
					if(v.refinement_type_clause.refinement_type!=null){
						v.refinement_type_clause.refinement_type.assert_refinement(cs, v, v.get_Expr(cs));
					}else{
						refinement_type rt = cs.search_refinement_type(v.class_object.type, v.refinement_type_clause.ident);
						if(rt!=null){
							rt.assert_refinement(cs, v, v.get_Expr(cs));
						}else{
							throw new Exception("cant find refinement type " + v.refinement_type_clause.ident);
						}
					}
				}
			}
			//事前条件
			//BoolExpr pre_invariant_expr = null;
			/*これはいらん
			if(cd.class_block.invariants!=null&&cd.class_block.invariants.size()>0){
				for(invariant inv : cd.class_block.invariants){
					if(pre_invariant_expr == null){
						pre_invariant_expr = (BoolExpr) inv.check(cs);
					}else{
						pre_invariant_expr = cs.ctx.mkAnd(pre_invariant_expr, (BoolExpr)inv.check(cs));
					}
				}
				cs.assert_constraint(pre_invariant_expr);
			}
			*/
			BoolExpr require_expr = null;

			if(md.method_specification != null){
				require_expr = md.method_specification.requires_expr(cs);
				cs.assert_constraint(require_expr);
			}
			
			//old data
			Check_status csc = cs.clone();
			csc.clone_list();
			cs.old_status = csc;
			
			//assign
			if(md.method_specification != null){
				
				Pair<List<F_Assign>, BoolExpr> assign_cnsts = md.method_specification.assignables(cs);
				for(F_Assign fa : assign_cnsts.fst){
					BoolExpr assign_expr;
					//フィールドへの代入
					if(fa.cnst!=null){
						assign_expr = cs.ctx.mkImplies(fa.cnst, fa.field.assinable_cnst);
					}else{
						assign_expr = cs.ctx.mkBool(true);
					}
					//配列の要素に代入
					if(fa.cnst_array.size()>0){
						IntExpr index_expr = cs.ctx.mkIntConst("tmpIdex" + cs.Check_status_share.tmp_num);
						BoolExpr call_assign_expr = fa.assign_index_expr(index_expr, cs);
						BoolExpr field_assign_expr = fa.field.assign_index_expr(index_expr, cs);
						assign_expr = cs.ctx.mkAnd(assign_expr, cs.ctx.mkImplies(call_assign_expr, field_assign_expr));
					}else{
						assign_expr = cs.ctx.mkAnd(assign_expr, cs.ctx.mkBool(true));
					}
					//何でも代入していい
					assign_expr = cs.ctx.mkOr(assign_expr, cs.assinable_cnst_all);
					
					
					System.out.println("check assign");
					cs.assert_constraint(assign_expr);
					
					//実際に代入する制約を追加する
					System.out.println("assign " + fa.field.field_name);
					//フィールドへの代入
					if(fa.cnst!=null){
						Expr tmp_Expr;
						if(fa.field.type.equals("int")){
							tmp_Expr = cs.ctx.mkIntConst("tmpInt" + cs.Check_status_share.tmp_num);
						}else if(fa.field.type.equals("boolean")){
							tmp_Expr = cs.ctx.mkBoolConst("tmpBool" + cs.Check_status_share.tmp_num);
						}else{
							tmp_Expr = cs.ctx.mkConst("tmpRef" + cs.Check_status_share.tmp_num,  cs.ctx.mkUninterpretedSort("Ref"));
						}
						BoolExpr expr = cs.ctx.mkEq(cs.ctx.mkSelect((ArrayExpr)fa.field.get_Expr_assign(cs), fa.field.class_object_expr), 
								cs.ctx.mkITE(cs.ctx.mkOr(fa.cnst, cs.assinable_cnst_all), tmp_Expr, cs.ctx.mkSelect((ArrayExpr)fa.field.get_Expr(cs), fa.field.class_object_expr)));
						cs.add_constraint(expr);
						fa.field.temp_num++;
						
					}
					//配列の要素に代入
					if(fa.cnst_array.size()>0){
						Expr tmp_Expr;
						if(fa.field.type.equals("int")){
							tmp_Expr = cs.ctx.mkIntConst("tmpInt" + cs.Check_status_share.tmp_num);
						}else if(fa.field.type.equals("boolean")){
							tmp_Expr = cs.ctx.mkBoolConst("tmpBool" + cs.Check_status_share.tmp_num);
						}else{
							tmp_Expr = cs.ctx.mkConst("tmpRef" + cs.Check_status_share.tmp_num,  cs.ctx.mkUninterpretedSort("Ref"));
						}
						
						IntExpr index_expr = cs.ctx.mkIntConst("tmpIdex" + cs.Check_status_share.tmp_num);
						Expr old_element = cs.ctx.mkSelect((ArrayExpr) cs.ctx.mkSelect((ArrayExpr)fa.field.get_Expr(cs), fa.field.class_object_expr), index_expr);
						Expr new_element = cs.ctx.mkSelect((ArrayExpr) cs.ctx.mkSelect((ArrayExpr)fa.field.get_Expr_assign(cs), fa.field.class_object_expr), index_expr);
						BoolExpr expr = cs.ctx.mkImplies(cs.ctx.mkNot(cs.ctx.mkOr(fa.assign_index_expr(index_expr, cs), cs.assinable_cnst_all)), cs.ctx.mkEq(old_element, new_element));
						cs.add_constraint(expr);
						fa.field.temp_num++;
						
					}
					
					
				}
			}else{
				//assignableを含めた任意の仕様が書かれていない関数
				for(Field f_a : cs.fields){
					f_a.temp_num++;
				}
			}
			
			
			
			//事後条件
			BoolExpr post_invariant_expr = null;
			if(cd.class_block.invariants!=null&&cd.class_block.invariants.size()>0){
				for(invariant inv : cd.class_block.invariants){
					if(post_invariant_expr == null){
						post_invariant_expr = (BoolExpr) inv.check(cs);
					}else{
						post_invariant_expr = cs.ctx.mkAnd(post_invariant_expr, (BoolExpr)inv.check(cs));
					}
				}
				cs.add_constraint(post_invariant_expr);
			}
			BoolExpr ensures_expr = null;
			if(md.method_specification != null){
				ensures_expr = md.method_specification.ensures_expr(cs);
				cs.assert_constraint(ensures_expr);
			}
			cs.in_method_call = false;
			
			return result;
		}else{
			throw new Exception("wrong new clause");
		}
	}
	

}

