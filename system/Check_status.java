package system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.microsoft.z3.*;

import system.parsers.*;

public class Check_status {
	public Check_status_share Check_status_share;
	
	public method_decl md;
	
	public BoolExpr pathcondition;
	public ArrayList<BoolExpr> return_conditions;//メソッド内で共有したいので、そのままcloneをする。メソッドの最初で初期化して上げる必要がある
	public List<Variable> variables;
	//List<Variable> before_if_variables;
	public List<Field> fields;
	public List<Model_Field> model_fields;
	public Context ctx;
	public Solver solver;
	public Field this_field;
	public List<Pair<String, refinement_type>> local_refinements;
	
	
	public Expr instance_expr;
	public String instance_class_name;
	//public Field instance_Field;
	//public ArrayList<IntExpr> instance_indexs;
	
	public boolean in_refinement_predicate;//篩型の述語のの中でメソッドを呼び出した場合、その検証中はこのフラグはfalseになる
	public Field refined_Field;
	public Expr refined_Expr;
	public String refinement_type_value;
	
	//メソッド呼び出し
	public boolean in_method_call;
	public List<Variable> called_method_args;
	public Check_status old_status;
	public Variable result;
	
	//事後条件用
	public Variable return_v;
	public Expr return_expr;//事後条件用
	public boolean after_return;
	public Check_status this_old_status; // this_old_statusのthis_old_statusにはこのインスタンスが入る
	
	//assignable
	public BoolExpr assinable_cnst_all;//フィールドに代入できる条件
	
	//JML節の中の可視性の確認
	public boolean ban_private_visibility;
	public boolean ban_default_visibility;
	
	public Right_side_status right_side_status;
	
	public int refinement_deep, refinement_deep_limmit;
	
	public boolean in_constructor;
	
	public boolean can_not_use_mutable;//篩型の中のメソッド呼び出し
	
	public List<Pair<String, Expr>> quantifiers;
	
	public boolean in_jml_predicate;
	public boolean in_postconditions;
	public boolean use_only_helper_method;
	
	
	public boolean in_helper; //helperメソッドの中かどうか
	public boolean in_no_refinement_type; //no_refinement_typeメソッドの中かどうか
	public ArrayList<Helper_assigned_field> helper_assigned_fields;
	
	public int invariant_refinement_type_deep_limmit;//invarintどこまで検証するか　また、helperメソッドで普通のメソッドを呼び出す際にどこまで篩型を検証するか　0の時は検証しない
	
	public ArrayList<Pair<Field, Expr>> checked_refinement_type_field;//篩型の制約を既に追加したもの　Fieldとclass_objectのExprのPair    使い切りなのでcloneでは中身は気にしなくていい
	
	//配列のポインターから配列
	public Array array_arrayref;
	public Array array_int;
	public Array array_boolean;
	public Array array_ref;
	
	public BoolExpr this_alias; //コンストラクタの検証において、thisを渡してしまった条件
	
	public ArrayList<method_decl> used_methods;//メソッドの事前条件、事後条件にそのメソッド自身を書いた場合、制約のないただの値として返される。
	
	
	public Check_status(compilation_unit cu){
		this.ctx = new Context(new HashMap<>());
		this.solver = ctx.mkSolver();
		variables = new ArrayList<Variable>();
		this.Check_status_share = new Check_status_share(cu);
		this.local_refinements = new ArrayList<Pair<String, refinement_type>>();
		this.fields = new ArrayList<Field>();
		this.model_fields = new ArrayList<Model_Field>();
		//this.assignables = new ArrayList<Field>();
		this.right_side_status = new Right_side_status();
		quantifiers = new ArrayList<Pair<String, Expr>>();
		helper_assigned_fields = new ArrayList<Helper_assigned_field>();
		checked_refinement_type_field = new ArrayList<Pair<Field, Expr>>();
		
		array_arrayref = new Array(this.ctx.mkUninterpretedSort("ArrayRef"), this);
		array_int = new Array(this.ctx.mkIntSort(), this);
		array_boolean = new Array(this.ctx.mkBoolSort(), this);
		array_ref = new Array(this.ctx.mkUninterpretedSort("Ref"), this);
		
		this_alias = this.ctx.mkBool(false);
		
		used_methods = new ArrayList<method_decl>();
	}
	
	Check_status(){
		
	}
	
	public BoolExpr get_pathcondition(){
		if(this.pathcondition!=null) return this.pathcondition;
		return this.ctx.mkBool(true);
	}
	
	public boolean search_variable(String ident){
		boolean ret = false;
		for(Variable v :variables){
			if(ident.equals(v.field_name)){
				ret = true;
			}
		}
		return ret;
	}
	
	public Quantifier_Variable search_quantifier(String ident, Check_status cs){
		for(Pair<String, Expr> quantifier : quantifiers){
			if(quantifier.fst.equals(ident)){
				return new Quantifier_Variable(ident, quantifier.snd);
			}
		}
		return null;
	}
	
	//identはxとかで検索
	public Field search_field(String ident, String class_type_name, Check_status cs) throws Exception{
		
		variable_definition vd = this.Check_status_share.compilation_unit.search_field(class_type_name, ident, false, this_field.type);
		
		if(vd == null){
			return null;
		}
		
		String field_name = ident + "_" + vd.class_type_name;
		
		for(Field v :fields){
			if(field_name.equals(v.field_name + "_" + v.class_type_name)&&v.class_type_name.equals(class_type_name)){
				return v;
			}
		}
		
		//データグループのリストを作る
		ArrayList<Model_Field> data_groups = new ArrayList<Model_Field>();
		for(group_name gn : vd.group_names){
			String class_type = null;
			if(gn.is_super){
				class_declaration cd = this.Check_status_share.compilation_unit.search_class(class_type_name);
				class_type = cd.super_class.class_name;
			}else{
				class_type = class_type_name;
			}
			
			
			data_groups.add(search_model_field(gn.ident, class_type, this));
		}
		
		Field f = new Field(this.Check_status_share.get_tmp_num(), ident, vd.variable_decls.type_spec.type.type, vd.variable_decls.type_spec.dims, vd.variable_decls.type_spec.refinement_type_clause, vd.modifiers, vd.class_type_name, cs.ctx.mkBool(true), data_groups);
		
		
		
		this.fields.add(f);
		
		//初期値をold用のcsにも追加しておく
		if(cs.this_old_status!=null){
			Field f_old = f.clone_e();
			cs.this_old_status.fields.add(f_old);
		}
		
		
		
		return f;
	}
	
	//identはxとかで検索
	public Model_Field search_model_field(String ident, String class_type_name, Check_status cs) throws Exception{
		
		variable_definition vd = this.Check_status_share.compilation_unit.search_field(class_type_name, ident, true, this_field.type);
		
		if(vd == null){
			return null;
		}
		
		String field_name = ident + "_" + vd.class_type_name;
		for(Model_Field model_field :model_fields){
			if(field_name.equals(model_field.field_name + "_" + model_field.class_type_name) && model_field.class_type_name.equals(class_type_name)){
				return model_field;
			}
		}
		
		//データグループのリストを作る
		ArrayList<Model_Field> data_groups = new ArrayList<Model_Field>();
		for(group_name gn : vd.group_names){
			String class_type = null;
			if(gn.is_super){
				class_declaration cd = this.Check_status_share.compilation_unit.search_class(class_type);
				class_type = cd.super_class.class_name;
			}else{
				class_type = class_type_name;
			}
			
			data_groups.add(search_model_field(gn.ident, class_type, this));
		}
		
		Model_Field mf = new Model_Field(this.Check_status_share.get_tmp_num(), ident, vd.variable_decls.type_spec.type.type, vd.variable_decls.type_spec.dims, vd.variable_decls.type_spec.refinement_type_clause, vd.modifiers, vd.class_type_name, cs.ctx.mkBool(true), data_groups);
		mf.set_repersents(cs);

		
		this.model_fields.add(mf);
		//初期値をold用のcsにも追加しておく
		if(cs.this_old_status!=null){
			Model_Field mf_old = mf.clone_e();
			cs.this_old_status.model_fields.add(mf_old);
		}
		
		
		
		return mf;
	}
	
	public Field search_internal_id(int internal_id){
		if(this.this_field.internal_id == internal_id){
			return this.this_field;
		}
		for(Field f : this.fields){
			if(f.internal_id == internal_id){
				return f;
			}
		}
		
		for(Field v : this.variables){
			if(v.internal_id == internal_id){
				return v;
			}
		}
		
		for(Field mf : this.model_fields){
			if(mf.internal_id == internal_id){
				return mf;
			}
		}
		
		return null;
	}
	
	
	public boolean search_called_method_arg(String ident){
		boolean ret = false;
		for(Variable v : called_method_args){
			if(ident.equals(v.field_name)){
				ret = true;
			}
		}
		return ret;
	}
	
	public Variable get_variable(String ident) throws Exception{
		for(Variable v :variables){
			if(ident.equals(v.field_name)){
				return v;
			}
		}
		throw new Exception("can't find " + ident + "\n");
		//return null;
	}
	
	public Variable get_called_method_arg(String ident) throws Exception{
		for(Variable v :called_method_args){
			if(ident.equals(v.field_name)){
				return v;
			}
		}
		throw new Exception("can't find " + ident + "\n");
		//return null;
	}

	
	public String new_temp(){
		return this.Check_status_share.new_temp();
	}
	
	public void add_constraint(BoolExpr arg_expr) throws Exception{
		System.out.println("add" + arg_expr);
		this.solver.add(arg_expr);
	}
	
	public void assert_constraint(BoolExpr arg_expr) throws Exception{
		BoolExpr expr;
		if(this.pathcondition!=null){
			expr = this.ctx.mkAnd(this.pathcondition, this.ctx.mkNot(arg_expr));
		}else{
			expr = this.ctx.mkNot(arg_expr);
		}
		if(this.return_conditions!=null && this.return_conditions.size()>0){
			for(BoolExpr return_condition : this.return_conditions){
				expr = this.ctx.mkAnd(expr, this.ctx.mkNot(return_condition));
			}
		}
		System.out.println("assert " + expr);
		
		

		this.solver.push();
		this.solver.add(expr);
        if(solver.check() == Status.SATISFIABLE) {
            Model model = solver.getModel();
            System.out.println("this assert have counter example");
            System.out.println(model.toString());
            throw new Exception("!invalid assert!");

        }else{
        	System.out.println("this assert is correct");
        }

		this.solver.pop();
	}
	
	public Variable add_variable(String variable, String type, int dims, refinement_type_clause refinement_type_clause, modifiers modifiers, BoolExpr alias_2d) throws Exception{
		Variable v = new Variable(this.Check_status_share.get_tmp_num(), variable, type, dims, refinement_type_clause, modifiers, this.this_field.type, alias_2d);
		this.variables.add(v);
		return v;
	}
	

	
	public Check_status clone(){
		Check_status cs = new Check_status();
		cs.ctx = this.ctx;
		cs.solver = this.solver;
		cs.md = this.md;
		cs.Check_status_share = this.Check_status_share;
		cs.pathcondition = this.pathcondition;
		cs.return_conditions = this.return_conditions;
		cs.variables = new ArrayList<Variable>();
		for(Variable v : this.variables){
			cs.variables.add(v);
		}
		//cs.before_if_variables = this.before_if_variables;
		cs.local_refinements = new ArrayList<Pair<String,refinement_type>>();
		for(Pair<String,refinement_type> rt : this.local_refinements){
			cs.local_refinements.add(rt);
		}
		
		cs.fields = new ArrayList<Field>();
		for(Field f : this.fields){
			cs.fields.add(f);
		}
		
		cs.model_fields = new ArrayList<Model_Field>();
		for(Model_Field mf : this.model_fields){
			cs.model_fields.add(mf);
		}
		
		cs.this_field = this.this_field;
		
		cs.in_method_call = this.in_method_call;
		cs.called_method_args = this.called_method_args;
		cs.old_status = this.old_status;
		cs.result = this.result;
		
		cs.return_v = this.return_v;
		cs.return_expr = this.return_expr;
		cs.after_return = this.after_return;
		
		cs.this_old_status = this.this_old_status;
				
		cs.assinable_cnst_all = this.assinable_cnst_all;
		
		cs.right_side_status = this.right_side_status;
		
		cs.refinement_deep = this.refinement_deep;
		cs.refinement_deep_limmit = this.refinement_deep_limmit;
		
		cs.instance_expr = this.instance_expr;
		cs.instance_class_name = this.instance_class_name;
		//cs.instance_Field = this.instance_Field;
		
		cs.in_constructor = this.in_constructor;
		
		cs.can_not_use_mutable = this.can_not_use_mutable;
		
		cs.quantifiers = new ArrayList<Pair<String, Expr>>();
		
		cs.in_jml_predicate = this.in_jml_predicate;
		cs.use_only_helper_method = this.use_only_helper_method;
		
		cs.in_helper = this.in_helper;
		cs.in_no_refinement_type = this.in_no_refinement_type;
			
		cs.helper_assigned_fields = new ArrayList<Helper_assigned_field>();
		for(Helper_assigned_field assigned_field : this.helper_assigned_fields){
			cs.helper_assigned_fields.add(assigned_field);
		}
		cs.invariant_refinement_type_deep_limmit = this.invariant_refinement_type_deep_limmit;
		
		cs.checked_refinement_type_field = new ArrayList<Pair<Field, Expr>>();
		
		cs.array_arrayref = this.array_arrayref.clone(this);
		cs.array_int = this.array_int.clone(this);
		cs.array_boolean = this.array_boolean.clone(this);
		cs.array_ref = this.array_ref.clone(this);
		
		cs.this_alias = this.this_alias;
		
		cs.used_methods = this.used_methods;
		
		return cs;
	}
	
	
	// \oldとかで使う、変数等の保存用
	public void clone_list() throws Exception{
		if(this.variables!=null){
			List<Variable> v_tmp = this.variables;
			this.variables = new ArrayList<Variable>();
			for(Variable v : v_tmp){
				Variable new_v = v.clone_e();
				this.variables.add(new_v);
			}
		}

		if(this.fields!=null){
			List<Field> f_tmp = this.fields;
			this.fields = new ArrayList<Field>();
			for(Field f : f_tmp){
				Field new_f = f.clone_e();
				this.fields.add(new_f);
			}
		}
		
		if(this.model_fields!=null){
			List<Model_Field> mf_tmp = this.model_fields;
			this.model_fields = new ArrayList<Model_Field>();
			for(Model_Field mf : mf_tmp){
				Model_Field new_mf = mf.clone_e();
				this.model_fields.add(new_mf);
			}
		}
		
		if(this.called_method_args!=null){
			List<Variable> v_tmp = this.called_method_args;
			this.called_method_args = new ArrayList<Variable>();
			for(Variable v : v_tmp){
				Variable new_v = v.clone_e();
				this.called_method_args.add(new_v);
			}
		}
		
		//Refinement_typeは中身変わらないからいらない
	}
	
	
	
	public void add_path_condition(BoolExpr expr) throws Exception{
		System.out.println("check unreachable");
		if(this.pathcondition==null){
			this.pathcondition = expr;
		}else{
			this.pathcondition = this.ctx.mkAnd(this.pathcondition, expr);
		}
		
		this.solver.push();
		if(this.pathcondition!=null){
			this.solver.add(this.pathcondition);
		}
		if(solver.check() == Status.SATISFIABLE) {

        }else{
        	throw new Exception("Unreachable. when path condition is " + this.pathcondition);
        }
		this.solver.pop();
	}
	
	//論理式内でのパスコンディションなど
	//Unreachableでもいい
	public void add_path_condition_tmp(BoolExpr expr) throws Exception{
		
		if(this.pathcondition==null){
			this.pathcondition = expr;
		}else{
			this.pathcondition = this.ctx.mkAnd(this.pathcondition, expr);
		}
		
	}
	
	//localの篩型も含めて探す
	public refinement_type search_refinement_type(String class_name, String type_name){
		//フィールド
		refinement_type rt = this.Check_status_share.compilation_unit.search_refinement_type(class_name, type_name);
		if(rt != null) return rt;
		//ローカル
		for(Pair<String,refinement_type> local_rt : this.local_refinements){
			refinement_type refinement_type = local_rt.get_snd(type_name);
			if(refinement_type != null) return refinement_type; 
		}
		return null;
	}
	
	
	public void constructor_refinement_check() throws Exception{
		System.out.println("check all refinement type predicates");
		this_field.assert_all_refinement_type(0, 2, null, new ArrayList<IntExpr>(), this);
	}
	
	
	
	public void assert_all_refinement_type() throws Exception{
		System.out.println("check all refinement");
		this_field.assert_all_refinement_type(0, invariant_refinement_type_deep_limmit+1, null, new ArrayList<IntExpr>(), this);
		//ローカル変数も
		for(Variable v : variables){
			v.assert_all_refinement_type(0, invariant_refinement_type_deep_limmit+1, null, new ArrayList<IntExpr>(), this);
		}
		
		
	}
	
	//必要なinvariant
	public BoolExpr all_invariant_expr() throws Exception{
		BoolExpr ret_expr = this.ctx.mkBool(true);
		ret_expr = this.ctx.mkAnd(ret_expr, this_field.all_invariants_expr(0, invariant_refinement_type_deep_limmit, null, new ArrayList<IntExpr>(), this));
		//ローカル変数も
		for(Variable v : variables){
			if(!(v.type.equals("int") || v.type.equals("boolean")) && v.is_arg){//引数だけ考える
				ret_expr = this.ctx.mkAnd(ret_expr, v.all_invariants_expr(0, invariant_refinement_type_deep_limmit, null, new ArrayList<IntExpr>(), this));
			}
		}
		
		return ret_expr;
	}
	
	public void check_array_alias(Field f1, Expr f1_class_object_expr, ArrayList<IntExpr> f1_indexs, Field f2, Expr f2_class_object_expr, ArrayList<IntExpr> f2_indexs) throws Exception{
		//配列の篩型が安全かどうか
		BoolExpr pathcondition;
		if(this.pathcondition==null){
			pathcondition = this.ctx.mkBool(true);
		}else{
			pathcondition = this.pathcondition;
		}
		if(f2!=null && f2.dims>0 && f2.dims!=f2_indexs.size() && f2.hava_refinement_type() && f2.have_index_access(this)){
			if(f1.dims>0 && f1.dims!=f1_indexs.size() && f1.hava_refinement_type() && f1.have_index_access(this)){//どっちも篩型を持つ配列
				this.equal_predicate(f1, f1_indexs, f1_class_object_expr, f2, f2_indexs, f2_class_object_expr);
			}else if(f1.dims>0 && f1.dims!=f1_indexs.size() && f1 instanceof Variable){//ローカル変数
				if(((Variable) f1).out_loop_v) throw new Exception("can not alias with refined array　in loop");//ループの中ではエイリアスできない
				
				Expr alias;
				if(((Variable) f1).alias == null){
					alias = this.ctx.mkBool(false);
				}else{
					alias = ((Variable) f1).alias;
				}
				
				this.assert_constraint(this.ctx.mkNot(alias));
				
				Expr alias_refined;
				if(((Variable) f1).alias_refined == null){
					alias_refined = this.ctx.mkBool(false);
				}else{
					alias_refined = ((Variable) f1).alias_refined;
				}
				
				this.assert_constraint(this.ctx.mkNot(alias_refined));
				
				if(((Variable) f1).alias_refined == null){
					((Variable) f1).alias_refined = pathcondition;
				}else{
					((Variable) f1).alias_refined = this.ctx.mkOr(((Variable) f1).alias_refined, pathcondition);
				}
			}else{//篩型の安全を保証できないような大入
				throw new Exception("can not alias with refined array");
			}
		}else if(f1.dims>0 && f1.dims!=f1_indexs.size()  && f1.hava_refinement_type() && f1.have_index_access(this)){
			if(f2!=null && f2.dims>0 && f2.dims!=f2_indexs.size() && f2 instanceof Variable){//ローカル変数
				
				if(((Variable)f2).out_loop_v) throw new Exception("can not alias with refined array　in loop");//ループの中ではエイリアスできない
				
				Expr alias;
				if(((Variable) f2).alias == null){
					alias = this.ctx.mkBool(false);
				}else{
					alias = ((Variable) f2).alias;
				}
				
				this.assert_constraint(this.ctx.mkNot(alias));
				
				Expr alias_refined;
				if(((Variable) f2).alias_refined == null){
					alias_refined = this.ctx.mkBool(false);
				}else{
					alias_refined = ((Variable) f2).alias_refined;
				}
				
				this.assert_constraint(this.ctx.mkNot(alias_refined));
				
				if(((Variable) f2).alias_refined == null){
					((Variable) f2).alias_refined = pathcondition;
				}else{
					((Variable) f2).alias_refined = this.ctx.mkOr(((Variable) f2).alias_refined, pathcondition);
				}
			}else{//篩型の安全を保証できないような大入
				throw new Exception("can not alias with refined array");
			}	
		}else{ 
			if(f2!=null && f2.dims>0 && f2.dims!=f2_indexs.size() && f2 instanceof Variable && !(f1 != null && f1.new_array)){//ローカル変数
				Expr alias_refined;
				if(((Variable) f2).alias_refined == null){
					alias_refined = this.ctx.mkBool(false);
				}else{
					alias_refined = ((Variable) f2).alias_refined;
				}
				
				this.assert_constraint(this.ctx.mkNot(alias_refined));
				
				if(((Variable) f2).alias == null){
					((Variable) f2).alias = pathcondition;
				}else{
					((Variable) f2).alias = this.ctx.mkOr(((Variable) f2).alias, pathcondition);
				}
			}
			if(f1!=null && f1.dims>0 && f1.dims!=f1_indexs.size() && f1 instanceof Variable && !(f2!=null && f2.new_array)){//ローカル変数
				Expr alias_refined;
				if(((Variable) f1).alias_refined == null){
					alias_refined = this.ctx.mkBool(false);
				}else{
					alias_refined = ((Variable) f1).alias_refined;
				}
				
				this.assert_constraint(this.ctx.mkNot(alias_refined));
				
				
				if(((Variable) f1).alias == null){
					((Variable) f1).alias = pathcondition;
				}else{
					((Variable) f1).alias = this.ctx.mkOr(((Variable) f1).alias, pathcondition);
				}
				
			}
		}
	}

	//配列の篩型同士の比較
	//assign_field_exprは長さに関する制約で使う
	public void equal_predicate(Field field_1, ArrayList<IntExpr> indexs_1, Expr class_Expr_1, Field field_2, ArrayList<IntExpr> indexs_2, Expr class_Expr_2) throws Exception{
		
		ArrayList<Expr> field_1_alias_exprs = new ArrayList<Expr>();
		for(int i = 0; i <= indexs_1.size(); i++){
			field_1_alias_exprs.add(field_1.get_Expr_with_indexs(class_Expr_1, new ArrayList<IntExpr>(indexs_1.subList(0, i)), this));
		}
		
		
		ArrayList<Expr> field_2_alias_exprs = new ArrayList<Expr>();
		for(int i = 0; i <= indexs_2.size(); i++){
			field_2_alias_exprs.add(field_2.get_Expr_with_indexs(class_Expr_2, new ArrayList<IntExpr>(indexs_2.subList(0, i)), this));
		}
		
		
		//変数のtmp_numを破壊してしまってもいいように、cloneしてから作業する
		Check_status cs = this.clone();
		cs.clone_list();
		Field cs_field_1 = cs.search_internal_id(field_1.internal_id);
		Field cs_field_2 = cs.search_internal_id(field_2.internal_id);
		if(cs_field_1!=null){
			field_1 = cs_field_1;
		}else{//variablesやfieldsに含まれない場合
			field_1 = field_1.clone_e();
		}
		if(cs_field_2!=null){
			field_2 = cs_field_2;
		}else{//variablesやfieldsに含まれない場合
			field_2 = field_2.clone_e();
		}
		
		
		//変数は全て値をフレッシュにする
		cs.solver.push();
		ArrayList<Field> refresh_fields = new ArrayList<Field>();
		for(Field f : cs.fields){
			if(!(f.modifiers!=null && f.modifiers.is_final)){
				f.temp_num += 9999;//9999を足す。別に被ってもpushpopによって問題ないので、今後9999回代入されてもいい
				refresh_fields.add(f);
			}
		}
		for(Variable v : cs.variables){
			if(!(v.modifiers!=null && v.modifiers.is_final)){
				v.temp_num += 9999;
				refresh_fields.add(v);
			}
		}
		for(Model_Field mf : cs.model_fields){
			if(!(mf.modifiers!=null && mf.modifiers.is_final)){
				mf.temp_num += 9999;//9999を足す。別に被ってもpushpopによって問題ないので、今後9999回代入されてもいい
				refresh_fields.add(mf);
			}
		}
		//配列も初期化
		cs.array_arrayref = new Array(cs.ctx.mkUninterpretedSort("ArrayRef"), cs);
		cs.array_int = new Array(cs.ctx.mkIntSort(), cs);
		cs.array_boolean = new Array(cs.ctx.mkBoolSort(), cs);
		cs.array_ref = new Array(cs.ctx.mkUninterpretedSort("Ref"), cs);
		
		
		//フレッシュにした状態で必要な値を用意する
		Expr field_1_expr = null;
		if(field_1 instanceof Variable){
			field_1_expr = field_1.get_Expr(cs);
		}else{
			field_1_expr = cs.ctx.mkSelect(field_1.get_Expr(cs), class_Expr_1);
		}
		
		Expr field_2_expr = null;
		if(field_2 instanceof Variable){
			field_2_expr = field_2.get_Expr(cs);
		}else{
			field_2_expr = cs.ctx.mkSelect(field_2.get_Expr(cs), class_Expr_2);
		}
		
		Expr expr_1 = field_1.get_Expr_with_indexs(class_Expr_1, indexs_1, cs);
		
		Expr expr_2 = field_2.get_Expr_with_indexs(class_Expr_2, indexs_2, cs);
		
		
		//エイリアスする部分は変わらない
		for(int i = 0; i <= indexs_1.size(); i++){
			Expr alias_ex = field_1_alias_exprs.get(i);
			Expr ex = field_1.get_Expr_with_indexs(class_Expr_1, new ArrayList<IntExpr>(indexs_1.subList(0, i)), cs);
			
			cs.add_constraint(cs.ctx.mkEq(alias_ex, ex));
		}
		for(int i = 0; i <= indexs_2.size(); i++){
			Expr alias_ex = field_2_alias_exprs.get(i);
			Expr ex = field_2.get_Expr_with_indexs(class_Expr_2, new ArrayList<IntExpr>(indexs_2.subList(0, i)), cs);
			
			cs.add_constraint(cs.ctx.mkEq(alias_ex, ex));
		}
		
		//エイリアスしているという制約
		cs.add_constraint(cs.ctx.mkEq(expr_1, expr_2));
		
		
		Array array;
		Expr fresh_array;
		String ret;
		
		//この型を持つ変数の値が変わっても他方の述語は満たされる
		System.out.println("check refinement type equality 1");
		cs.solver.push();
		
		field_2.add_refinement_constraint(cs, class_Expr_2, true);
		
		//篩型を満たすような新しい配列を1の方に代入する
		ret = "fresh_array" + "_" + cs.Check_status_share.get_tmp_num();
		if(field_1.dims > indexs_1.size()+1){
		    array = cs.array_arrayref;
			fresh_array = cs.ctx.mkArrayConst(ret, cs.ctx.mkIntSort(), cs.ctx.mkUninterpretedSort("ArrayRef"));
		}else{
		    if(field_1.type.equals("int")){
		        array = cs.array_int;
		    	fresh_array = cs.ctx.mkArrayConst(ret, cs.ctx.mkIntSort(), cs.ctx.mkIntSort());
		    }else if(field_1.type.equals("boolean")){
		        array = cs.array_boolean;
		    	fresh_array = cs.ctx.mkArrayConst(ret, cs.ctx.mkIntSort(), cs.ctx.mkBoolSort());
		    }else{
		        array = cs.array_ref;
		    	fresh_array = cs.ctx.mkArrayConst(ret, cs.ctx.mkIntSort(), cs.ctx.mkUninterpretedSort("Ref"));
		    }
		}
		array.update_array(expr_1, fresh_array, cs);
		field_1.add_refinement_constraint(cs, class_Expr_1, true);
		
		field_2.assert_refinement(cs, class_Expr_2);
		
		cs.solver.pop();
		
		//他方の篩型を持つ変数の値が変わってもこの型の述語は満たされる
		System.out.println("check refinement type equality 2");
		cs.solver.push();
		
		field_1.add_refinement_constraint(cs, class_Expr_1, true);
		
		//篩型を満たすような新しい配列を1の方に代入する
		ret = "fresh_array" + "_" + cs.Check_status_share.get_tmp_num();
		if(field_2.dims > indexs_2.size()+1){
		    array = cs.array_arrayref;
			fresh_array = cs.ctx.mkArrayConst(ret, cs.ctx.mkIntSort(), cs.ctx.mkUninterpretedSort("ArrayRef"));
		}else{
		    if(field_2.type.equals("int")){
		        array = cs.array_int;
		    	fresh_array = cs.ctx.mkArrayConst(ret, cs.ctx.mkIntSort(), cs.ctx.mkIntSort());
		    }else if(field_2.type.equals("boolean")){
		        array = cs.array_boolean;
		    	fresh_array = cs.ctx.mkArrayConst(ret, cs.ctx.mkIntSort(), cs.ctx.mkBoolSort());
		    }else{
		        array = cs.array_ref;
		    	fresh_array = cs.ctx.mkArrayConst(ret, cs.ctx.mkIntSort(), cs.ctx.mkUninterpretedSort("Ref"));
		    }
		}
		array.update_array(expr_2, fresh_array, cs);
		field_2.add_refinement_constraint(cs, class_Expr_2, true);
		
		field_1.assert_refinement(cs, class_Expr_1);
		
		cs.solver.pop();
		
		cs.solver.pop();

	}
	
	public void refresh_all_array(BoolExpr condition) throws Exception{
		this.array_arrayref.refresh(condition, this);
		this.array_int.refresh(condition, this);
		this.array_boolean.refresh(condition, this);
		this.array_ref.refresh(condition, this);
	}
}
