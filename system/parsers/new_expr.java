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
			throw new Exception("can't use new in jml");
		}	
		return st;
	}
	
	public Variable check(Check_status cs) throws Exception{
		if(this.new_suffix.is_index){
			Variable ret = null;
			ret = new Variable(cs.Check_status_share.get_tmp_num(), "new_" + this.type.type + "_array_tmp", this.type.type, this.new_suffix.array_decl.dims, null, new modifiers(), cs.this_field);
			ret.temp_num++;
			ret.new_array = true;
			

			List<IntExpr> lengths = this.new_suffix.array_decl.check(cs);
			
			IntExpr[] tmps = new IntExpr[lengths.size()-1];
			IntExpr[] tmps_full = new IntExpr[this.new_suffix.array_decl.dims];
			ArrayList<IntExpr> tmp_list = new ArrayList<IntExpr>();
			BoolExpr guard = null;
			BoolExpr length_cnst = null;
			
			Expr ex = ret.get_Expr(cs);
			
			
			for(int i = 0; i < lengths.size(); i++){
				
				//lengthに関する制約
				int array_dim = this.new_suffix.array_decl.dims - (i);
				String array_type;
				if(ret.type.equals("int")){
					array_type = "int";
				}else if(ret.type.equals("boolean")){
					array_type = "boolean";
				}else{
					array_type = "ref";
				}
				IntExpr length = (IntExpr) cs.ctx.mkSelect(cs.ctx.mkArrayConst("length_" + array_dim + "d_" + array_type, ex.getSort(), cs.ctx.mkIntSort()), ex);
				if(length_cnst == null){
					length_cnst = cs.ctx.mkEq(length, lengths.get(i));
				}else{
					length_cnst = cs.ctx.mkAnd(length_cnst, cs.ctx.mkEq(length, lengths.get(i)));
				}
				
				
				
				IntExpr index = cs.ctx.mkIntConst("tmpIdex" + cs.Check_status_share.get_tmp_num());
				
				if(i == lengths.size() - 1){//最後のループで制約を追加
					BoolExpr expr;
					if(guard == null){
						expr = length_cnst;
						cs.add_constraint(expr);
					}else{
						expr = cs.ctx.mkImplies(guard, length_cnst);
						cs.add_constraint(cs.ctx.mkForall(tmps, expr, 1, null, null, null, null));
					}
					
					
				}else{
					tmps[i] = index;
				}
				
				tmps_full[i] = index;
				tmp_list.add(index);
				//インデックスの範囲のガード 
				if(guard == null){
					guard = cs.ctx.mkAnd(cs.ctx.mkGe(index, cs.ctx.mkInt(0)), cs.ctx.mkGt(lengths.get(i), index));
				}else{
					guard = cs.ctx.mkAnd(guard, cs.ctx.mkAnd(cs.ctx.mkGe(index, cs.ctx.mkInt(0)), cs.ctx.mkGt(lengths.get(i), index)));
				}
				
				//次のex
				ex = ret.get_full_Expr((ArrayList<IntExpr>) tmp_list.clone(), cs);
				
			}
			
			
			
			
			//配列の初期値についての制約
			for(int i = lengths.size(); i < this.new_suffix.array_decl.dims; i++){
				IntExpr index = cs.ctx.mkIntConst("tmpIdex" + cs.Check_status_share.get_tmp_num());
				tmps_full[i] = index;
				tmp_list.add(index);
			}
			
			if(this.type.type.equals("int")){
				BoolExpr value_cnst = cs.ctx.mkEq(cs.ctx.mkInt(0), ret.get_full_Expr(tmp_list, cs));
				cs.add_constraint(cs.ctx.mkForall(tmps_full, cs.ctx.mkImplies(guard, value_cnst), 1, null, null, null, null));
			}else if(this.type.type.equals("boolean")){
				BoolExpr value_cnst = cs.ctx.mkEq(cs.ctx.mkBool(false), ret.get_full_Expr(tmp_list, cs));
				cs.add_constraint(cs.ctx.mkForall(tmps_full, cs.ctx.mkImplies(guard, value_cnst), 1, null, null, null, null));
			}
			

			return ret;
		}else if(this.new_suffix.expression_list!=null){
			//String ident = 
			//Field f;
			//Expr ex
			new_suffix ps = this.new_suffix;
			//IntExpr f_index
			
			boolean pre_can_not_use_mutable = cs.can_not_use_mutable;
			if(cs.in_refinement_predicate) cs.can_not_use_mutable = true;
			boolean pre_in_refinement_predicate = cs.in_refinement_predicate;
			cs.in_refinement_predicate = false;
			
			
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
			
			Expr pre_instance_expr = cs.instance_expr;
			Field pre_instance_Field = cs.instance_Field;
			ArrayList<IntExpr> pre_instance_indexs = cs.instance_indexs;
			
			cs.instance_expr = result.get_Expr(cs);
			cs.instance_Field = result;
			cs.instance_indexs = new ArrayList<IntExpr>();

			
			for(int j = 0; j < md.formals.param_declarations.size(); j++){
				param_declaration pd = md.formals.param_declarations.get(j);
				modifiers m = new modifiers();
				m.is_final = pd.is_final;
				Variable v = new Variable(cs.Check_status_share.get_tmp_num(), pd.ident, pd.type_spec.type.type, pd.type_spec.dims, pd.type_spec.refinement_type_clause, m, result);
				cs.called_method_args.add(v);
				v.temp_num = 0;
				//引数に値を紐づける
				cs.add_constraint(cs.ctx.mkEq(v.get_Expr(cs), ps.expression_list.expressions.get(j).check(cs).expr));
				//篩型
				if(v.refinement_type_clause!=null){
					if(v.refinement_type_clause.refinement_type!=null){
						v.refinement_type_clause.refinement_type.assert_refinement(cs, v, v.get_Expr(cs), null, null, new ArrayList<IntExpr>());//class_Fieldとかは本来resultなどだが、まだできていないオブジェクトなのでnullでいいはず
					}else{
						refinement_type rt = cs.search_refinement_type(v.class_object.type, v.refinement_type_clause.ident);
						if(rt!=null){
							rt.assert_refinement(cs, v, v.get_Expr(cs), null, null, new ArrayList<IntExpr>());
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
					BoolExpr assign_expr = null;
					//配列の要素に代入できる制約
					if(fa.cnst_array.size()>0){
						for(int i = 1; i <= fa.field.dims; i++){//各次元に関して
							List<IntExpr> index_expr = new ArrayList<IntExpr>();
							for(int j = 0; j < i; j++){
								index_expr.add(cs.ctx.mkIntConst("tmpIdex" + cs.Check_status_share.tmp_num));
							}
							BoolExpr call_assign_expr = fa.assign_index_expr(index_expr, cs);
							BoolExpr field_assign_expr = fa.field.assign_index_expr(index_expr, cs);
							if(assign_expr == null){
								assign_expr = cs.ctx.mkImplies(call_assign_expr, field_assign_expr);
							}else{
								assign_expr = cs.ctx.mkAnd(assign_expr, cs.ctx.mkImplies(call_assign_expr, field_assign_expr));
							}
						}
					}else{
						assign_expr = cs.ctx.mkBool(true);
					}
					//何でも代入していい
					assign_expr = cs.ctx.mkOr(assign_expr, cs.assinable_cnst_all);
					
					System.out.println("check assign");
					cs.assert_constraint(assign_expr);
					
					//実際に代入する制約を追加する
					System.out.println("assign " + fa.field.field_name);
					
					
					//配列の要素に代入
					//そのフィールドにたどり着くまでに配列アクセスをしていた場合も
					if(fa.cnst_array.size()>0){
						for(int i = 0; i <= fa.field.dims; i++){//各次元に関して
							ArrayList<IntExpr> index_expr = new ArrayList<IntExpr>();
							for(int j = 0; j < fa.field.class_object_dims_sum(); j++){//そのフィールドまでの配列のindex
								index_expr.add(cs.ctx.mkIntConst("tmpIdex" + cs.Check_status_share.get_tmp_num()));
							}
							Expr old_element = fa.field.get_full_Expr((ArrayList)index_expr.clone(), cs);
							Expr new_element = fa.field.get_full_Expr_assign((ArrayList)index_expr.clone(), cs);
							for(int j = 0; j < i; j++){//その配列に対するindex
								
								//長さに関する制約
								int array_dim = fa.field.dims - j;
								String array_type;
								if(fa.field.type.equals("int")){
									array_type = "int";
								}else if(fa.field.type.equals("boolean")){
									array_type = "boolean";
								}else{
									array_type = "ref";
								}
								if(j == 0 && fa.field.class_object_dims_sum()==0){
									IntExpr length = (IntExpr) cs.ctx.mkSelect(cs.ctx.mkArrayConst("length_" + array_dim + "d_" + array_type, old_element.getSort(), cs.ctx.mkIntSort()), old_element);
									IntExpr length_assign = (IntExpr) cs.ctx.mkSelect(cs.ctx.mkArrayConst("length_" + array_dim + "d_" + array_type, new_element.getSort(), cs.ctx.mkIntSort()), new_element);
									
									cs.add_constraint(cs.ctx.mkEq(length, length_assign));
								}else{
									IntExpr[] index_tmps = new IntExpr[index_expr.size()];
									for(int k = 0; k < index_expr.size(); k++){
										index_tmps[k] = index_expr.get(k);
									}
									
									IntExpr length = (IntExpr) cs.ctx.mkSelect(cs.ctx.mkArrayConst("length_" + array_dim + "d_" + array_type, old_element.getSort(), cs.ctx.mkIntSort()), old_element);
									IntExpr length_assign = (IntExpr) cs.ctx.mkSelect(cs.ctx.mkArrayConst("length_" + array_dim + "d_" + array_type, new_element.getSort(), cs.ctx.mkIntSort()), new_element);
									
									cs.add_constraint(cs.ctx.mkForall(index_tmps, cs.ctx.mkEq(length, length_assign), 1, null, null, null, null));
								}	

								
								
								
								IntExpr index = cs.ctx.mkIntConst("tmpIdex" + cs.Check_status_share.get_tmp_num());
								index_expr.add(index);
								old_element = cs.ctx.mkSelect((ArrayExpr) old_element, index);
								new_element = cs.ctx.mkSelect((ArrayExpr) new_element, index);
							}
							
							
							
							BoolExpr expr = cs.ctx.mkImplies(cs.ctx.mkNot(cs.ctx.mkOr(fa.assign_index_expr(index_expr, cs), cs.assinable_cnst_all)), cs.ctx.mkEq(old_element, new_element));
							
							if(index_expr.size()>0){
								Expr[] index_exprs_array = new Expr[index_expr.size()];
								for(int j = 0; j < index_expr.size(); j++){
									index_exprs_array[j] = index_expr.get(j);
								}
								
								
								cs.add_constraint(cs.ctx.mkForall(index_exprs_array, expr, 1, null, null, null, null));
								fa.field.temp_num++;
							}else{
								cs.add_constraint(expr);
							}
						}
						
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
				cs.add_constraint(ensures_expr);
			}
			cs.in_method_call = false;
			
			cs.instance_expr = pre_instance_expr;
			cs.instance_Field = pre_instance_Field;
			cs.instance_indexs = pre_instance_indexs;
			
			cs.can_not_use_mutable = pre_can_not_use_mutable;
			cs.in_refinement_predicate = pre_in_refinement_predicate;
			
			return result;
		}else{
			throw new Exception("wrong new clause");
		}
	}
	

}

