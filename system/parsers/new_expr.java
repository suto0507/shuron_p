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
import system.Field;
import system.Pair;
import system.Parser;
import system.Parser_status;
import system.Source;
import system.Variable;
import system.F_Assign;

public class new_expr implements Parser<String>{
	String type;
	new_suffix new_suffix;
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		st = st + new string("new").parse(s, ps);
		st = st + new spaces().parse(s, ps);
		type type = new type();
		st = st + type.parse(s, ps);
		this.type = type.type;
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
			ret = new Variable(cs.Check_status_share.get_tmp_num(), "new_" + this.type + "_array_tmp", this.type, this.new_suffix.array_decl.dims, null, new modifiers(), cs.this_field.type, cs.ctx.mkBool(false));
			ret.temp_num++;
			ret.new_array = true;
			

			List<IntExpr> lengths = this.new_suffix.array_decl.check(cs);
			
			IntExpr[] tmps = new IntExpr[lengths.size()-1];
			IntExpr[] tmps_full = new IntExpr[this.new_suffix.array_decl.dims];
			ArrayList<IntExpr> tmp_list = new ArrayList<IntExpr>();
			BoolExpr guard = null;
			BoolExpr length_cnst = null;
			
			Expr ex = ret.get_Expr(cs);
			
			//newで新しく作ったrefは被らないことを表すための制約
			ArrayExpr alloc = cs.ctx.mkArrayConst("alloc_array", cs.ctx.mkUninterpretedSort("ArrayRef"), cs.ctx.mkIntSort());
			BoolExpr constraint = cs.ctx.mkEq(cs.ctx.mkSelect(alloc, ex), cs.ctx.mkInt(cs.Check_status_share.get_tmp_num()));
			cs.add_constraint(constraint);
			
			
			for(int i = 0; i < lengths.size(); i++){
				
				//lengthに関する制約
				IntExpr length = (IntExpr) cs.ctx.mkSelect(cs.ctx.mkArrayConst("length", cs.ctx.mkUninterpretedSort("ArrayRef"), cs.ctx.mkIntSort()), ex);
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
				Array array;
				alloc = null;
				if(i<lengths.size()-1){
				    array = cs.array_arrayref;
			        alloc = cs.ctx.mkArrayConst("alloc_array", cs.ctx.mkUninterpretedSort("ArrayRef"), cs.ctx.mkIntSort());
				}else{
				    if(this.type.equals("int")){
				        array = cs.array_int;
				    }else if(this.type.equals("boolean")){
				        array = cs.array_boolean;
				    }else{
				        array = cs.array_ref;
				        alloc = cs.ctx.mkArrayConst("alloc", cs.ctx.mkUninterpretedSort("Ref"), cs.ctx.mkIntSort());
				    }
				}
				
				Expr pre_ex = ex;
				ex = array.index_access_array(ex, index, cs);
				
				//newで新しく作ったrefは被らないことを表すための制約
				if(alloc != null){//配列か、参照型
					constraint = cs.ctx.mkEq(cs.ctx.mkSelect(alloc, ex), cs.ctx.mkInt(cs.Check_status_share.get_tmp_num()));
					
					IntExpr[] fresh_tmps = new IntExpr[tmp_list.size()];
					for(int j = 0; j < tmp_list.size(); j++){
						fresh_tmps[j] = tmp_list.get(i);
					}
					constraint = cs.ctx.mkForall(fresh_tmps, cs.ctx.mkImplies(guard, constraint), 1, null, null, null, null);
					
					cs.add_constraint(constraint);
					
					//この配列の要素同士が別の参照であるということの制約を加える
					IntExpr index_another = cs.ctx.mkIntConst("tmpIdex_another" + cs.Check_status_share.get_tmp_num());
					BoolExpr elements_guard = cs.ctx.mkAnd(
							guard
							, cs.ctx.mkGe(index, cs.ctx.mkInt(0)), cs.ctx.mkGt(lengths.get(i), index_another)
							, cs.ctx.mkDistinct(index, index_another));
					Expr ex_another = array.index_access_array(pre_ex, index_another, cs);
					IntExpr[] fresh_tmps_another = new IntExpr[tmp_list.size()+1];
					for(int j = 0; j < tmp_list.size(); j++){
						fresh_tmps_another[j] = tmp_list.get(i);
					}
					fresh_tmps_another[tmp_list.size()] = index_another;
					BoolExpr elements_constraint = cs.ctx.mkForall(fresh_tmps_another, cs.ctx.mkImplies(elements_guard, cs.ctx.mkDistinct(ex, ex_another)), 1, null, null, null, null);
					
					cs.add_constraint(elements_constraint);
				}
				
				
			}
			
			
			//配列の初期値についての制約
			for(int i = lengths.size(); i < this.new_suffix.array_decl.dims; i++){
				IntExpr index = cs.ctx.mkIntConst("tmpIdex" + cs.Check_status_share.get_tmp_num());
				tmps_full[i] = index;
				tmp_list.add(index);
			}
			
			if(this.type.equals("int")){
				BoolExpr value_cnst = cs.ctx.mkEq(cs.ctx.mkInt(0), ex);
				cs.add_constraint(cs.ctx.mkForall(tmps_full, cs.ctx.mkImplies(guard, value_cnst), 1, null, null, null, null));
			}else if(this.type.equals("boolean")){
				BoolExpr value_cnst = cs.ctx.mkEq(cs.ctx.mkBool(false), ex);
				cs.add_constraint(cs.ctx.mkForall(tmps_full, cs.ctx.mkImplies(guard, value_cnst), 1, null, null, null, null));
			}
			

			return ret;
		}else if(this.new_suffix.expression_list!=null){//コンストラクタ
			//String ident = 
			//Field f;
			//Expr ex
			new_suffix ps = this.new_suffix;
			//IntExpr f_index
			
			boolean pre_can_not_use_mutable = cs.can_not_use_mutable;
			if(cs.in_refinement_predicate) cs.can_not_use_mutable = true;
			boolean pre_in_refinement_predicate = cs.in_refinement_predicate;
			cs.in_refinement_predicate = false;
			
			
			class_declaration cd = cs.Check_status_share.compilation_unit.search_class(this.type);
			if(cd == null){
				throw new Exception("can't find class " + this.type);
			}
			method_decl md = cs.Check_status_share.compilation_unit.search_method(this.type, this.type);
			if(md == null){
				throw new Exception("can't find method " + this.type);
			}
			
			//引数の処理
			if(md.formals.param_declarations.size()!=ps.expression_list.expressions.size()){
				throw new Exception("wrong number of arguments");
			}
			
			
			//helperメソッドの中でhelperでないメソッドを呼んだ場合、全ての篩型を検証する
			if(cs.in_helper && !cs.in_helper){
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

			
			//返り値
			Variable result = new Variable(cs.Check_status_share.get_tmp_num(), "class_" + this.type + "_constructor_tmp", this.type, 0, null, md.modifiers, this.type, cs.ctx.mkBool(false));
			result.temp_num++;
			cs.result = result;
			
			//newで新しく作ったrefは被らないことを表すための制約
			ArrayExpr alloc = cs.ctx.mkArrayConst("alloc_array", cs.ctx.mkUninterpretedSort("Ref"), cs.ctx.mkIntSort());
			BoolExpr constraint = cs.ctx.mkEq(cs.ctx.mkSelect(alloc, result.get_Expr(cs)), cs.ctx.mkInt(cs.Check_status_share.get_tmp_num()));
			cs.add_constraint(constraint);
			
			Expr pre_instance_expr = cs.instance_expr;
			String pre_instance_class_name = cs.instance_class_name;
			
			cs.instance_expr = result.get_Expr(cs);
			cs.instance_class_name = this.type;
			
			
			for(int j = 0; j < md.formals.param_declarations.size(); j++){
				param_declaration pd = md.formals.param_declarations.get(j);
				modifiers m = new modifiers();
				m.is_final = pd.is_final;
				
				Variable v = new Variable(cs.Check_status_share.get_tmp_num(), pd.ident, pd.type_spec.type.type, pd.type_spec.dims, pd.type_spec.refinement_type_clause, m, result.type, cs.ctx.mkBool(true));
				cs.called_method_args.add(v);
				v.temp_num = 0;
				//引数に値を紐づける
				cs.add_constraint(cs.ctx.mkEq(v.get_Expr(cs), ps.expression_list.expressions.get(j).check(cs).expr));
				v.arg_field =  method_arg_valuse.get(j).field;
				if(method_arg_valuse.get(j).field!=null) v.internal_id = method_arg_valuse.get(j).field.internal_id;
				
				//配列の篩型が安全かどうか
				cs.check_array_alias(v, result.get_Expr(cs), new ArrayList<IntExpr>(), method_arg_valuse.get(j).field, method_arg_valuse.get(j).class_expr, method_arg_valuse.get(j).indexs);
				
				
				//篩型
				if(v.hava_refinement_type()){
					v.assert_refinement(cs, null);//class_Fieldとかは本来resultなどだが、まだできていないオブジェクトなのでnullでいいはず
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
			System.out.println("constructor call requires");
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
			if(md.modifiers.is_pure){
				//何もしない
			}else if(md.method_specification != null){
				
				Pair<List<F_Assign>, BoolExpr> assign_cnsts = md.method_specification.assignables(cs);
				
				//helperメソッド、コンストラクタでは、代入前に検証が必要な場合がある
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
				
				for(Variable variable : cs.called_method_args){//引数
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
								index_expr.add(cs.ctx.mkIntConst("tmpIdex" + cs.Check_status_share.tmp_num));
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
			
			
			//事後条件
			if(!md.modifiers.is_helper){
				BoolExpr post_invariant_expr = cs.all_invariant_expr();
				//生成したクラスの不変条件も追加する
				post_invariant_expr = cs.ctx.mkAnd(post_invariant_expr, result.all_invariants_expr(0, cs.invariant_refinement_type_deep_limmit, null, new ArrayList<IntExpr>(), cs));
				cs.add_constraint(post_invariant_expr);
			}
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
			
			
			return result;
		}else{
			throw new Exception("wrong new clause");
		}
	}
	
	public Check_return loop_assign(Pair<List<F_Assign>,BoolExpr>assigned_fields, Check_status cs) throws Exception{
		if(this.new_suffix.is_index){
			return null;
		}else if(this.new_suffix.expression_list!=null){//コンストラクタ
			Field f = loop_assign_method(assigned_fields, cs, this.new_suffix);
			Expr ex = f.get_Expr(cs);
			return new Check_return(ex, f, new ArrayList<IntExpr>(), null);
		}else{
			return null;
		}
	}
	
	public Field loop_assign_method(Pair<List<F_Assign>,BoolExpr>assigned_fields, Check_status cs, new_suffix ps)throws Exception{
		
		class_declaration cd = cs.Check_status_share.compilation_unit.search_class(this.type);
		if(cd == null){
			throw new Exception("can't find class " + this.type);
		}
		method_decl md = cs.Check_status_share.compilation_unit.search_method(this.type, this.type);
		if(md == null){
			throw new Exception("can't find method " + this.type);
		}
		
		
		
		
		//返り値
		Variable result = new Variable(cs.Check_status_share.get_tmp_num(), "class_" + this.type + "_constructor_tmp", this.type, 0, null, md.modifiers, this.type, cs.ctx.mkBool(false));
		result.temp_num++;
		
		//メソッドの事前条件、事後条件にそのメソッド自身を書いた場合、制約のないただの値として返される。
		if(cs.in_jml_predicate && cs.used_methods.contains(md))return result;
		
		cs.result = result;
		
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
		
		cs.instance_expr = result.get_Expr(cs);
		cs.instance_class_name = this.type;
		
		cs.used_methods.add(md);
		
		for(int j = 0; j < md.formals.param_declarations.size(); j++){
			param_declaration pd = md.formals.param_declarations.get(j);
			modifiers m = new modifiers();
			m.is_final = pd.is_final;
			Variable v = new Variable(cs.Check_status_share.get_tmp_num(), pd.ident, pd.type_spec.type.type, pd.type_spec.dims, pd.type_spec.refinement_type_clause, m, result.type, cs.ctx.mkBool(true));
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
		result.temp_num++;
		
		cs.in_method_call = false;
		
		cs.instance_expr = pre_instance_expr;
		cs.instance_class_name = pre_instance_class_name;
		
		cs.used_methods.remove(md);
		
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

