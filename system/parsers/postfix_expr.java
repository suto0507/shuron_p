package system.parsers;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.Status;

import system.Array;
import system.Check_return;
import system.Check_status;
import system.Dummy_Field;
import system.Field;
import system.Method_Assign;
import system.Model_Field;
import system.Pair;
import system.Parser;
import system.Parser_status;
import system.Quantifier_Variable;
import system.Source;
import system.Type_info;
import system.Variable;
import system.F_Assign;

public class postfix_expr implements Parser<String>{
	primary_expr primary_expr;
	//要素数が1以上なら後ろが含まれる
	ArrayList<primary_suffix> primary_suffixs;
	public String parse(Source s,Parser_status ps)throws Exception{
		this.primary_suffixs = new ArrayList<primary_suffix>();
		String st = "";
		this.primary_expr = new primary_expr();
		st = st + this.primary_expr.parse(s,ps);
		Source s_backup = s.clone();
		try{
			while(true){
				s_backup = s.clone();
				primary_suffix ee = new primary_suffix();
				st = st + ee.parse(s, ps);
				this.primary_suffixs.add(ee);
			}
		}catch (Exception e){
			s.revert(s_backup);
		}
		
		return st;
	}
	
	public Check_return check(Check_status cs) throws Exception{
		Expr ex = null;
		Field f = null;
		String ident = null;
		Expr class_expr = cs.instance_expr;
		Type_info type_info = null;
		boolean is_refine_value = false;
		

		//suffixについて
		List<IntExpr> indexs = new ArrayList<IntExpr>();
		
		
		if(this.primary_expr.is_this){
			if(this.primary_suffixs.size() == 0 && cs.in_constructor){//this単体のコンストラクターでの使用
				cs.constructor_refinement_check();
				cs.this_alias = cs.ctx.mkOr(cs.this_alias, cs.get_pathcondition());
			}
			
			if(cs.this_field.get_Expr(cs).equals(cs.instance_expr)){
				f = cs.this_field;
			}else{
				f = new Dummy_Field(cs.instance_class_name, cs.instance_expr);
			}
			
			ex = cs.instance_expr;
			type_info = new Type_info(cs.instance_class_name, 0);
			
			
		}else if(this.primary_expr.is_super){
			class_declaration cd = cs.Check_status_share.compilation_unit.search_class(cs.instance_class_name);
			cd = cd.super_class;
			if(this.primary_suffixs.size() > 0 && this.primary_suffixs.get(0).is_method){//スーパークラスのコンストラクター
				if(primary_expr.constructor_first == false){
					throw new Exception("calling super constructor must be the first statement in a constructor");
				}
				
				method(cs, cd.class_name, cd.class_name, cs.instance_expr, this.primary_suffixs.get(0), true);
				ex = cs.instance_expr;
				class_expr = null;
				ident = null;
				indexs = new ArrayList<IntExpr>();
				type_info = new Type_info(cd.class_name, 0);
				
				if(this.primary_suffixs.size() > 1){
					throw new Exception("super constructor cannot have suffixs");
				}
				
				return new Check_return(ex, f, (ArrayList<IntExpr>) indexs, class_expr, type_info);
			}else{
				if(cs.this_field.get_Expr(cs).equals(cs.instance_expr)){
					f = cs.this_field.clone_e();
					f.type = cd.class_name;
				}else{
					f = new Dummy_Field(cd.class_name, cs.instance_expr);
				}
				
				ex = cs.instance_expr;
				type_info = new Type_info(cd.class_name, 0);
			}
			
		}else if(this.primary_expr.ident!=null){
			
			Quantifier_Variable quantifier = cs.search_quantifier(this.primary_expr.ident, cs);
			if(quantifier != null){//量化子として束縛されているか
				f = quantifier;
				ex = f.get_Expr(cs);
			}else if(cs.in_refinement_predicate==true && this.primary_expr.ident.equals(cs.refinement_type_value)){
				f = cs.refined_Field;
				ex = cs.refined_Expr;
				class_expr = cs.instance_expr;
				is_refine_value = true;
			}else{
				if(this.primary_suffixs.size() > 0 && this.primary_suffixs.get(0).is_method){//メソッド
					ident = this.primary_expr.ident;
					f = new Dummy_Field(cs.instance_class_name, cs.instance_expr);
					ex = cs.instance_expr;
				}else{
					//ローカル変数
					if(cs.in_refinement_predicate){//篩型
						if(cs.search_variable(this.primary_expr.ident)){
							f = cs.get_variable(this.primary_expr.ident);
							ex = f.get_Expr(cs);
						}
						
					}else if(cs.in_method_call){//関数呼び出し
						if(cs.search_called_method_arg(primary_expr.ident)){
							f = cs.get_called_method_arg(primary_expr.ident);
							ex = f.get_Expr(cs);
						}
						
					}else{
						if(cs.search_variable(primary_expr.ident)){
							f = cs.get_variable(primary_expr.ident);
							ex = f.get_Expr(cs);
							if(cs.in_postconditions && ((Variable)f).is_arg){//メソッドの引数には暗黙のoldが付く
								f = cs.this_old_status.get_variable(primary_expr.ident);
								ex = f.get_Expr(cs.this_old_status);
							}
						}
					}
					
					if(f==null){//ローカル変数ではない場合
						Field searched_field = cs.search_field(primary_expr.ident, cs.instance_class_name ,cs);
						if(searched_field != null){//フィールド
							f = searched_field;
							ex = cs.ctx.mkSelect((ArrayExpr) f.get_Expr(cs), cs.instance_expr);
							class_expr = cs.instance_expr;
							if(cs.can_not_use_mutable){
								if(!f.modifiers.is_final){
									new Exception("method depends on mutable field in refinenment type predicate.");
								}
							}
							if(f.modifiers.is_final){
								f.set_initialize(cs.instance_expr, cs);
							}
							
						}else{
							Model_Field searched_model_field = cs.search_model_field(primary_expr.ident, cs.instance_class_name ,cs);
							if(searched_model_field != null){//modelフィールド
								f = searched_model_field;
								ex = cs.ctx.mkSelect((ArrayExpr) ((Model_Field)f).get_Expr(cs.instance_expr, cs),cs.instance_expr);
								class_expr = cs.instance_expr;
								if(cs.can_not_use_mutable){
									new Exception("method depends on model field in refinenment type predicate.");
								}
							}else{
								throw new Exception(cs.instance_class_name + " don't have " + this.primary_expr.ident);
							}
						}
					}
					f.set_ref_info(ex, cs);
				}
				

				if(cs.in_refinement_predicate==true){
					if((f.modifiers!=null&&f.modifiers.is_final==false) || f instanceof Model_Field){//篩型の中ではfinalである必要がある
						throw new Exception("can use only final variable in refinement type");
					}
				}
				//JML節での使えない可視性の確認
				if(cs.ban_default_visibility){
					if(f.modifiers!=null&&f.modifiers.is_private==false){
						throw new Exception("cannot use default visibility variable");
					}
				}
				if(cs.ban_private_visibility){
					if(f.modifiers!=null&&f.modifiers.is_private==true){
						throw new Exception("cannot use private visibility variable");
					}
				}
				if(cs.can_use_type_in_invariant!=null){
					if(ident == null && //メソッド呼び出しはいい
							!(cs.can_use_type_in_invariant.equals(f.class_type_name) && class_expr.equals(cs.instance_expr))){
						throw new Exception("invariant depends on only field of class " + cs.can_use_type_in_invariant);
					}
				}
			}
			type_info = new Type_info(f.type, f.dims);
		}else if(this.primary_expr.bracket_expression!=null){
			Check_return cr = this.primary_expr.bracket_expression.check(cs);
			f = cr.field;
			ex = cr.expr;
			class_expr = cr.class_expr;
			indexs = cr.indexs;
			type_info = cr.type_info;
		}else if(this.primary_expr.java_literal!=null){
			if(this.primary_suffixs.size() == 0){
				Check_return cr = this.primary_expr.java_literal.check(cs);
				ex = cr.expr;
				type_info = cr.type_info;
			}else{
				throw new Exception("literal don't have suffix");
				
			}
		}else if(this.primary_expr.jml_primary!=null){
			if(cs.in_method_call){
				if(this.primary_expr.jml_primary.is_result){
					f = cs.result;
					ex = f.get_Expr(cs);
					type_info = new Type_info(f.type, f.dims);
				}else if(this.primary_expr.jml_primary.old_expression!=null){
					Check_return cr = this.primary_expr.jml_primary.old_expression.spec_expression.check(cs.old_status);
					f = cr.field;
					ex = cr.expr;
					class_expr = cr.class_expr;
					indexs = cr.indexs;
					type_info = cr.type_info;
				}else if(this.primary_expr.jml_primary.spec_quantified_expr!=null){
					ex = this.primary_expr.jml_primary.spec_quantified_expr.check(cs);
					type_info = new Type_info("boolean", 0);
					if(this.primary_suffixs.size() != 0) throw new Exception("quantifier don't have suffix");
				}
			}else{
				if(this.primary_expr.jml_primary.is_result){
					if(!cs.in_postconditions){
						throw new Exception("\\result can use only in postcondition");
					}
					f = cs.return_v;
					ex = cs.return_expr;
					type_info = new Type_info(f.type, f.dims);
				}else if(this.primary_expr.jml_primary.old_expression!=null){
					if(!cs.in_postconditions){
						throw new Exception("\\old can use only in postcondition");
					}
					Check_return cr = this.primary_expr.jml_primary.old_expression.spec_expression.check(cs.this_old_status);
					f = cr.field;
					ex = cr.expr;
					class_expr = cr.class_expr;
					indexs = cr.indexs;
					type_info = cr.type_info;
				}else if(this.primary_expr.jml_primary.spec_quantified_expr!=null){
					ex = this.primary_expr.jml_primary.spec_quantified_expr.check(cs);
					type_info = new Type_info("boolean", 0);
					if(this.primary_suffixs.size() != 0) throw new Exception("quantifier don't have suffix");
				}
			}
		}else if(this.primary_expr.new_expr!=null){
			f = this.primary_expr.new_expr.check(cs);
			ex = f.get_Expr(cs);
			type_info = new Type_info(f.type, f.dims);
		}else{
			//return null;
		}
		
		if(f!=null && f.hava_refinement_type() && is_refine_value==false){//篩型			
			add_refinement_constraint(cs, f, cs.instance_expr);
		}
		
		//コンストラクタの中ではthisは使えない メソッドは許す ローカル変数も許さないといけない
		if(f != null && cs.in_constructor_precondition 
				&& (cs.this_field.get_Expr(cs).equals(ex) && ident == null  && !(this.primary_suffixs.size() >= 2 && (this.primary_suffixs.get(1).is_method))
						|| cs.this_field.get_Expr(cs).equals(class_expr) && ident == null && !(f instanceof Variable))){
			if(!(f.modifiers!=null && f.modifiers.is_final && f.final_initialized)){//初期値が与えられている定数は使ってよい
				throw new Exception("cannot use field of \"this\" in precondition of constructor");
			}
		}
		
		
		
		for(int i = 0; i < this.primary_suffixs.size(); i++){
			primary_suffix ps = this.primary_suffixs.get(i);
			if(ps.is_field){
				if(this.primary_suffixs.size() > i+1 && this.primary_suffixs.get(i+1).is_method){
					ident = ps.ident;
				}else if(f.dims > indexs.size() && ps.ident.equals("length")){
					if(this.primary_suffixs.size()-1 != i) throw new Exception("length don't have suffix");
					
					ex = (IntExpr) cs.ctx.mkSelect(cs.ctx.mkArrayConst("length", cs.ctx.mkUninterpretedSort("ArrayRef"), cs.ctx.mkIntSort()), ex);
					f = null;
					class_expr = null;
					type_info = new Type_info("int", 0);
				}else{
					Field pre_f = f;
					class_expr = ex;
					
					Field searched_field = cs.search_field(ps.ident, type_info.type, cs);
					if(searched_field != null){
						f = searched_field;
						ex = cs.ctx.mkSelect((ArrayExpr)f.get_Expr(cs), ex);
						if(cs.can_not_use_mutable){
							if(!f.modifiers.is_final){
								new Exception("method depends on mutable field in refinenment type predicate.");
							}
						}

						if(f.modifiers.is_final){
							f.set_initialize(class_expr, cs);
						}
					}else{
						Model_Field searched_model_field = cs.search_model_field(ps.ident, f.type, cs);
						if(searched_model_field != null){
							f = searched_model_field;
							ex = cs.ctx.mkSelect((ArrayExpr) ((Model_Field)f).get_Expr(ex, cs), ex);
							if(cs.can_not_use_mutable){
								new Exception("method depends on model field in refinenment type predicate.");
							}
						}else{
							throw new Exception(f.type + " don't have " + ps.ident);
						}
					}
					f.set_ref_info(ex, cs);
					type_info = new Type_info(f.type, f.dims);
					
					if(f.hava_refinement_type()){//篩型
						add_refinement_constraint(cs, f, class_expr);
					}
					
					if(cs.in_refinement_predicate==true){//篩型の中ではfinalである必要がある
						if(f.modifiers.is_final==false || f instanceof Model_Field){
							throw new Exception("can use only final variable in refinement type");
						}
					}
					
					//JML節での使えない可視性の確認
					if(cs.ban_default_visibility){
						if(f.modifiers!=null&&f.modifiers.is_private==false){
							throw new Exception("cannot use default visibility variable");
						}
					}
					if(cs.ban_private_visibility){
						if(f.modifiers!=null&&f.modifiers.is_private==true){
							throw new Exception("cannot use private visibility variable");
						}
					}
					if(cs.can_use_type_in_invariant!=null){
						if(ident == null && //メソッド呼び出しはいい
								!(cs.can_use_type_in_invariant.equals(f.class_type_name) && class_expr.equals(cs.instance_expr))){
							throw new Exception("invariant depends on only field of class " + cs.can_use_type_in_invariant);
						}
					}
				}
				
				indexs = new ArrayList<IntExpr>();
				
			}else if(ps.is_index){
				IntExpr index =  (IntExpr) ps.expression.check(cs).expr;
				
				IntExpr length = (IntExpr) cs.ctx.mkSelect(cs.ctx.mkArrayConst("length", cs.ctx.mkUninterpretedSort("ArrayRef"), cs.ctx.mkIntSort()), ex);
				BoolExpr index_bound = cs.ctx.mkGe(index, cs.ctx.mkInt(0));
				index_bound = cs.ctx.mkAnd(index_bound, cs.ctx.mkGt(length, index));
				if(!cs.in_jml_predicate){
					System.out.println("check index out of bounds");
					cs.assert_constraint(index_bound);
				}
				
				
				indexs.add(index);
				
				Array array;
				if(indexs.size()<f.dims){
				    array = cs.array_arrayref;
				}else{
				    if(f.type.equals("int")){
				        array = cs.array_int;
				    }else if(f.type.equals("boolean")){
				        array = cs.array_boolean;
				    }else{
				        array = cs.array_ref;
				    }
				}
				ex = array.index_access_array(ex, index, cs);
				
				
				ident = null;
				type_info.dims--;
				
				
				if(cs.in_refinement_predicate==true){//篩型の中では配列は使えない
					if(!f.equals(cs.refined_Field)){
						if(cs.search_quantifier(f.field_name, cs)==null){
							throw new Exception("cannot use array in refinement type");
						}
					}
				}
				
			}else if(ps.is_method){
				//関数の呼び出し
				f = method(cs, ident, f.type, ex, ps, false);
				class_expr = null;
				ident = null;
				indexs = new ArrayList<IntExpr>();
				if(f != null){
					ex = f.get_Expr(cs);
					type_info = new Type_info(f.type, f.dims);
				}else{
					ex = null;
					type_info = new Type_info("void", 0);
				}
			}
		}

		
		return new Check_return(ex, f, (ArrayList<IntExpr>) indexs, class_expr, type_info);
	}
	
	public Check_return check_assign(Check_status cs) throws Exception{
		Expr ex = null;
		Field f = null;
		Type_info type_info = null;
		Expr class_expr = cs.instance_expr;
		ArrayList<IntExpr> indexs = new ArrayList<IntExpr>();
		
		if(this.primary_suffixs.size() == 0){
			if(this.primary_expr.is_this){
				//これはアウト
				throw new Exception("cannot assign this");
			}else if(this.primary_expr.is_super){
				//これはアウト
				throw new Exception("cannot assign this");
			}else if(this.primary_expr.ident!=null){

				Field searched_field = cs.search_field(primary_expr.ident, cs.instance_class_name, cs);
				if(cs.search_variable(primary_expr.ident)){
					f = cs.get_variable(primary_expr.ident);
					class_expr = cs.this_field.get_Expr(cs);
					ex = ((Variable)f).get_Expr(cs, true);
				}else if(searched_field != null){
					f = searched_field;
					class_expr = cs.this_field.get_Expr(cs);
					ex = cs.ctx.mkSelect((ArrayExpr) f.get_Expr(cs), cs.this_field.get_Expr(cs));
				}else{
					throw new Exception(cs.this_field.type + " don't have " + this.primary_expr.ident);
				}
				f.set_ref_info(ex, cs);
				type_info = new Type_info(f.type, f.dims);

			}else if(this.primary_expr.java_literal!=null){
				throw new Exception("cannot assign java literal");
			}else{
				throw new Exception("cannot write in lef side");
			}
			return new Check_return(ex, f, (ArrayList<IntExpr>) indexs, class_expr, f.type, f.dims - indexs.size());

		}else{

			String ident = null;
			if(this.primary_expr.is_this){
				f = cs.this_field;
				ex = f.get_Expr(cs);
				type_info = new Type_info(cs.instance_class_name, 0);
			}else if(this.primary_expr.is_super){
				class_declaration cd = cs.Check_status_share.compilation_unit.search_class(cs.instance_class_name);
				cd = cd.super_class;
				
				f = cs.this_field.clone_e();
				f.type = cd.class_name;
				ex = f.get_Expr(cs);
				type_info = new Type_info(cd.class_name, 0);
			}else if(this.primary_expr.ident!=null){
				if(this.primary_suffixs.size() > 0 && this.primary_suffixs.get(0).is_method){
					ident = this.primary_expr.ident;
					f = cs.this_field;
					ex = f.get_Expr(cs);
				}else{
					Field searched_field = cs.search_field(primary_expr.ident, cs.instance_class_name, cs);
					if(cs.search_variable(primary_expr.ident)){
						f = cs.get_variable(primary_expr.ident);
						class_expr = cs.this_field.get_Expr(cs);
						ex = ((Variable)f).get_Expr(cs, true);
					}else if(searched_field != null){
						f = searched_field;
						class_expr = cs.this_field.get_Expr(cs);
						ex = cs.ctx.mkSelect((ArrayExpr) f.get_Expr(cs), cs.this_field.get_Expr(cs));
					}else{
						throw new Exception(cs.this_field.type + " don't have " + this.primary_expr.ident);
					}
					f.set_ref_info(ex, cs);
				}
				
				type_info = new Type_info(f.type, f.dims);

				
			}else if(this.primary_expr.bracket_expression!=null){
				Check_return cr = this.primary_expr.bracket_expression.check(cs);
				f = cr.field;
				ex = cr.expr;
				class_expr = cr.class_expr;
				indexs = cr.indexs;
				type_info = cr.type_info;
			}else if(this.primary_expr.java_literal!=null){
				throw new Exception("literal don't have suffix");
			}else{
				//new expr
				return null;
			}
			//suffixについて
			for(int i = 0; i < this.primary_suffixs.size(); i++){
				primary_suffix ps = this.primary_suffixs.get(i);
				if(ps.is_field){
					if(this.primary_suffixs.size() > i+1 && this.primary_suffixs.get(i+1).is_method){
						ident = ps.ident;
					}else{
						Field searched_field = cs.search_field(ps.ident, type_info.type, cs);
						if(searched_field != null){
							f = searched_field;
							class_expr = ex;
							ex = cs.ctx.mkSelect((ArrayExpr)f.get_Expr(cs), ex);
							f.set_ref_info(ex, cs);
							type_info = new Type_info(f.type, f.dims);
						}else{
							throw new Exception(f.type + " don't have " + ps.ident);
						}

					}
					
					indexs = new ArrayList<IntExpr>();
					
				}else if(ps.is_index){
					IntExpr index = (IntExpr)ps.expression.check(cs).expr;

					//配列のbound check
					IntExpr length = (IntExpr) cs.ctx.mkSelect(cs.ctx.mkArrayConst("length", cs.ctx.mkUninterpretedSort("ArrayRef"), cs.ctx.mkIntSort()), ex);
					BoolExpr index_bound = cs.ctx.mkGe(index, cs.ctx.mkInt(0));
					index_bound = cs.ctx.mkAnd(index_bound, cs.ctx.mkGt(length, index));
					System.out.println("check index out of bounds");
					if(!cs.in_jml_predicate)cs.assert_constraint(index_bound);
					
					indexs.add(index);
					
					Array array;
					if(indexs.size()<f.dims){
					    array = cs.array_arrayref;
					}else{
					    if(f.type.equals("int")){
					        array = cs.array_int;
					    }else if(f.type.equals("boolean")){
					        array = cs.array_boolean;
					    }else{
					        array = cs.array_ref;
					    }
					}
					ex = array.index_access_array(ex, index, cs);
					
					ident = null;
					type_info.dims--;
				}else if(ps.is_method){
					//関数の呼び出し
					if(i == this.primary_suffixs.size()-1){
						throw new Exception("The left-hand side of an assignment must be a variable");
					}
					f = method(cs, ident, f.type, ex, ps, false);
					
					class_expr = null;
					ident = null;
					indexs = new ArrayList<IntExpr>();
					if(f != null){
						ex = f.get_Expr(cs);
						type_info = new Type_info(f.type, f.dims);
					}else{
						ex = null;
						type_info = new Type_info("void", 0);
					}
				}
			}
			return new Check_return(ex, f, (ArrayList<IntExpr>) indexs, class_expr, f.type, f.dims - indexs.size());
		}
	}

	
	
	public Field method(Check_status cs, String ident, String class_type_name, Expr ex, primary_suffix ps, boolean is_super_constructor)throws Exception{
		
		System.out.println("call method " + ident);
		
		
		
		class_declaration cd = cs.Check_status_share.compilation_unit.search_class(class_type_name);
		
		//引数の処理
		List<Check_return> method_arg_valuse = new ArrayList<Check_return>();
		ArrayList<Type_info> param_types = new ArrayList<Type_info>();
		for(int j = 0; j < ps.expression_list.expressions.size(); j++){
			Check_return cr = ps.expression_list.expressions.get(j).check(cs);
			method_arg_valuse.add(cr);
			param_types.add(cr.type_info);
		}
		
		
		method_decl md = cs.Check_status_share.compilation_unit.search_method(class_type_name, ident, param_types, is_super_constructor, cs.this_field.type);
		if(md == null){
			String args = "(";
			for(int i = 0; i < param_types.size(); i++){
				args += param_types.get(i).type;
				if(i != param_types.size()-1)args += ", ";
			}
			args += ")";
			
			throw new Exception("cannot find method " + ident + args + " in class " + class_type_name);
		}
		
		if(md.formals.param_declarations.size()!=ps.expression_list.expressions.size()){
			throw new Exception("wrong number of arguments");
		}
		
		if(cs.in_jml_predicate && !md.modifiers.is_pure){
			throw new Exception("non pure method in jml predicate");
		}
		if(cs.use_only_helper_method && !md.modifiers.is_helper){
			throw new Exception("non helper method in invarinat or in refinement type");
		}
		//JML節での使えない可視性の確認
		if(cs.ban_default_visibility){
			if(md.modifiers!=null&&md.modifiers.is_private==false){
				throw new Exception("cannot use default visibility variable");
			}
		}
		if(cs.ban_private_visibility){
			if(md.modifiers!=null&&md.modifiers.is_private==true){
				throw new Exception("cannot use private visibility variable");
			}
		}
		
		//返り値
		Variable result = null;
		if(!(is_super_constructor || md.type_spec.type.type.equals("void"))){
			modifiers m_tmp = new modifiers();
			result = new Variable(cs.Check_status_share.get_tmp_num(), "return_tmp", md.type_spec.type.type, md.type_spec.dims, md.type_spec.refinement_type_clause, m_tmp, class_type_name, cs.ctx.mkBool(true));
			result.alias = cs.ctx.mkBool(true); //引数はエイリアスしている可能性がある。
			result.temp_num++;
			result.set_ref_info(result.get_Expr(cs), cs);
		}
		
		//メソッドの事前条件、事後条件にそのメソッド自身を書いた場合、制約のないただの値として返される。
		if(cs.in_jml_predicate && cs.used_methods.contains(md))return result;
		

		boolean pre_can_not_use_mutable = cs.can_not_use_mutable;
		if(cs.in_refinement_predicate) cs.can_not_use_mutable = true;
		boolean pre_in_refinement_predicate = cs.in_refinement_predicate;
		cs.in_refinement_predicate = false;
		
		
		//コンストラクタでの自インスタンスの関数呼び出し
		if(cs.in_constructor && ex.equals(cs.this_field.get_Expr(cs)) && !is_super_constructor){
			if(!md.modifiers.is_helper)cs.constructor_refinement_check();
			cs.this_alias = cs.ctx.mkOr(cs.this_alias, cs.get_pathcondition());
		}
		
		
		//helperメソッドの中でhelperでないメソッドを呼んだ場合、全ての篩型を検証する
		if((cs.in_helper || cs.in_no_refinement_type) && !(md.modifiers.is_helper || md.modifiers.is_no_refinement_type)){
			cs.assert_all_refinement_type();
		}
		
		
		
		
		//関数内の処理
		cs.in_method_call = true;
		
		cs.called_method_args = new ArrayList<Variable>();
		
		Expr pre_instance_expr = cs.instance_expr;
		String pre_instance_class_name = cs.instance_class_name;
		
		cs.instance_expr = ex;
		cs.instance_class_name = class_type_name;
		
		cs.used_methods.add(md);
		
		//可視性について
		boolean pre_ban_private_visibility = cs.ban_private_visibility;
		boolean pre_ban_default_visibility = cs.ban_default_visibility;
		cs.ban_default_visibility = false;
		if(md.modifiers != null && md.modifiers.is_private){
			cs.ban_private_visibility = false;
		}else{
			cs.ban_private_visibility = true;
		}
		
		for(int j = 0; j < md.formals.param_declarations.size(); j++){
			param_declaration pd = md.formals.param_declarations.get(j);
			modifiers m = new modifiers();
			m.is_final = pd.is_final;
			
			Variable v = new Variable(cs.Check_status_share.get_tmp_num(), pd.ident, pd.type_spec.type.type, pd.type_spec.dims, pd.type_spec.refinement_type_clause, m, class_type_name, cs.ctx.mkBool(true));
			cs.called_method_args.add(v);
			v.temp_num = 0;
			v.alias = cs.ctx.mkBool(true);
			//引数に値を紐づける
			cs.add_constraint(cs.ctx.mkEq(v.get_Expr(cs), method_arg_valuse.get(j).expr));
			v.arg_field =  method_arg_valuse.get(j).field;
			if(method_arg_valuse.get(j).field!=null) v.internal_id = method_arg_valuse.get(j).field.internal_id;
			
			//配列の篩型が安全かどうか
			cs.check_array_alias(v, ex, new ArrayList<IntExpr>(), method_arg_valuse.get(j).field, method_arg_valuse.get(j).class_expr, method_arg_valuse.get(j).indexs);
			
			
			//篩型の検証
			if(v.hava_refinement_type()){
				//篩型の中で使えるローカル変数
				if(v.refinement_type_clause.refinement_type!=null){
					v.refinement_type_clause.refinement_type.defined_variables.addAll(cs.called_method_args);
				}
				v.assert_refinement(cs, ex);
			}
			//配列がエイリアスしたときに、右辺(渡した引数)の配列の篩型の検証 　　初めてのエイリアスである可能性であるときだけ検証
			if(cs.in_helper || cs.in_no_refinement_type){
				if(v.hava_refinement_type() && v.have_index_access(cs) 
						&& method_arg_valuse.get(j).field != null && method_arg_valuse.get(j).field.hava_refinement_type() && method_arg_valuse.get(j).field.have_index_access(cs) 
						&& v.dims >= 2){
					method_arg_valuse.get(j).field.assert_refinement(cs, method_arg_valuse.get(j).class_expr);
				}
			}else if(cs.in_constructor && method_arg_valuse.get(j).field!=null && cs.this_field.get_Expr(cs).equals(method_arg_valuse.get(j).class_expr) && method_arg_valuse.get(j).field.constructor_decl_field){
				if(v.hava_refinement_type() && v.have_index_access(cs) 
						&& method_arg_valuse.get(j).field != null && method_arg_valuse.get(j).field.hava_refinement_type() && method_arg_valuse.get(j).field.have_index_access(cs) 
						&& v.dims >= 1){
					method_arg_valuse.get(j).field.assert_refinement(cs, method_arg_valuse.get(j).class_expr);
				}
			}
			
		}
		
		//事前条件
		if(!is_super_constructor){
			System.out.println("method call pre invarinats");
			if(!md.modifiers.is_helper){
				BoolExpr pre_invariant_expr = cs.all_invariant_expr();
				cs.assert_constraint(pre_invariant_expr);
			}
		}
		

		System.out.println("method call requires");
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
		System.out.println("method call assign");
		if(md.modifiers.is_pure){
			//何もしない
		}else if(md.method_specification != null){
			
			Method_Assign method_assign = md.method_specification.assignables(cs);
			
			if(md.modifiers.is_helper || md.modifiers.is_no_refinement_type){
				//helperメソッド、コンストラクタでは、配列は代入前に検証が必要な場合がある
				ArrayList<Pair<Check_return, BoolExpr>> array_assigns = method_assign.array_assign(cs);
				for(Pair<Check_return, BoolExpr> array_assign : array_assigns){
					array_assign.fst.field.assert_all_array_assign_in_helper(0, 1, array_assign.fst.class_expr, array_assign.snd, new ArrayList<IntExpr>(), cs);
				}
				
				//何でも代入できる場合
				cs.this_field.assert_all_array_assign_in_helper(0, cs.invariant_refinement_type_deep_limmit+1, null, method_assign.all_assign_condition, new ArrayList<IntExpr>(), cs);
				
				for(Variable variable : cs.called_method_args){//引数 ちょっと冗長な処理
					variable.assert_all_array_assign_in_helper(0, cs.invariant_refinement_type_deep_limmit+1, null, method_assign.all_assign_condition, new ArrayList<IntExpr>(), cs);
				}
			}
			
			System.out.println("check assign");
			for(Field f : method_assign.fields){//フィールドへの代入ができるかの検証
				Expr tmp_class_expr = cs.ctx.mkConst("tmpRef" + cs.Check_status_share.get_tmp_num(), cs.ctx.mkUninterpretedSort("Ref"));
				BoolExpr assign_condition = method_assign.assign_condition(f, tmp_class_expr, cs);
				BoolExpr assignable_condition = cs.method_assign.assign_condition(f, tmp_class_expr, cs);
				ArrayExpr alloc = cs.ctx.mkArrayConst("alloc", cs.ctx.mkUninterpretedSort("Ref"), cs.ctx.mkIntSort());
				assignable_condition = cs.ctx.mkOr(assignable_condition, cs.ctx.mkGt(cs.ctx.mkSelect(alloc, tmp_class_expr), cs.ctx.mkInt(0)), cs.method_assign.all_assign_condition);
				cs.assert_constraint(cs.ctx.mkImplies(assign_condition, assignable_condition));
			}
			//配列への代入ができるかの検証
			IntExpr tmp_index = cs.ctx.mkIntConst("tmpIdex" + cs.Check_status_share.get_tmp_num());
			Expr tmp_array_ref = cs.ctx.mkConst("tmpArrayRef" + cs.Check_status_share.get_tmp_num(), cs.ctx.mkUninterpretedSort("ArrayRef"));
			BoolExpr assign_condition = method_assign.assign_condition(tmp_array_ref, tmp_index, cs);
			BoolExpr assignable_condition = cs.method_assign.assign_condition(tmp_array_ref, tmp_index, cs);
			ArrayExpr alloc = cs.ctx.mkArrayConst("alloc_array", cs.ctx.mkUninterpretedSort("ArrayRef"), cs.ctx.mkIntSort());
			assignable_condition = cs.ctx.mkOr(assignable_condition, cs.ctx.mkGt(cs.ctx.mkSelect(alloc, tmp_array_ref), cs.ctx.mkInt(0)), cs.method_assign.all_assign_condition);
			cs.assert_constraint(cs.ctx.mkImplies(assign_condition, assignable_condition));
			
			//実際に代入する
			System.out.println("assign fields");
			method_assign.assign_all(cs);
			
			//assign_cnsts.fstに含まれないものは、assign_cnsts.sndのときに代入を行う
			cs.assert_constraint(cs.ctx.mkImplies(method_assign.all_assign_condition, cs.method_assign.all_assign_condition));
			for(Field field : cs.fields){
				cs.add_constraint(cs.ctx.mkImplies(cs.ctx.mkNot(method_assign.all_assign_condition), cs.ctx.mkEq(field.get_Expr(cs), field.get_Expr_assign(cs))));
				field.tmp_plus_with_data_group(method_assign.all_assign_condition, cs);
			}
			
			cs.refresh_all_array(method_assign.all_assign_condition);
			
		}else{//assignableを含めた任意の仕様が書かれていない関数
			
			if(md.modifiers.is_helper || md.modifiers.is_no_refinement_type){
				//helperメソッド、コンストラクタでは、代入前に検証が必要な場合がある
				//何でも代入できる場合
				cs.this_field.assert_all_array_assign_in_helper(0, cs.invariant_refinement_type_deep_limmit+1, null, cs.ctx.mkBool(true), new ArrayList<IntExpr>(), cs);
				
				for(Variable variable : cs.called_method_args){//引数 ちょっと冗長な処理
					variable.assert_all_array_assign_in_helper(0, cs.invariant_refinement_type_deep_limmit+1, null, cs.ctx.mkBool(true), new ArrayList<IntExpr>(), cs);
				}
			}

			
			for(Field f_a : cs.fields){
				f_a.tmp_plus_with_data_group(cs);
			}
			
			cs.refresh_all_array(cs.ctx.mkBool(true));
			
		}
		
		
		
		//helperメソッドやコンストラクターにおける配列のエイリアス
		if(md.array_alias(cs)){
			update_alias_in_helper_or_constructor(9999999, cs.get_pathcondition(), cs, cs.in_helper || cs.in_no_refinement_type, cs.in_constructor);
		}
		
		//返り値の処理
		if(result!=null){
			cs.result = result;
			if(result.hava_refinement_type()){
				result.add_refinement_constraint(cs, ex, true);
			}
		}
		
		//事後条件
		System.out.println("method call post invarinats");
		
		if(!md.modifiers.is_helper){
			String pre_type = null;
			if(is_super_constructor){
				pre_type = cs.this_field.type;
				cs.this_field.type = md.ident;
			}
			BoolExpr post_invariant_expr = cs.all_invariant_expr();
			if(is_super_constructor){
				cs.this_field.type = pre_type;
			}
			
			cs.add_constraint(post_invariant_expr);
		}
		
		System.out.println("method call ensures");
		BoolExpr ensures_expr = null;
		
		if(md.method_specification != null){
			ensures_expr = md.method_specification.ensures_expr(cs);
			cs.add_constraint(ensures_expr);
		}

		cs.in_method_call = false;
		
		cs.instance_expr = pre_instance_expr;
		cs.instance_class_name = pre_instance_class_name;

		
		cs.can_not_use_mutable = pre_can_not_use_mutable;
		cs.in_refinement_predicate = pre_in_refinement_predicate;
		
		cs.ban_private_visibility = pre_ban_private_visibility;
		cs.ban_default_visibility = pre_ban_default_visibility;
		
		cs.used_methods.remove(md);
		
		return result;
	}

	void add_refinement_constraint(Check_status cs, Field f, Expr class_Expr) throws Exception{
	    //System.out.println(f.field_name + " has refinement type");
    
	    cs.refinement_deep++;
	    if(cs.refinement_deep <= cs.refinement_deep_limmit){
	        f.add_refinement_constraint(cs, class_Expr);
	    }else{
	        throw new Exception("The depth of refinement type's verification has reached it's limit.");
	    }
	    cs.refinement_deep--;
	}
	
	public boolean have_index_access(Check_status cs){
		boolean have = primary_expr.have_index_access(cs);
		for(primary_suffix ps : primary_suffixs){
			have = have || ps.have_index_access(cs);
		}
		return have;
	}
	
	public Check_return loop_assign(Pair<List<F_Assign>,BoolExpr>assigned_fields, Check_status cs) throws Exception{
		
		Field f = null;
		Expr ex = null;
		String ident = null;
		Expr class_expr = cs.instance_expr;
		Type_info type_info = null;
		boolean is_refine_value = false;
		

		//suffixについて
		List<IntExpr> indexs = new ArrayList<IntExpr>();
		
		
		if(this.primary_expr.is_this){
			if(cs.this_field.get_Expr(cs).equals(cs.instance_expr)){
				f = cs.this_field;
			}else{
				f = new Dummy_Field(cs.instance_class_name, cs.instance_expr);
			}
			ex = cs.instance_expr;
			type_info = new Type_info(cs.instance_class_name, 0);
		}else if(this.primary_expr.is_super){
			class_declaration cd = cs.Check_status_share.compilation_unit.search_class(cs.instance_class_name);
			cd = cd.super_class;
			if(this.primary_suffixs.size() > 0 && this.primary_suffixs.get(0).is_method){//スーパークラスのコンストラクター 書いたけどこれあり得ないのでは？
				if(primary_expr.constructor_first == false){
					throw new Exception("calling super constructor must be the first statement in a constructor");
				}
				loop_assign_method(assigned_fields, cs, cd.class_name, cd.class_name, cs.instance_expr, this.primary_suffixs.get(0), true);
				ex = cs.instance_expr;
				class_expr = null;
				ident = null;
				indexs = new ArrayList<IntExpr>();
				type_info = new Type_info(cd.class_name, 0);
				
				if(this.primary_suffixs.size() > 1){
					throw new Exception("super constructor cannot have suffixs");
				}
				return new Check_return(ex, f, (ArrayList<IntExpr>) indexs, class_expr, type_info);
			}else{
				if(cs.this_field.get_Expr(cs).equals(cs.instance_expr)){
					f = cs.this_field.clone_e();
					f.type = cd.class_name;
				}else{
					f = new Dummy_Field(cd.class_name, cs.instance_expr);
				}
				
				ex = cs.instance_expr;
				type_info = new Type_info(cd.class_name, 0);
			}
			
		}else if(this.primary_expr.ident!=null){
			//ローカル変数
			if(cs.in_method_call){//関数呼び出し
				if(cs.search_called_method_arg(primary_expr.ident)){
					f = cs.get_called_method_arg(primary_expr.ident);
					ex = f.get_Expr(cs);
				}
				
			}else{
				if(cs.search_variable(primary_expr.ident)){
					f = cs.get_variable(primary_expr.ident);
					ex = f.get_Expr(cs);
				}
			}
			
			if(f==null){//ローカル変数ではない場合
				Field searched_field = cs.search_field(primary_expr.ident, cs.instance_class_name ,cs);
				if(this.primary_suffixs.size() > 0 && this.primary_suffixs.get(0).is_method){//メソッド
					ident = this.primary_expr.ident;
					f = new Dummy_Field(cs.instance_class_name, cs.instance_expr);
					ex = cs.instance_expr;
				}else if(searched_field != null){//フィールド
					f = searched_field;
					ex = cs.ctx.mkSelect((ArrayExpr) f.get_Expr(cs),cs.instance_expr);
					class_expr = cs.instance_expr;
					f.set_ref_info(ex, cs);
				}else{
					throw new Exception(cs.instance_class_name + " don't have " + this.primary_expr.ident);
				}
			}
			type_info = new Type_info(f.type, f.dims);
			
		}else if(this.primary_expr.bracket_expression!=null){
			Check_return cr = this.primary_expr.bracket_expression.loop_assign(assigned_fields, cs);
			f = cr.field;
			ex = cr.expr;
			class_expr = cr.class_expr;
			indexs = cr.indexs;
			type_info = cr.type_info;
		}else if(this.primary_expr.java_literal!=null){
			if(this.primary_suffixs.size() == 0){
				Check_return cr = this.primary_expr.java_literal.check(cs);
				ex = cr.expr;
				type_info = cr.type_info;
			}else{
				throw new Exception("literal don't have suffix");
				
			}
		}else if(this.primary_expr.jml_primary!=null){
			//jml内は見ない
		}else if(this.primary_expr.new_expr!=null){
			this.primary_expr.new_expr.loop_assign(assigned_fields, cs);
		}else{
			//return null;
		}
		
		for(int i = 0; i < this.primary_suffixs.size(); i++){
			primary_suffix ps = this.primary_suffixs.get(i);
			if(ps.is_field){
				if(this.primary_suffixs.size() > i+1 && this.primary_suffixs.get(i+1).is_method){
					ident = ps.ident;
				}else if(f.dims > indexs.size() && ps.ident.equals("length")){
					if(this.primary_suffixs.size()-1 != i) throw new Exception("length don't have suffix");
					
					ex = (IntExpr) cs.ctx.mkSelect(cs.ctx.mkArrayConst("length", cs.ctx.mkUninterpretedSort("ArrayRef"), cs.ctx.mkIntSort()), ex);
					f = null;
					class_expr = null;
					type_info = new Type_info("int", 0);
				}else{
					Field pre_f = f;
					class_expr = ex;
					
					Field searched_field = cs.search_field(ps.ident, type_info.type, cs);
					if(searched_field != null){
						f = searched_field;
						ex = cs.ctx.mkSelect((ArrayExpr)f.get_Expr(cs), ex);
						if(cs.can_not_use_mutable){
							if(!f.modifiers.is_final){
								new Exception("method depends on mutable field in refinenment type predicate.");
							}
						}
						type_info = new Type_info(f.type, f.dims);
					}else{
						throw new Exception(f.type + " don't have " + ps.ident);
					}
				}
				
				indexs = new ArrayList<IntExpr>();
				
			}else if(ps.is_index){
				IntExpr index =  (IntExpr) ps.expression.loop_assign(assigned_fields, cs).expr;
				
				indexs.add(index);
				Array array;
				if(indexs.size()<f.dims){
				    array = cs.array_arrayref;
				}else{
				    if(f.type.equals("int")){
				        array = cs.array_int;
				    }else if(f.type.equals("boolean")){
				        array = cs.array_boolean;
				    }else{
				        array = cs.array_ref;
				    }
				}
				ex = array.index_access_array(ex, index, cs);
				ident = null;
				type_info.dims--;
				
			}else if(ps.is_method){
				//関数の呼び出し
				f = loop_assign_method(assigned_fields, cs, ident, f.type, ex, ps, false);
				
				class_expr = null;
				ident = null;
				indexs = new ArrayList<IntExpr>();
				if(f != null){
					ex = f.get_Expr(cs);
					type_info = new Type_info(f.type, f.dims);
				}else{
					ex = null;
					type_info = new Type_info("void", 0);
				}
			}
		}

		
		return new Check_return(ex, f, (ArrayList<IntExpr>) indexs, class_expr, type_info);
	}
	
	public Field loop_assign_method(Pair<List<F_Assign>,BoolExpr>assigned_fields, Check_status cs, String ident, String class_type_name, Expr ex, primary_suffix ps, boolean is_super_constructor)throws Exception{
		
		
		class_declaration cd = cs.Check_status_share.compilation_unit.search_class(class_type_name);
		
		//引数の処理
		List<Check_return> method_arg_valuse = new ArrayList<Check_return>();
		ArrayList<Type_info> param_types = new ArrayList<Type_info>();
		for(int j = 0; j < ps.expression_list.expressions.size(); j++){
			Check_return cr = ps.expression_list.expressions.get(j).check(cs);
			method_arg_valuse.add(cr);
			param_types.add(cr.type_info);
		}
		
		method_decl md = cs.Check_status_share.compilation_unit.search_method(class_type_name, ident, param_types, is_super_constructor, cs.this_field.type);
		if(md == null){
			throw new Exception("can't find method " + ident);
		}
		
		
		//関数内の処理
		cs.in_method_call = true;
		
		cs.called_method_args = new ArrayList<Variable>();
		
		Expr pre_instance_expr = cs.instance_expr;
		String pre_instance_class_name = cs.instance_class_name;
		
		cs.instance_expr = ex;
		cs.instance_class_name = class_type_name;
		
		for(int j = 0; j < md.formals.param_declarations.size(); j++){
			param_declaration pd = md.formals.param_declarations.get(j);
			modifiers m = new modifiers();
			m.is_final = pd.is_final;
			Variable v = new Variable(cs.Check_status_share.get_tmp_num(), pd.ident, pd.type_spec.type.type, pd.type_spec.dims, pd.type_spec.refinement_type_clause, m, class_type_name, cs.ctx.mkBool(true));
			cs.called_method_args.add(v);
			v.temp_num = 0;
			//引数に値を紐づける
			cs.add_constraint(cs.ctx.mkEq(v.get_Expr(cs), method_arg_valuse.get(j).expr));
			v.arg_field =  method_arg_valuse.get(j).field;
			if(method_arg_valuse.get(j).field!=null) v.internal_id = method_arg_valuse.get(j).field.internal_id;
			
		}
		
		//assign
		if(md.method_specification != null){
			md.method_specification.requires_expr(cs);//事前条件は作る必要がある	
			
			Method_Assign method_assign = md.method_specification.assignables(cs);
			method_assign.loop_assign(assigned_fields, cs);
		}else{
			//assignableを含めた任意の仕様が書かれていない関数
			assigned_fields.snd = cs.ctx.mkOr(assigned_fields.snd, cs.get_pathcondition());
		}
		
		//helperメソッドやコンストラクターにおける配列のエイリアス
		//loop_assign_methodではpre_in_helperなどは用意しない
		if(md.array_alias(cs)){
			update_alias_in_helper_or_constructor(9999999, cs.get_pathcondition(), cs, cs.in_helper || cs.in_no_refinement_type, cs.in_constructor);
		}
		
		
		//返り値
		Variable result = null;
		if(!(is_super_constructor || md.type_spec.type.type.equals("void"))){
			modifiers m_tmp = new modifiers();
			result = new Variable(cs.Check_status_share.get_tmp_num(), "return_tmp", md.type_spec.type.type, md.type_spec.dims, md.type_spec.refinement_type_clause, m_tmp, class_type_name, cs.ctx.mkBool(true));
			result.temp_num++;
		}
		
		cs.in_method_call = false;
		
		cs.instance_expr = pre_instance_expr;
		cs.instance_class_name = pre_instance_class_name;
		
		return result;
	}
	
	//メソッド呼び出しの際にalias_in_helper_or_consutructorとalias_2d_in_helper_or_consutructorを更新する
	//cs.in_helperとかは呼び出しているメソッドのもの　呼び出し元のメソッドがどうなのかが必要なので引数として渡す
	public void update_alias_in_helper_or_constructor(int dim, BoolExpr condition, Check_status cs, boolean in_helper, boolean in_constructor) throws Exception{
		//1次元以上の配列としてエイリアスした場合には、それ以降配列を代入する前に篩型の検証を行わなければならない
		if(1 <= dim){
			if(in_helper){
				for(Variable v : cs.called_method_args){
					if(v.arg_field!=null && v.arg_field instanceof Variable && v.dims>=1 && v.hava_refinement_type() && v.have_index_access(cs)){
						v.arg_field.alias_1d_in_helper = cs.ctx.mkOr(v.arg_field.alias_1d_in_helper, condition);
					}
				}
				if(in_constructor){
					for(Field v : cs.fields){
						if(v.dims>=1 && v.hava_refinement_type() && v.have_index_access(cs)){
							v.alias_1d_in_helper = cs.ctx.mkOr(v.alias_1d_in_helper, condition);
						}
					}
				}
			}else if(in_constructor){//コンストラクタでは、それ以降篩型を満たさなければいけない
				for(Field v : cs.fields){
					if(v.dims>=1 && v.hava_refinement_type() && v.have_index_access(cs)){
						v.alias_in_consutructor_or_2d_in_helper = cs.ctx.mkOr(v.alias_in_consutructor_or_2d_in_helper, condition);
					}
				}
			}
		}
		//2次元以上の配列としてエイリアスした場合には、それ以降篩型を満たさなければいけない
		if(2 <= dim){
			if(in_helper){
				for(Variable v : cs.called_method_args){
					if(v.arg_field!=null && v.arg_field instanceof Variable && v.dims>=2 && v.hava_refinement_type() && v.have_index_access(cs)){
						v.arg_field.alias_in_consutructor_or_2d_in_helper = cs.ctx.mkOr(v.arg_field.alias_in_consutructor_or_2d_in_helper, condition);
					}
				}
				if(in_constructor){
					for(Field v : cs.fields){
						if(v.dims>=1 && v.hava_refinement_type() && v.have_index_access(cs)){
							v.alias_in_consutructor_or_2d_in_helper = cs.ctx.mkOr(v.alias_in_consutructor_or_2d_in_helper, condition);
						}
					}
				}
			}
		}
	}
	
}

