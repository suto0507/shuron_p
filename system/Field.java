package system;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.*;

import system.parsers.refinement_type_clause;
import system.parsers.variable_definition;
import system.parsers.class_declaration;
import system.parsers.invariant;
import system.parsers.modifiers;
import system.parsers.refinement_type;

public class Field {
	public int id;
	public int internal_id;//if文とかループとかでクローンしても変わらない。メソッド呼び出しでは、引数のこれは渡したフィールドのものと同じになる
	public int temp_num;
	public String field_name;
	public String type;
	public int dims;
	public refinement_type_clause refinement_type_clause;
	public modifiers modifiers;
	
	
	
	public boolean new_array;//newとかで生成された配列　
	
	//コンストラクタのfinalの初期化状態
	public boolean final_initialized;
	//コンストラクタで、そのクラスで定義されたフィールド
	public boolean constructor_decl_field;
	
	//宣言されたクラスの名前
	public String class_type_name;
	
	//helperメソッドやコンストラクターの中で、２次元以上の配列としてエイリアスした場合
	//これが関係あるのは、ローカル変数かコンストラクタの中のthisのフィールドだけなので、class_exprはいらない (thisのフィールドでのみ使う)
	public BoolExpr alias_1d_in_helper;
	public BoolExpr alias_in_consutructor_or_2d_in_helper;//コンストラクタでは、エイリアスした次元数に関係なく、エイリアス後は篩型を満たす必要がある。
	//ループの反復の前の検証でエイリアスしているかもしれないやつ
	public BoolExpr alias_1d_in_helper_pre_loop;
	public BoolExpr alias_in_consutructor_or_2d_in_helper_pre_loop;
	
	//データフィールド
	public ArrayList<Model_Field> model_fields;
	
	//新しくフィールドを作る時には、alias_1d_in_helperとalias_in_consutructor_or_2d_in_helperは同じ引数から初期化する
	public Field(int id, String field_name, String type, int dims, refinement_type_clause refinement_type_clause, modifiers modifiers, String class_type_name, BoolExpr alias_1d_in_helper, ArrayList<Model_Field> model_fields) throws Exception{
		this.id = id;
		this.internal_id = id;
		this.temp_num = 0;
		this.field_name = field_name;
		this.type = type;
		this.dims = dims;
		this.refinement_type_clause = refinement_type_clause;
		this.modifiers = modifiers;
		this.class_type_name = class_type_name;
		this.new_array = false;
		this.alias_1d_in_helper = alias_1d_in_helper;
		this.alias_in_consutructor_or_2d_in_helper = alias_1d_in_helper;
		this.model_fields = model_fields;
		this.final_initialized = true;
		this.constructor_decl_field = false;
	}
	
	public Field(){}
	
	
	public Field clone_e() throws Exception{
		Field ret = new Field(this.internal_id, this.field_name, this.type, this.dims, this.refinement_type_clause, this.modifiers, class_type_name, alias_1d_in_helper, model_fields);
		ret.temp_num = this.temp_num;
		
		ret.final_initialized = final_initialized;
		ret.constructor_decl_field = this.constructor_decl_field;
		ret.alias_in_consutructor_or_2d_in_helper = this.alias_in_consutructor_or_2d_in_helper;//新しくフィールドを作る時には、alias_in_helper_or_consutructorとalias_2d_in_helper_or_consutructorは同じ引数から初期化する
		
		ret.alias_1d_in_helper_pre_loop = alias_1d_in_helper_pre_loop;
		ret.alias_in_consutructor_or_2d_in_helper_pre_loop = alias_in_consutructor_or_2d_in_helper_pre_loop;
		return ret;
	}
	
	
	public Expr get_Expr(Check_status cs) throws Exception{
		if(this.type.equals("int")&&this.dims==0){
			String ret = field_name + "_temp_" + this.id + "_"  + this.temp_num;
			return cs.ctx.mkArrayConst(ret, cs.ctx.mkUninterpretedSort("Ref"), cs.ctx.mkIntSort());
		}else if(this.type.equals("boolean")&&this.dims==0){
			String ret = field_name + "_temp_" + this.id + "_"  + this.temp_num;
			return cs.ctx.mkArrayConst(ret, cs.ctx.mkUninterpretedSort("Ref"), cs.ctx.mkBoolSort());
		}else if(this.type.equals("void")){
			//throw new Exception("void variable ?");
			return null;
		}else if(this.dims==0){
			//クラス
			String ret = field_name + "_temp_" + this.id + "_"  + this.temp_num;
			return cs.ctx.mkArrayConst(ret, cs.ctx.mkUninterpretedSort("Ref"), cs.ctx.mkUninterpretedSort("Ref"));
		}else if(this.dims>0){
			String ret = field_name + "_temp_" + this.id + "_"  + this.temp_num;
			return cs.ctx.mkArrayConst(ret, cs.ctx.mkUninterpretedSort("Ref"), cs.ctx.mkUninterpretedSort("ArrayRef"));
		}
		throw new Exception("unexpect variable");
	}
	
	

	
	public Expr get_Expr_assign(Check_status cs) throws Exception{
		this.temp_num ++;
		Expr ex =  this.get_Expr(cs);
		this.temp_num --;
		return ex;
	}
	
	public Expr get_Expr_with_indexs(Expr class_expr, ArrayList<IntExpr> indexs, Check_status cs) throws Exception{
		Expr expr = null;
		if(this instanceof Variable){
			expr = this.get_Expr(cs);
		}else{
			expr = cs.ctx.mkSelect(this.get_Expr(cs), class_expr);
		}
		this.set_ref_info(expr, cs);
		for(int i = 0; i < indexs.size(); i++){
			Array array;
			if(i<this.dims-1){
			    array = cs.array_arrayref;
			}else{
			    if(this.type.equals("int")){
			        array = cs.array_int;
			    }else if(this.type.equals("boolean")){
			        array = cs.array_boolean;
			    }else{
			        array = cs.array_ref;
			    }
			}
			expr = array.index_access_array(expr, indexs.get(i), cs);
		}
		
		return expr;
	}
	
	//値を更新する
	//配列の要素への代入でないならば、tmp_numを更新する
	public void assign_value(Expr class_expr, ArrayList<IntExpr> indexs, Expr value, Check_status cs) throws Exception{
		if(indexs.size()>0){
			Expr expr = cs.ctx.mkSelect(this.get_Expr(cs), class_expr);
			for(int i = 0; i < indexs.size()-1; i++){
				expr = cs.array_arrayref.index_access_array(expr, indexs.get(i), cs);
			}
			Array array = null;
			if(indexs.size()<this.dims){
				array = cs.array_arrayref;
			}else{
				if(this.type.equals("int")){
					array = cs.array_int;
				}else if(this.type.equals("boolean")){
					array = cs.array_boolean;
				}else{
					array = cs.array_ref;
				}
			}
			array.update_array(expr, indexs.get(indexs.size()-1), value, cs);
			
			//配列への代入でも、modelフィールドは変わる
			for(Model_Field mf : this.model_fields){
				mf.tmp_plus_with_data_group(class_expr, cs);
			}
		}else{
			cs.add_constraint(cs.ctx.mkEq(this.get_Expr_assign(cs), cs.ctx.mkStore(this.get_Expr(cs), class_expr, value)));
			this.tmp_plus_with_data_group(class_expr, cs);
		}
	}
	
	
	//自分と同じ型のフレッシュなExprを返す
	public Expr get_fresh_value(Check_status cs) throws Exception{
		if(this.type.equals("int")&&this.dims==0){
			String ret = "freshInt" + cs.Check_status_share.get_tmp_num();
			return cs.ctx.mkIntConst(ret);
		}else if(this.type.equals("boolean")&&this.dims==0){
			String ret = "freshBool" + cs.Check_status_share.get_tmp_num();
			return cs.ctx.mkBoolConst(ret);
		}else if(this.type.equals("void")){
			//throw new Exception("void variable ?");
			return null;
		}else if(this.dims==0){
			//クラス
			String ret = "freshRef" + cs.Check_status_share.get_tmp_num();;
			Expr ref =  cs.ctx.mkConst(ret, cs.ctx.mkUninterpretedSort("Ref"));
			this.set_ref_info(ref, cs);
			return ref;
		}else if(this.dims>0){ //配列
			String ret = "freshArrayRef" + cs.Check_status_share.get_tmp_num();;
			Expr ref =  cs.ctx.mkConst(ret, cs.ctx.mkUninterpretedSort("ArrayRef"));
			this.set_ref_info(ref, cs);
			return ref;
		}
		throw new Exception("unexpect variable");
	}
	
	
	public boolean equals(Field f, Check_status cs){
		//if(this.field_name.equals(f.field_name) && ((this.class_object==null&&f.class_object==null) || this.class_object.equals(f.class_object, cs) ) ){
		if(this.internal_id == f.internal_id){
			return true;
		}
		
		return false;
	}
	
	public void tmp_plus(Check_status cs) throws Exception{
		if(this.modifiers!=null && this.modifiers.is_final && this.dims == 0){
			return;
		}
		this.temp_num++;
	}
	
	//tmp_numをインクリメントし、データグループの値も更新する　tmp_numのインクリメントした結果との制約は、データグループのモデルフィールドに対してだけ追加され、このフィールドに関しては制約は追加されない
	public void tmp_plus_with_data_group(Check_status cs) throws Exception{
		if(this.modifiers!=null && this.modifiers.is_final && this.dims == 0){
			return;
		}
		this.temp_num++;
		for(Model_Field mf : this.model_fields){
			mf.tmp_plus_with_data_group(cs);
		}
	}
	
	public void tmp_plus_with_data_group(Expr class_expr, Check_status cs) throws Exception{
		if(this.modifiers!=null && this.modifiers.is_final && this.dims == 0){
			return;
		}
		this.temp_num++;
		for(Model_Field mf : this.model_fields){
			mf.tmp_plus_with_data_group(class_expr, cs);
		}
	}
	
	public void tmp_plus_with_data_group(BoolExpr condition, Check_status cs) throws Exception{//conditionは値を変更する条件
		if(this.modifiers!=null && this.modifiers.is_final && this.dims == 0){
			return;
		}
		this.temp_num++;
		for(Model_Field mf : this.model_fields){
			mf.tmp_plus_with_data_group(condition, cs);
		}
	}
	
	public void tmp_plus_with_data_group(Expr class_expr, BoolExpr condition, Check_status cs) throws Exception{//conditionは値を変更する条件
		if(this.modifiers!=null && this.modifiers.is_final && this.dims == 0){
			return;
		}
		this.temp_num++;
		for(Model_Field mf : this.model_fields){
			mf.tmp_plus_with_data_group(class_expr, condition, cs);
		}
	}
	
	
	public void add_refinement_constraint(Check_status cs, Expr class_Expr) throws Exception{
		add_refinement_constraint(cs, class_Expr, false);
	}
	
	//class_Fieldは篩型を持つフィールド、変数を持つクラス
	//add_onceは、一度だけhelperやin_cnstructorの制約を無視して篩型の述語をaddする//つまり、篩型の述語の中に記述されたフィールドの篩型は無視したい時に使う
	public void add_refinement_constraint(Check_status cs, Expr class_Expr, boolean add_once) throws Exception{
		boolean root_check = false;
		if(cs.checked_refinement_type_field.size()==0){
			root_check = true;
		}else{
			for(Pair<Field, Expr> checked_field : cs.checked_refinement_type_field){
				if(checked_field.fst == this && checked_field.snd.equals(class_Expr)){
					return;
				}
			}
		}
		cs.checked_refinement_type_field.add(new Pair(this, class_Expr));
		
		if(this.refinement_type_clause!=null){
			System.out.println(this.field_name + " has refinement type");
			Expr ex = null;
			if(this instanceof Variable){
				ex = get_Expr(cs);
			}else{
				ex = cs.ctx.mkSelect(get_Expr(cs), class_Expr);
			}
			
			if(this.refinement_type_clause.refinement_type!=null){
				this.refinement_type_clause.refinement_type.add_refinement_constraint(cs, this, ex, class_Expr, add_once);
			}else if(this.refinement_type_clause.ident!=null){
				refinement_type rt = cs.search_refinement_type(this.class_type_name, this.refinement_type_clause.ident);
				if(rt!=null){
					rt.add_refinement_constraint(cs, this, ex, class_Expr, add_once);
				}else{
	                throw new Exception("cannot find refinement type " + this.refinement_type_clause.ident);
	            }
			}
		}
		
		for(Model_Field mf : this.model_fields){
			mf.add_refinement_constraint(cs, class_Expr, add_once);
		}
		
		if(root_check){
			cs.checked_refinement_type_field = new ArrayList<Pair<Field, Expr>>();
		}
		
	}
	
	public void assert_refinement(Check_status cs, Expr class_Expr) throws Exception{
		
		boolean root_check = false;
		if(cs.checked_refinement_type_field.size()==0){
			root_check = true;
		}else{
			for(Pair<Field, Expr> checked_field : cs.checked_refinement_type_field){
				if(checked_field.fst == this && checked_field.snd.equals(class_Expr)){
					return;
				}
			}
		}
		cs.checked_refinement_type_field.add(new Pair(this, class_Expr));
		
		if(this.refinement_type_clause!=null){
			System.out.println("assert refinement type of " + this.field_name);
			Expr ex = null;
			if(this instanceof Variable){
				ex = get_Expr(cs);
			}else{
				ex = cs.ctx.mkSelect(get_Expr(cs), class_Expr);
			}
			
			if(this.refinement_type_clause.refinement_type!=null){
				this.refinement_type_clause.refinement_type.assert_refinement(cs, this, ex, class_Expr);
			}else if(this.refinement_type_clause.ident!=null){
				refinement_type rt = cs.search_refinement_type(this.class_type_name, this.refinement_type_clause.ident);
				if(rt!=null){
					rt.assert_refinement(cs, this, ex, class_Expr);
				}else{
	                throw new Exception("cannot find refinement type " + this.refinement_type_clause.ident);
	            }
			}
		}
		
		for(Model_Field mf : this.model_fields){
			mf.assert_refinement(cs, class_Expr);
		}
		if(root_check){
			cs.checked_refinement_type_field = new ArrayList<Pair<Field, Expr>>();
		}
		
	}
	
	public boolean hava_refinement_type(){
		if(this.refinement_type_clause!=null){
			return true;
		}
		for(Model_Field mf : this.model_fields){
			if(mf.hava_refinement_type()) return true;
		}
		return false;
	}
	
	//篩型が配列の要素に言及しているかどうか
	public boolean have_index_access(Check_status cs) throws Exception{
		boolean have = false;
		
		if(this.refinement_type_clause!=null){
			if(this.refinement_type_clause.refinement_type!=null){
				have = have || this.refinement_type_clause.refinement_type.have_index_access(cs);
			}else if(this.refinement_type_clause.ident!=null){
				refinement_type rt = cs.search_refinement_type(this.class_type_name, this.refinement_type_clause.ident);
				if(rt!=null){
					have = have || rt.have_index_access(cs);
				}else{
	                throw new Exception("cannot find refinement type " + this.refinement_type_clause.ident);
	            }
			}
			for(Model_Field mf : this.model_fields){
				have = have || mf.have_index_access(cs);
			}
		}
		return have;
	}
	
	//finalのフィールドにinitializerが付いていた場合
	public void set_initialize(Expr class_expr, Check_status cs) throws Exception{
		variable_definition vd = cs.Check_status_share.compilation_unit.search_field(class_type_name, this.field_name, false, cs.this_field.type);
		//initializerが付いていた場合
		if(vd.variable_decls.initializer!=null && vd.modifiers.is_final){
			Check_return init_Expr = vd.variable_decls.initializer.check(cs);
			Expr field_Expr = cs.ctx.mkSelect(this.get_Expr(cs), class_expr);
			cs.add_constraint(cs.ctx.mkEq(field_Expr, init_Expr.expr));
		}
	}
	
	//このフィールドのフィールドに関しても制約を返す
	public BoolExpr all_invariants_expr(int deep, int deep_limmit, Expr class_expr, ArrayList<IntExpr> indexs, Check_status cs) throws Exception{
		
		if(deep >= deep_limmit) return cs.ctx.mkBool(true);
		
		ArrayList<IntExpr> fresh_indexs = (ArrayList<IntExpr>) indexs.clone();

		BoolExpr ret_expr =  cs.ctx.mkBool(true);
		
		class_declaration cd = cs.Check_status_share.compilation_unit.search_class(type);
		if(cd==null) throw new Exception("cannot find class " + type);
		
		Expr expr = this.get_Expr(cs);
		if(!(this instanceof Variable))expr = cs.ctx.mkSelect(this.get_Expr(cs), class_expr);
		
		BoolExpr bound_guard = null;
		for(int i = 0; i < this.dims; i++){
			String ret = "tmpIndex" + cs.Check_status_share.get_tmp_num();
			IntExpr fresh_index = cs.ctx.mkIntConst(ret);
			
			IntExpr length = (IntExpr) cs.ctx.mkSelect(cs.ctx.mkArrayConst("length", cs.ctx.mkUninterpretedSort("ArrayRef"), cs.ctx.mkIntSort()), expr);
			BoolExpr index_bound = cs.ctx.mkGe(fresh_index, cs.ctx.mkInt(0));
			index_bound = cs.ctx.mkAnd(index_bound, cs.ctx.mkGt(length, fresh_index));
			if(bound_guard == null){
				bound_guard = index_bound;
			}else{
				bound_guard = cs.ctx.mkAnd(bound_guard, index_bound);
			}
			
			Array array;
			if(i<this.dims-1){
			    array = cs.array_arrayref;
			}else{
			    if(this.type.equals("int")){
			        array = cs.array_int;
			    }else if(this.type.equals("boolean")){
			        array = cs.array_boolean;
			    }else{
			        array = cs.array_ref;
			    }
			}
			
			expr = array.index_access_array(expr, fresh_index, cs);
			fresh_indexs.add(fresh_index);
		}
	
		if(cd.class_block.invariants.size()!=0){
			Expr pre_instance_expr = cs.instance_expr;
			String pre_instance_class_name = cs.instance_class_name;
			
			BoolExpr pre_pathcondition = cs.pathcondition;
			
			cs.instance_expr = expr;
			cs.instance_class_name = this.type;
			
			for(invariant invariant : cd.class_block.invariants){
				if(invariant.is_private==true){//可視性が同じものしか使えない
					cs.ban_default_visibility = true;
				}else{
					cs.ban_private_visibility = true;
				}
				
				Expr invariant_expr = invariant.check(cs);
				
				cs.ban_default_visibility = false;
				cs.ban_private_visibility = false;
				
				

				cs.add_path_condition_tmp((BoolExpr) invariant_expr);
				ret_expr = cs.ctx.mkAnd(ret_expr, invariant_expr);
			}
			cs.instance_expr = pre_instance_expr;
			cs.instance_class_name = pre_instance_class_name;

			cs.pathcondition = pre_pathcondition;
			
			if(fresh_indexs.size() > 0){
				IntExpr[] tmps = new IntExpr[fresh_indexs.size()];
				for(int i = 0; i < fresh_indexs.size(); i++){
					tmps[i] = fresh_indexs.get(i);
				}
				ret_expr = cs.ctx.mkForall(tmps, cs.ctx.mkImplies(bound_guard, ret_expr), 1, null, null, null, null);
			}
		}
		
		for(Field f : cd.all_field(cs)){
			if(!(f.type.equals("int") || f.type.equals("boolean"))){
				ret_expr = cs.ctx.mkAnd(ret_expr, f.all_invariants_expr(deep+1, deep_limmit, expr, fresh_indexs, cs));
			}
		}
		
		return ret_expr;
	}
	
	//このフィールドのフィールドに関しても制約を返す
	//assertなので、forallを使う必要はなさそう
	public void assert_all_refinement_type(int deep, int deep_limmit, Expr class_expr, ArrayList<IntExpr> indexs, Check_status cs) throws Exception{		
		if(deep >= deep_limmit) return;
		
		ArrayList<IntExpr> fresh_indexs = (ArrayList<IntExpr>) indexs.clone();

		Expr expr = this.get_Expr(cs);
		if(!(this instanceof Variable))expr = cs.ctx.mkSelect(this.get_Expr(cs), class_expr);
		
		//篩型の検証
		this.assert_refinement(cs, class_expr);
		
		class_declaration cd = cs.Check_status_share.compilation_unit.search_class(type);
		if(cd==null) return;//intやboolフィールド
		
		for(int i = 0; i < this.dims; i++){
			String ret = "tmpIndex" + cs.Check_status_share.get_tmp_num();
			IntExpr fresh_index = cs.ctx.mkIntConst(ret);
			
			IntExpr length = (IntExpr) cs.ctx.mkSelect(cs.ctx.mkArrayConst("length", cs.ctx.mkUninterpretedSort("ArrayRef"), cs.ctx.mkIntSort()), expr);
			BoolExpr index_bound = cs.ctx.mkGe(fresh_index, cs.ctx.mkInt(0));
			index_bound = cs.ctx.mkAnd(index_bound, cs.ctx.mkGt(length, fresh_index));
			cs.add_constraint(index_bound);
			
			Array array;
			if(i<this.dims-1){
			    array = cs.array_arrayref;
			}else{
			    if(this.type.equals("int")){
			        array = cs.array_int;
			    }else if(this.type.equals("boolean")){
			        array = cs.array_boolean;
			    }else{
			        array = cs.array_ref;
			    }
			}
			expr = array.index_access_array(expr, fresh_index, cs);
			fresh_indexs.add(fresh_index);
		}
	
		for(Field f : cd.all_field(cs)){
			f.assert_all_refinement_type(deep+1, deep_limmit, expr, fresh_indexs, cs);
		}
	}
	
	//helperメソッド内で、配列の代入前に篩型の検証を行なわなければならないときに使う
	//このフィールドのフィールドに関しても制約を返す
	//assertなので、forallを使う必要はなさそう
	public void assert_all_array_assign_in_helper(int deep, int deep_limmit, Expr class_expr, BoolExpr condition, ArrayList<IntExpr> indexs, Check_status cs) throws Exception{		
		if(deep >= deep_limmit) return;
		
		if(!(this.dims >= 1 && this.hava_refinement_type() && this.have_index_access(cs) && (cs.in_helper || cs.in_no_refinement_type)))return;
		
		ArrayList<IntExpr> fresh_indexs = (ArrayList<IntExpr>) indexs.clone();

		Expr expr = this.get_Expr(cs);
		if(!(this instanceof Variable))expr = cs.ctx.mkSelect(this.get_Expr(cs), class_expr);
		
		//篩型の検証
		cs.solver.push();
		
		//エイリアスしているときだけでいい
		if(this instanceof Variable
				|| (cs.in_constructor && !(this instanceof Variable) && cs.this_field.get_Expr(cs).equals(class_expr) && this.constructor_decl_field)){
			if(this.alias_1d_in_helper_pre_loop!=null){
				cs.add_constraint(cs.ctx.mkOr(this.alias_1d_in_helper_pre_loop, this.alias_1d_in_helper));
			}else{
				cs.add_constraint(this.alias_1d_in_helper);
			}
			
		}
		
		
		cs.add_constraint(condition);
		Field v = this;
		Field old_v = cs.this_old_status.search_internal_id(v.internal_id);


		Expr old_assign_field_expr = null;
		Expr assign_field_expr = null;
		if(v instanceof Variable){
			assign_field_expr = v.get_Expr(cs);
		}else{
			old_assign_field_expr = cs.ctx.mkSelect(old_v.get_Expr(cs.this_old_status), class_expr);
			assign_field_expr = cs.ctx.mkSelect(v.get_Expr(cs), class_expr);
		}
		
		//メソッドの最初では篩型が満たしていることを仮定していい
		//フィールドだけ
		if((cs.in_helper || cs.in_no_refinement_type) && !(v instanceof Variable)){
			old_v.add_refinement_constraint(cs.this_old_status, class_expr, true);
		}
		
		v.assert_refinement(cs, class_expr);
		
		cs.solver.pop();
		
		//次のフィールド
		class_declaration cd = cs.Check_status_share.compilation_unit.search_class(type);
		if(cd==null) return;//intやboolフィールド
		
		for(int i = 0; i < this.dims; i++){
			String ret = "tmpIndex" + cs.Check_status_share.get_tmp_num();
			IntExpr fresh_index = cs.ctx.mkIntConst(ret);
			
			IntExpr length = (IntExpr) cs.ctx.mkSelect(cs.ctx.mkArrayConst("length", cs.ctx.mkUninterpretedSort("ArrayRef"), cs.ctx.mkIntSort()), expr);
			BoolExpr index_bound = cs.ctx.mkGe(fresh_index, cs.ctx.mkInt(0));
			index_bound = cs.ctx.mkAnd(index_bound, cs.ctx.mkGt(length, fresh_index));
			cs.add_constraint(index_bound);
			
			Array array;
			if(i<this.dims-1){
			    array = cs.array_arrayref;
			}else{
			    if(this.type.equals("int")){
			        array = cs.array_int;
			    }else if(this.type.equals("boolean")){
			        array = cs.array_boolean;
			    }else{
			        array = cs.array_ref;
			    }
			}
			expr = array.index_access_array(expr, fresh_index, cs);
			fresh_indexs.add(fresh_index);
		}
	
		for(Field f : cd.all_field(cs)){
			f.assert_all_refinement_type(deep+1, deep_limmit, expr, fresh_indexs, cs);
		}
	}
	
	//new などで新しく作ったrefと被らないことを表すための制約
	public void ref_constraint(int deep, int deep_limmit, Expr class_expr, ArrayList<IntExpr> indexs, Check_status cs) throws Exception{
		if(deep >= deep_limmit) return;
		
		if(!(this.dims >= 1 && this.hava_refinement_type() && this.have_index_access(cs) && (cs.in_helper || cs.in_no_refinement_type)))return;
		
		ArrayList<IntExpr> fresh_indexs = (ArrayList<IntExpr>) indexs.clone();

		Expr expr = this.get_Expr(cs);
		if(!(this instanceof Variable))expr = cs.ctx.mkSelect(this.get_Expr(cs), class_expr);
		
		//クラス型の制約
		ArrayExpr alloc = null;
		if(this.dims == 0 && !(this.type.equals("int") || this.type.equals("boolean"))){
			alloc = cs.ctx.mkArrayConst("alloc", cs.ctx.mkUninterpretedSort("Ref"), cs.ctx.mkIntSort());
		}else if(this.dims > 0){
			alloc = cs.ctx.mkArrayConst("alloc_array", cs.ctx.mkUninterpretedSort("ArrayRef"), cs.ctx.mkIntSort());
		}
		if(alloc != null){
			BoolExpr constraint = cs.ctx.mkLt(cs.ctx.mkSelect(alloc, expr), cs.ctx.mkInt(0));
			if(fresh_indexs.size() > 0){
				IntExpr[] tmps = new IntExpr[fresh_indexs.size()];
				for(int i = 0; i < fresh_indexs.size(); i++){
					tmps[i] = fresh_indexs.get(i);
				}
				constraint = cs.ctx.mkForall(tmps, constraint, 1, null, null, null, null);
			}
			cs.add_constraint(constraint);
		}
		
		
		BoolExpr bound_guard = null;
		for(int i = 0; i < this.dims; i++){
			String ret = "tmpIndex" + cs.Check_status_share.get_tmp_num();
			IntExpr fresh_index = cs.ctx.mkIntConst(ret);
			
			IntExpr length = (IntExpr) cs.ctx.mkSelect(cs.ctx.mkArrayConst("length", cs.ctx.mkUninterpretedSort("ArrayRef"), cs.ctx.mkIntSort()), expr);
			BoolExpr index_bound = cs.ctx.mkGe(fresh_index, cs.ctx.mkInt(0));
			index_bound = cs.ctx.mkAnd(index_bound, cs.ctx.mkGt(length, fresh_index));
			if(bound_guard == null){
				bound_guard = index_bound;
			}else{
				bound_guard = cs.ctx.mkAnd(bound_guard, index_bound);
			}
			
			alloc = null;
			Array array;
			if(i<this.dims-1){
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
			expr = array.index_access_array(expr, fresh_index, cs);
			fresh_indexs.add(fresh_index);
			
			if(alloc != null){
				BoolExpr constraint = cs.ctx.mkLt(cs.ctx.mkSelect(alloc, expr), cs.ctx.mkInt(0));
				
				IntExpr[] tmps = new IntExpr[fresh_indexs.size()];
				for(int j = 0; j < fresh_indexs.size(); j++){
					tmps[j] = fresh_indexs.get(i);
				}
				constraint = cs.ctx.mkForall(tmps, cs.ctx.mkImplies(bound_guard, constraint), 1, null, null, null, null);
			
				cs.add_constraint(constraint);
			}
		}
		
		//次のフィールド
		class_declaration cd = cs.Check_status_share.compilation_unit.search_class(type);
		if(cd==null) return;//intやboolフィールド
	
		for(Field f : cd.all_field(cs)){
			f.assert_all_refinement_type(deep+1, deep_limmit, expr, fresh_indexs, cs);
		}
	}
	
	//RefやArrayRefに型の情報を与える
	public void set_ref_info(Expr ref, Check_status cs) throws Exception{
		if(this.dims > 0){
			ArrayExpr dim_expr = cs.ctx.mkArrayConst("dim_array", cs.ctx.mkUninterpretedSort("ArrayRef"), cs.ctx.mkIntSort());
			cs.add_constraint(cs.ctx.mkEq(cs.ctx.mkSelect(dim_expr, ref), cs.ctx.mkInt(this.dims)));
			ArrayExpr type_expr = cs.ctx.mkArrayConst("type_array", cs.ctx.mkUninterpretedSort("ArrayRef"), cs.ctx.mkStringSort());
			cs.add_constraint(cs.ctx.mkEq(cs.ctx.mkSelect(type_expr, ref), cs.ctx.mkString(this.type)));
		}else{
			if(!(this.type.equals("int") || this.type.equals("boolean"))){
				ArrayExpr type_expr = cs.ctx.mkArrayConst("type_ref", cs.ctx.mkUninterpretedSort("Ref"), cs.ctx.mkStringSort());
				cs.add_constraint(cs.ctx.mkEq(cs.ctx.mkSelect(type_expr, ref), cs.ctx.mkString(this.type)));
			}
		}
	}

}
