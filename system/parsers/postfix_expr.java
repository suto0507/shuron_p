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
import system.Model_Field;
import system.Pair;
import system.Parser;
import system.Parser_status;
import system.Quantifier_Variable;
import system.Source;
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
		Expr class_expr = null;
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
				//ローカル変数
				if(cs.in_method_call){//関数呼び出し
					if(cs.search_called_method_arg(primary_expr.ident)){
						f = cs.get_called_method_arg(primary_expr.ident);
						ex = f.get_Expr(cs);
					}
					
				}else if(cs.in_refinement_predicate){//篩型
					if(cs.search_variable(this.primary_expr.ident)){
						f = cs.get_variable(this.primary_expr.ident);
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
						}else if(this.primary_suffixs.size() > 0 && this.primary_suffixs.get(0).is_method){//メソッド
							ident = this.primary_expr.ident;
							f = new Dummy_Field(cs.instance_class_name, cs.instance_expr);
							ex = cs.instance_expr;
						}else{
							throw new Exception(cs.instance_class_name + " don't have " + this.primary_expr.ident);
						}
					}
				}

				if(cs.in_refinement_predicate==true){
					if((f.modifiers!=null&&f.modifiers.is_final==false) || f instanceof Model_Field){//篩型の中ではfinalである必要がある
						throw new Exception("can use only final variable in refinement type");
					}
				}
				//JML節での使えない可視性の確認
				if(cs.ban_default_visibility){
					if(f.modifiers!=null&&f.modifiers.is_private==false){
						throw new Exception("can not use default visibility variable");
					}
				}
				if(cs.ban_private_visibility){
					if(f.modifiers!=null&&f.modifiers.is_private==true){
						throw new Exception("can not use private visibility variable");
					}
				}
			}
		}else if(this.primary_expr.bracket_expression!=null){
			Check_return cr = this.primary_expr.bracket_expression.check(cs);
			f = cr.field;
			ex = cr.expr;
			class_expr = cr.class_expr;
			indexs = cr.indexs;
		}else if(this.primary_expr.java_literal!=null){
			if(this.primary_suffixs.size() == 0){
				ex = this.primary_expr.java_literal.check(cs);
			}else{
				throw new Exception("literal don't have suffix");
				
			}
		}else if(this.primary_expr.jml_primary!=null){
			if(cs.in_method_call){
				if(this.primary_expr.jml_primary.is_result){
					f = cs.result;
					ex = f.get_Expr(cs);
				}else if(this.primary_expr.jml_primary.old_expression!=null){
					Check_return cr = this.primary_expr.jml_primary.old_expression.spec_expression.check(cs.old_status);
					f = cr.field;
					ex = cr.expr;
					class_expr = cr.class_expr;
					indexs = cr.indexs;
				}else if(this.primary_expr.jml_primary.spec_quantified_expr!=null){
					ex = this.primary_expr.jml_primary.spec_quantified_expr.check(cs);
					if(this.primary_suffixs.size() != 0) throw new Exception("quantifier don't have suffix");
				}
			}else{
				if(this.primary_expr.jml_primary.is_result){
					f = cs.return_v;
					ex = cs.return_expr;
					//return ex;
				}else if(this.primary_expr.jml_primary.old_expression!=null){
					Check_return cr = this.primary_expr.jml_primary.old_expression.spec_expression.check(cs.this_old_status);
					f = cr.field;
					ex = cr.expr;
					class_expr = cr.class_expr;
					indexs = cr.indexs;
				}else if(this.primary_expr.jml_primary.spec_quantified_expr!=null){
					ex = this.primary_expr.jml_primary.spec_quantified_expr.check(cs);
					if(this.primary_suffixs.size() != 0) throw new Exception("quantifier don't have suffix");
				}
			}
		}else if(this.primary_expr.new_expr!=null){
			f = this.primary_expr.new_expr.check(cs);
			ex = f.get_Expr(cs);
		}else{
			//return null;
		}
		
		if(f!=null && f.hava_refinement_type() && is_refine_value==false){//篩型			
			add_refinement_constraint(cs, f, cs.instance_expr);
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
				}else{
					Field pre_f = f;
					class_expr = ex;
					
					Field searched_field = cs.search_field(ps.ident, f.type, cs);
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
							throw new Exception("can not use default visibility variable");
						}
					}
					if(cs.ban_private_visibility){
						if(f.modifiers!=null&&f.modifiers.is_private==true){
							throw new Exception("can not use private visibility variable");
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
				if(cs.in_refinement_predicate==true){//篩型の中では配列は使えない
					if(!f.equals(cs.refined_Field)){
						if(cs.search_quantifier(f.field_name, cs)==null){
							throw new Exception("can not use array in refinement type");
						}
					}
				}
				
			}else if(ps.is_method){
				//関数の呼び出し
				f = method(cs, ident, f.type, ex, ps);
				ex = f.get_Expr(cs);
				class_expr = null;
				ident = null;
				indexs = new ArrayList<IntExpr>();
			}
		}

		
		return new Check_return(ex, f, (ArrayList<IntExpr>) indexs, class_expr);
	}
	
	public Check_return check_assign(Check_status cs) throws Exception{
		Expr ex = null;
		Field f = null;
		Expr class_expr = null;
		ArrayList<IntExpr> indexs = new ArrayList<IntExpr>();
		
		if(this.primary_suffixs.size() == 0){
			if(this.primary_expr.is_this){
				//これはアウト
				throw new Exception("can't assign this");
			}if(this.primary_expr.ident!=null){

				Field searched_field = cs.search_field(primary_expr.ident, cs.this_field.type, cs);
				if(cs.search_variable(primary_expr.ident)){
					f = cs.get_variable(primary_expr.ident);
					class_expr = cs.this_field.get_Expr(cs);
					ex = ((Variable)f).get_Expr(cs, true);
				}else if(searched_field != null){
					f = searched_field;
					class_expr = cs.this_field.get_Expr(cs);
					ex = f.get_Expr(cs);
				}else{
					throw new Exception(cs.this_field.type + " don't have " + this.primary_expr.ident);
				}

			}else if(this.primary_expr.java_literal!=null){
				throw new Exception("can't assign java literal");
			}else{
				throw new Exception("can't write in lef side");
			}
			return new Check_return(ex, f, (ArrayList<IntExpr>) indexs, class_expr);

		}else{

			String ident = null;
			if(this.primary_expr.is_this){
				f = cs.this_field;
				ex = f.get_Expr(cs);
			}else if(this.primary_expr.ident!=null){

				Field searched_field = cs.search_field(primary_expr.ident, cs.this_field.type, cs);
				if(cs.search_variable(primary_expr.ident)){
					f = cs.get_variable(primary_expr.ident);
					class_expr = cs.this_field.get_Expr(cs);
					ex = ((Variable)f).get_Expr(cs, true);
				}else if(searched_field != null){
					f = searched_field;
					class_expr = cs.this_field.get_Expr(cs);
					ex = cs.ctx.mkSelect((ArrayExpr) f.get_Expr(cs), cs.this_field.get_Expr(cs));
				}else if(this.primary_suffixs.size() > 0 && this.primary_suffixs.get(0).is_method){
					ident = this.primary_expr.ident;
					f = cs.this_field;
					ex = f.get_Expr(cs);
				}else{
					throw new Exception(cs.this_field.type + " don't have " + this.primary_expr.ident);
				}

				
			}else if(this.primary_expr.bracket_expression!=null){
				Check_return cr = this.primary_expr.bracket_expression.check(cs);
				f = cr.field;
				ex = cr.expr;
				class_expr = cr.class_expr;
				indexs = cr.indexs;
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
						Field searched_field = cs.search_field(ps.ident, f.type, cs);
						if(searched_field != null){
							f = searched_field;
							class_expr = ex;
							ex = cs.ctx.mkSelect((ArrayExpr)f.get_Expr(cs), ex);
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
				}else if(ps.is_method){
					//関数の呼び出し
					if(i == this.primary_suffixs.size()-1){
						throw new Exception("The left-hand side of an assignment must be a variable");
					}
					f = method(cs, ident, f.type, ex, ps);
					ex = f.get_Expr(cs);
					cs.in_method_call = false;
					ident = null;
					
					indexs = new ArrayList<IntExpr>();
				}
			}
			return new Check_return(ex, f, (ArrayList<IntExpr>) indexs, class_expr);
		}
	}

	
	
	public Field method(Check_status cs, String ident, String class_type_name, Expr ex, primary_suffix ps)throws Exception{
		
		System.out.println("call method " + ident);
		
		
		
		class_declaration cd = cs.Check_status_share.compilation_unit.search_class(class_type_name);
		method_decl md = cs.Check_status_share.compilation_unit.search_method(class_type_name, ident);
		if(md == null){
			throw new Exception("can't find method " + ident + "in class " + class_type_name);
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
		
		//返り値
		modifiers m_tmp = new modifiers();
		Variable result = new Variable(cs.Check_status_share.get_tmp_num(), "return_tmp", md.type_spec.type.type, md.type_spec.dims, md.type_spec.refinement_type_clause, m_tmp, class_type_name, cs.ctx.mkBool(true));
		result.alias = cs.ctx.mkBool(true); //引数はエイリアスしている可能性がある。
		result.temp_num++;
		
		//メソッドの事前条件、事後条件にそのメソッド自身を書いた場合、制約のないただの値として返される。
		if(cs.in_jml_predicate && cs.used_methods.contains(md))return result;
		

		boolean pre_can_not_use_mutable = cs.can_not_use_mutable;
		if(cs.in_refinement_predicate) cs.can_not_use_mutable = true;
		boolean pre_in_refinement_predicate = cs.in_refinement_predicate;
		cs.in_refinement_predicate = false;
		
		
		//コンストラクタでの自インスタンスの関数呼び出し
		if(cs.in_constructor && ex.equals(cs.this_field.get_Expr(cs))){
			if(!md.modifiers.is_helper)cs.constructor_refinement_check();
			cs.this_alias = cs.ctx.mkOr(cs.this_alias, cs.get_pathcondition());
		}
		
		
		//helperメソッドの中でhelperでないメソッドを呼んだ場合、全ての篩型を検証する
		if(cs.in_helper && !md.modifiers.is_helper){
			cs.assert_all_refinement_type();
		}
		
		
		
		//引数の処理
		List<Check_return> method_arg_valuse = new ArrayList<Check_return>();
		for(int j = 0; j < md.formals.param_declarations.size(); j++){
			method_arg_valuse.add(ps.expression_list.expressions.get(j).check(cs));
		}
		//関数内の処理
		cs.in_method_call = true;
		
		cs.called_method_args = new ArrayList<Variable>();
		
		Expr pre_instance_expr = cs.instance_expr;
		String pre_instance_class_name = cs.instance_class_name;
		
		cs.instance_expr = ex;
		cs.instance_class_name = class_type_name;
		
		cs.used_methods.add(md);
		
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
				v.assert_refinement(cs, ex);
			}
			//配列がエイリアスしたときに、右辺(渡した引数)の配列の篩型の検証 　　初めてのエイリアスである可能性であるときだけ検証
			if(cs.in_helper){
				if(v.hava_refinement_type() && v.have_index_access(cs) 
						&& method_arg_valuse.get(j).field != null && method_arg_valuse.get(j).field.hava_refinement_type() && method_arg_valuse.get(j).field.have_index_access(cs) 
						&& v.dims >= 2){
					method_arg_valuse.get(j).field.assert_refinement(cs, method_arg_valuse.get(j).class_expr);
				}
			}else if(cs.in_constructor && cs.this_field.get_Expr(cs).equals(method_arg_valuse.get(j).class_expr)){
				if(v.hava_refinement_type() && v.have_index_access(cs) 
						&& method_arg_valuse.get(j).field != null && method_arg_valuse.get(j).field.hava_refinement_type() && method_arg_valuse.get(j).field.have_index_access(cs) 
						&& v.dims >= 1){
					method_arg_valuse.get(j).field.assert_refinement(cs, method_arg_valuse.get(j).class_expr);
				}
			}
			
		}
		
		//事前条件
		System.out.println("method call pre invarinats");
		if(!md.modifiers.is_helper){
			BoolExpr pre_invariant_expr = cs.all_invariant_expr();
			cs.assert_constraint(pre_invariant_expr);
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
			
			Pair<List<F_Assign>, BoolExpr> assign_cnsts = md.method_specification.assignables(cs);
			
			//helperメソッド、コンストラクタでは、配列は代入前に検証が必要な場合がある
			for(F_Assign fa : assign_cnsts.fst){
				for(Pair<BoolExpr,List<Pair<Expr, List<IntExpr>>>> b_indexs : fa.cnst_array){
					for(Pair<Expr, List<IntExpr>> assign_indexs : b_indexs.snd){
						if(assign_indexs.snd.size() < fa.field.dims){
							fa.field.assert_all_array_assign_in_helper(0, 1, assign_indexs.fst, b_indexs.fst, new ArrayList<IntExpr>(), cs);
						}
					}
				}
			}
			//何でも代入できる場合
			cs.this_field.assert_all_array_assign_in_helper(0, cs.invariant_refinement_type_deep_limmit+1, null, assign_cnsts.snd, new ArrayList<IntExpr>(), cs);
			
			for(Variable variable : cs.called_method_args){//引数 ちょっと冗長な処理
				variable.assert_all_array_assign_in_helper(0, cs.invariant_refinement_type_deep_limmit+1, null, assign_cnsts.snd, new ArrayList<IntExpr>(), cs);
			}
			
			
			
			for(F_Assign fa : assign_cnsts.fst){

				BoolExpr assign_expr = null;
				
				//引数のフィールドに代入する場合には、紐づけられたフィールドに代入する
				if(fa.field instanceof Variable && ((Variable)fa.field).arg_field!=null && !((fa.field.type.equals("int") || fa.field.type.equals("boolean")) && fa.field.dims==0)){
					fa.field = ((Variable)fa.field).arg_field;
				}
				
				//配列の要素に代入できる制約
				if(fa.cnst_array.size()>0){
					for(int i = 0; i <= fa.field.dims; i++){//各次元に関して
						List<IntExpr> index_expr = new ArrayList<IntExpr>();
						for(int j = 0; j < i; j++){
							index_expr.add(cs.ctx.mkIntConst("tmpIdex" + cs.Check_status_share.get_tmp_num()));
						}
						Expr tmp_class_expr = cs.ctx.mkConst("tmpRef" + cs.Check_status_share.get_tmp_num(), cs.ctx.mkUninterpretedSort("Ref"));
						BoolExpr call_assign_expr = fa.assign_index_expr(tmp_class_expr, index_expr, cs);
						BoolExpr field_assign_expr = fa.field.assign_index_expr(tmp_class_expr, index_expr, cs);
						
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
				
				
				//代入
				if(fa.cnst_array.size()>0){
					fa.assign_fresh_value(cs);
				}
				
				
			}
			
			
			//assign_cnsts.fstに含まれないものは、assign_cnsts.sndのときに代入を行う
			cs.assert_constraint(cs.ctx.mkImplies(assign_cnsts.snd, cs.assinable_cnst_all));
			for(Field field : cs.fields){
				cs.add_constraint(cs.ctx.mkImplies(cs.ctx.mkNot(assign_cnsts.snd), cs.ctx.mkEq(field.get_Expr(cs), field.get_Expr_assign(cs))));
				field.tmp_plus_with_data_group(assign_cnsts.snd, cs);
			}
			
			cs.refresh_all_array(assign_cnsts.snd);
			
			
		}else{//assignableを含めた任意の仕様が書かれていない関数
			
			//helperメソッド、コンストラクタでは、代入前に検証が必要な場合がある
			//何でも代入できる場合
			cs.this_field.assert_all_array_assign_in_helper(0, cs.invariant_refinement_type_deep_limmit+1, null, cs.ctx.mkBool(true), new ArrayList<IntExpr>(), cs);
			
			for(Variable variable : cs.called_method_args){//引数 ちょっと冗長な処理
				variable.assert_all_array_assign_in_helper(0, cs.invariant_refinement_type_deep_limmit+1, null, cs.ctx.mkBool(true), new ArrayList<IntExpr>(), cs);
			}

			
			for(Field f_a : cs.fields){
				f_a.tmp_plus_with_data_group(cs);
			}
			
			cs.refresh_all_array(cs.ctx.mkBool(true));
			
		}
		
		
		
		//helperメソッドやコンストラクターにおける配列のエイリアス
		update_alias_in_helper_or_constructor(9999999, cs.get_pathcondition(), cs, cs.in_helper, cs.in_constructor);
				
		
		//返り値の処理
		cs.result = result;
		if(result.hava_refinement_type()){
			result.add_refinement_constraint(cs, ex, true);
		}
		
		//事後条件
		System.out.println("method call post invarinats");
		
		if(!md.modifiers.is_helper){
			BoolExpr post_invariant_expr = cs.all_invariant_expr();
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
		
		cs.used_methods.remove(md);
		
		return result;
	}

	void add_refinement_constraint(Check_status cs, Field f, Expr class_Expr) throws Exception{
	    System.out.println(f.field_name + " has refinement type");
    
	    cs.refinement_deep++;
	    if(cs.refinement_deep <= cs.refinement_deep_limmit){
	        f.add_refinement_constraint(cs, class_Expr);
	    }else{
	        throw new Exception("The depth of refinement type's verification has reached its limit.");
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
		Expr class_expr = null;
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
				if(searched_field != null){//フィールド
					f = searched_field;
					ex = cs.ctx.mkSelect((ArrayExpr) f.get_Expr(cs),cs.instance_expr);
					class_expr = cs.instance_expr;
				}else if(this.primary_suffixs.size() > 0 && this.primary_suffixs.get(0).is_method){//メソッド
					ident = this.primary_expr.ident;
					f = new Dummy_Field(cs.instance_class_name, cs.instance_expr);
					ex = cs.instance_expr;
				}else{
					throw new Exception(cs.instance_class_name + " don't have " + this.primary_expr.ident);
				}
			}

			
		}else if(this.primary_expr.bracket_expression!=null){
			Check_return cr = this.primary_expr.bracket_expression.loop_assign(assigned_fields, cs);
			f = cr.field;
			ex = cr.expr;
			class_expr = cr.class_expr;
			indexs = cr.indexs;
		}else if(this.primary_expr.java_literal!=null){
			if(this.primary_suffixs.size() == 0){
				ex = this.primary_expr.java_literal.check(cs);
			}else{
				throw new Exception("literal don't have suffix");
				
			}
		}else if(this.primary_expr.jml_primary!=null){
			//jml内は見ない
		}else if(this.primary_expr.new_expr!=null){
			//新しく作るオブジェクトも関係ない
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
				}else{
					Field pre_f = f;
					class_expr = ex;
					
					Field searched_field = cs.search_field(ps.ident, f.type, cs);
					if(searched_field != null){
						f = searched_field;
						ex = cs.ctx.mkSelect((ArrayExpr)f.get_Expr(cs), ex);
						if(cs.can_not_use_mutable){
							if(!f.modifiers.is_final){
								new Exception("method depends on mutable field in refinenment type predicate.");
							}
						}
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
				
			}else if(ps.is_method){
				//関数の呼び出し
				f = loop_assign_method(assigned_fields, cs, ident, f.type, ex, (ArrayList<IntExpr>) indexs, ps);
				ex = f.get_Expr(cs);
				class_expr = null;
				ident = null;
				indexs = new ArrayList<IntExpr>();
			}
		}

		
		return new Check_return(ex, f, (ArrayList<IntExpr>) indexs, class_expr);
	}
	
	public Field loop_assign_method(Pair<List<F_Assign>,BoolExpr>assigned_fields, Check_status cs, String ident, String class_type_name, Expr ex, ArrayList<IntExpr> indexs, primary_suffix ps)throws Exception{
		
		
		class_declaration cd = cs.Check_status_share.compilation_unit.search_class(class_type_name);
		method_decl md = cs.Check_status_share.compilation_unit.search_method(class_type_name, ident);
		if(md == null){
			throw new Exception("can't find method " + ident);
		}
		
		
		//引数の処理
		List<Check_return> method_arg_valuse = new ArrayList<Check_return>();
		for(int j = 0; j < md.formals.param_declarations.size(); j++){
			method_arg_valuse.add(ps.expression_list.expressions.get(j).check(cs));
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
			
			Pair<List<F_Assign>, BoolExpr> assign_cnsts = md.method_specification.assignables(cs);
			for(F_Assign fa : assign_cnsts.fst){
				
				boolean find_field = false;
				for(F_Assign f_i : assigned_fields.fst){
					if(f_i.field.equals(fa.field, cs) ){//見つかったら追加する
						find_field = true;
						for(Pair<BoolExpr,List<Pair<Expr, List<IntExpr>>>> b_i : fa.cnst_array){
							f_i.cnst_array.add(new Pair<BoolExpr,List<Pair<Expr, List<IntExpr>>>>(cs.ctx.mkAnd(cs.get_pathcondition(), b_i.fst), b_i.snd));
						}
						break;
					}
				}
				//見つからなかったら新しくフィールドごと追加する
				if(!find_field){
					List<Pair<BoolExpr,List<Pair<Expr, List<IntExpr>>>>> b_is = new ArrayList<Pair<BoolExpr,List<Pair<Expr, List<IntExpr>>>>>();
					for(Pair<BoolExpr,List<Pair<Expr, List<IntExpr>>>> b_i : fa.cnst_array){
						b_is.add(new Pair<BoolExpr,List<Pair<Expr, List<IntExpr>>>>(cs.ctx.mkAnd(cs.get_pathcondition(), b_i.fst), b_i.snd));
					}
					assigned_fields.fst.add(new F_Assign(fa.field, b_is));
				}
			}
			
			//なんでも代入できる場合
			assigned_fields.snd = cs.ctx.mkOr(assigned_fields.snd, cs.ctx.mkAnd(cs.get_pathcondition(), assign_cnsts.snd));
		}else{
			//assignableを含めた任意の仕様が書かれていない関数
			assigned_fields.snd = cs.ctx.mkOr(assigned_fields.snd, cs.get_pathcondition());
		}
		
		//helperメソッドやコンストラクターにおける配列のエイリアス
		//loop_assign_methodではpre_in_helperなどは用意しない
		update_alias_in_helper_or_constructor(9999999, cs.get_pathcondition(), cs, cs.in_helper, cs.in_constructor);

		
		
		//返り値
		modifiers m_tmp = new modifiers();
		Variable result = new Variable(cs.Check_status_share.get_tmp_num(), "return_tmp", md.type_spec.type.type, md.type_spec.dims, md.type_spec.refinement_type_clause, m_tmp, class_type_name, cs.ctx.mkBool(true));
		result.temp_num++;
		
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

