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
	public List<invariant> invariants;
	public Context ctx;
	public Solver solver;
	public Field this_field;
	public List<Pair<String, refinement_type>> local_refinements;
	
	
	public Expr instance_expr;
	public Field instance_Field;
	public ArrayList<IntExpr> instance_indexs;
	
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
	public boolean use_only_helper_method;
	
	
	public boolean in_helper; //helperメソッドの中かどうか
	public ArrayList<Helper_assigned_field> helper_assigned_fields;
	public int field_deep_limmit;
	
	public ArrayList<Pair<Field, Expr>> checked_refinement_type_field;//篩型の制約を既に追加したもの　Fieldとclass_objectのExprのPair    使い切りなのでcloneでは中身は気にしなくていい
	
	public Check_status(compilation_unit cu){
		variables = new ArrayList<Variable>();
		this.Check_status_share = new Check_status_share(cu);
		this.ctx = new Context(new HashMap<>());
		this.solver = ctx.mkSolver();
		this.local_refinements = new ArrayList<Pair<String, refinement_type>>();
		this.fields = new ArrayList<Field>();
		this.model_fields = new ArrayList<Model_Field>();
		//this.assignables = new ArrayList<Field>();
		this.right_side_status = new Right_side_status();
		quantifiers = new ArrayList<Pair<String, Expr>>();
		helper_assigned_fields = new ArrayList<Helper_assigned_field>();
		checked_refinement_type_field = new ArrayList<Pair<Field, Expr>>();
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
	public Field search_field(String ident, Field class_object, Check_status cs) throws Exception{
		
		variable_definition vd = this.Check_status_share.compilation_unit.search_field(class_object.type, ident, false);
		
		if(vd == null){
			return null;
		}
		
		String field_name = ident + "_" + vd.class_type_name;
		
		for(Field v :fields){
			if(field_name.equals(v.field_name + "_" + v.class_type_name)&&v.class_object.equals(class_object, cs)){
				return v;
			}
		}
		
		//データグループのリストを作る
		ArrayList<Model_Field> data_groups = new ArrayList<Model_Field>();
		for(group_name gn : vd.group_names){
			String class_type = null;
			if(gn.is_super){
				class_declaration cd = this.Check_status_share.compilation_unit.search_class(class_object.type);
				class_type = cd.super_class.class_name;
			}else{
				class_type = class_object.type;
			}
			
			String pre_type = class_object.type;
			class_object.type = class_type;
			data_groups.add(search_model_field(gn.ident, class_object, this));
			class_object.type = pre_type;
		}
		
		Field f = new Field(this.Check_status_share.get_tmp_num(), ident, vd.variable_decls.type_spec.type.type, vd.variable_decls.type_spec.dims, vd.variable_decls.type_spec.refinement_type_clause, vd.modifiers, class_object, vd.class_type_name, this.ctx.mkBool(true), data_groups);
		
		//新しく追加したフィールドはassinable節で触れられていない
		List<List<IntExpr>> indexs = new ArrayList<List<IntExpr>>();
		indexs.add(new ArrayList<IntExpr>());
		f.assinable_cnst_indexs.add(new Pair<BoolExpr,List<List<IntExpr>>>(cs.ctx.mkBool(false), indexs));
		
		this.fields.add(f);
		
		//初期値をold用のcsにも追加しておく
		if(cs.this_old_status!=null){
			Field f_old = f.clone_e();
			cs.this_old_status.fields.add(f_old);
			f_old.class_object = search_internal_id(f.class_object.internal_id);
		}
		
		
		
		return f;
	}
	
	//identはxとかで検索
	public Model_Field search_model_field(String ident, Field class_object, Check_status cs) throws Exception{
		
		variable_definition vd = this.Check_status_share.compilation_unit.search_field(class_object.type, ident, true);
		
		if(vd == null){
			return null;
		}
		
		String field_name = ident + "_" + vd.class_type_name;
		
		for(Model_Field model_field :model_fields){
			if(field_name.equals(model_field.field_name + "_" + model_field.class_type_name) && model_field.class_object.equals(class_object, cs)){
				return model_field;
			}
		}
		
		//データグループのリストを作る
		ArrayList<Model_Field> data_groups = new ArrayList<Model_Field>();
		for(group_name gn : vd.group_names){
			String class_type = null;
			if(gn.is_super){
				class_declaration cd = this.Check_status_share.compilation_unit.search_class(class_object.type);
				class_type = cd.super_class.class_name;
			}else{
				class_type = class_object.type;
			}
			
			String pre_type = class_object.type;
			class_object.type = class_type;
			data_groups.add(search_model_field(gn.ident, class_object, this));
			class_object.type = pre_type;
		}
		
		Model_Field mf = new Model_Field(this.Check_status_share.get_tmp_num(), ident, vd.variable_decls.type_spec.type.type, vd.variable_decls.type_spec.dims, vd.variable_decls.type_spec.refinement_type_clause, vd.modifiers, class_object, vd.class_type_name, this.ctx.mkBool(true), data_groups);
		
		
		
		this.model_fields.add(mf);
		
		//初期値をold用のcsにも追加しておく
		if(cs.this_old_status!=null){
			Model_Field mf_old = mf.clone_e();
			cs.this_old_status.model_fields.add(mf_old);
			mf_old.class_object = search_internal_id(mf.class_object.internal_id);
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
		Variable v = new Variable(this.Check_status_share.get_tmp_num(), variable, type, dims, refinement_type_clause, modifiers, this.this_field, alias_2d);
		this.variables.add(v);
		return v;
	}
	

	
	public Check_status clone(){
		Check_status cs = new Check_status();
		cs.md = this.md;
		cs.Check_status_share = this.Check_status_share;
		cs.ctx = this.ctx;
		cs.pathcondition = this.pathcondition;
		cs.return_conditions = this.return_conditions;
		cs.solver = this.solver;
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
		
		cs.invariants = this.invariants;
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
		cs.instance_Field = this.instance_Field;
		cs.instance_indexs = this.instance_indexs;
		
		cs.in_constructor = this.in_constructor;
		
		cs.can_not_use_mutable = this.can_not_use_mutable;
		
		cs.quantifiers = new ArrayList<Pair<String, Expr>>();
		
		cs.in_jml_predicate = this.in_jml_predicate;
		cs.use_only_helper_method = this.use_only_helper_method;
		
		cs.in_helper = this.in_helper;
			
		cs.helper_assigned_fields = new ArrayList<Helper_assigned_field>();
		for(Helper_assigned_field assigned_field : this.helper_assigned_fields){
			cs.helper_assigned_fields.add(assigned_field);
		}
		cs.field_deep_limmit = this.field_deep_limmit;
		
		cs.checked_refinement_type_field = this.checked_refinement_type_field;
		
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
	
	
	
	public void add_assign(Expr postfix_tmp, Expr implies_tmp) throws Exception{
		BoolExpr expr = this.ctx.mkEq(postfix_tmp, implies_tmp);
		this.add_constraint(expr);
	}
	
	public void add_path_condition(BoolExpr expr) throws Exception{
		System.out.println("check unreachable");
		if(this.pathcondition==null){
			this.pathcondition = expr;
		}else{
			this.pathcondition = this.ctx.mkAnd(this.pathcondition, expr);
		}
		/*これはOpneJMLではやっていない？　やらなくていいかも
		this.solver.push();
		if(this.pathcondition!=null){
			this.solver.add(this.pathcondition);
		}
		if(solver.check() == Status.SATISFIABLE) {

        }else{
        	throw new Exception("Unreachable. when path condition is " + this.pathcondition);
        }
		this.solver.pop();
		*/
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
	
	
	public void constructor_refinement_check() throws Exception{
		System.out.println("check all refinement type predicates");
		class_declaration cd = this.Check_status_share.compilation_unit.search_class(this.this_field.class_type_name);
		for(Field f : this.fields){
			if(f.class_object!=null && f.class_object.equals(this_field, this) && f.hava_refinement_type()){
				f.assert_refinement(this, this.this_field.get_Expr(this), new ArrayList<IntExpr>());
			}
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
	
	public void assert_all_refinement_type() throws Exception{
		System.out.println("check all refinement");
		class_declaration cd = Check_status_share.compilation_unit.search_class(this_field.type);
		ArrayList<Field> fields = cd.all_field(0, field_deep_limmit, this_field, this);
		for(Field field : fields){
			if(field.hava_refinement_type()){
				Pair<Expr, ArrayList<IntExpr>> expr_indexs = field.class_object.fresh_index_full_expr(this);
				Expr class_object_expr = expr_indexs.fst;
				ArrayList<IntExpr> indexs = expr_indexs.snd;
				Expr expr = this.ctx.mkSelect(field.get_Expr(this), class_object_expr);
				
				field.assert_refinement(this, class_object_expr, indexs);
			}
		}
	}
	
	public void check_array_alias(Field f1, Expr f1_expr, Expr f1_class_object_expr, ArrayList<IntExpr> f1_indexs, Field f2, Expr f2_expr, Expr f2_class_object_expr, ArrayList<IntExpr> f2_indexs) throws Exception{
		//配列の篩型が安全かどうか
		BoolExpr pathcondition;
		if(this.pathcondition==null){
			pathcondition = this.ctx.mkBool(true);
		}else{
			pathcondition = this.pathcondition;
		}
		if(f2!=null && f2.dims>0 && f2.dims_sum()!=f2_indexs.size() && f2.hava_refinement_type() && f2.have_index_access(this)){
			if(f1.dims>0 && f1.dims_sum()!=f1_indexs.size() && f1.hava_refinement_type() && f1.have_index_access(this)){//どっちも篩型を持つ配列
				this.equal_predicate(f1, f1_indexs, f1_class_object_expr, f2, f2_indexs, f2_class_object_expr);
			}else if(f1.dims>0 && f1.dims_sum()!=f1_indexs.size() && f1 instanceof Variable){//ローカル変数
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
		}else if(f1.dims>0 && f1.dims_sum()!=f1_indexs.size()  && f1.hava_refinement_type() && f1.have_index_access(this)){
			if(f2!=null && f2.dims>0 && f2.dims_sum()!=f2_indexs.size() && f2 instanceof Variable){//ローカル変数
				
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
			if(f2!=null && f2.dims>0 && f2.dims_sum()!=f2_indexs.size() && f2 instanceof Variable && !(f1 != null && f1.new_array)){//ローカル変数
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
			if(f1!=null && f1.dims>0 && f1.dims_sum()!=f1_indexs.size() && f1 instanceof Variable && !(f2!=null && f2.new_array)){//ローカル変数
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
		Expr field_1_alias_expr = null;
		if(field_1 instanceof Variable){
			field_1_alias_expr = field_1.get_Expr(this);
		}else{
			field_1_alias_expr = this.ctx.mkSelect(field_1.get_Expr(this), class_Expr_1);
		}
		
		Expr field_2_alias_expr = null;
		if(field_2 instanceof Variable){
			field_2_alias_expr = field_2.get_Expr(this);
		}else{
			field_2_alias_expr = this.ctx.mkSelect(field_2.get_Expr(this), class_Expr_2);
		}
		
		
		ArrayList<IntExpr> field_1_indexs = new ArrayList<IntExpr>(indexs_1.subList(field_1.class_object_dims_sum(), indexs_1.size()));
		ArrayList<IntExpr> field_2_indexs = new ArrayList<IntExpr>(indexs_2.subList(field_2.class_object_dims_sum(), indexs_2.size()));
		
		//変数は全て値をフレッシュにする
		this.solver.push();
		ArrayList<Field> refresh_fields = new ArrayList<Field>();
		for(Field f : this.fields){
			if(!(f.modifiers!=null && f.modifiers.is_final)){
				f.temp_num += 9999;//9999を足す。別に被ってもpushpopによって問題ないので、今後9999回代入されてもいい
				refresh_fields.add(f);
			}
		}
		for(Variable v : this.variables){
			if(!(v.modifiers!=null && v.modifiers.is_final)){
				v.temp_num += 9999;
				refresh_fields.add(v);
			}
		}
		
		//フレッシュにした状態で必要な値を用意する
		Expr field_1_expr = null;
		if(field_1 instanceof Variable){
			field_1_expr = field_1.get_Expr(this);
		}else{
			field_1_expr = this.ctx.mkSelect(field_1.get_Expr(this), class_Expr_1);
		}
		
		Expr field_2_expr = null;
		if(field_2 instanceof Variable){
			field_2_expr = field_2.get_Expr(this);
		}else{
			field_2_expr = this.ctx.mkSelect(field_2.get_Expr(this), class_Expr_2);
		}
		
		Expr expr_1 = null;
		if(field_1 instanceof Variable){
			expr_1 = field_1.get_Expr(this);
		}else{
			expr_1 = this.ctx.mkSelect(field_1.get_Expr(this), class_Expr_1);
		}
		for(IntExpr index : field_1_indexs){
			expr_1 = this.ctx.mkSelect(expr_1, index);
		}
		
		Expr expr_2 = null;
		if(field_2 instanceof Variable){
			expr_2 = field_2.get_Expr(this);
		}else{
			expr_2 = this.ctx.mkSelect(field_2.get_Expr(this), class_Expr_2);
		}
		for(IntExpr index : field_2_indexs){
			expr_2 = this.ctx.mkSelect(expr_2, index);
		}
		
		
		//エイリアスする部分に関する配列の長さは変わらない
		String array_type;
		if(field_1.type.equals("int")){
			array_type = "int";
		}else if(field_1.type.equals("boolean")){
			array_type = "boolean";
		}else{
			array_type = "ref";
		}
		
		for(int i = 0; i <= field_1_indexs.size(); i++){
			Expr alias_ex = field_1_alias_expr;
			if(i>0){
				for(IntExpr index : field_1_indexs.subList(0, i)){
					alias_ex = this.ctx.mkSelect(alias_ex, index);
				}
			}
			Expr ex = field_1_expr;
			if(i>0){
				for(IntExpr index : field_1_indexs.subList(0, i)){
					ex = this.ctx.mkSelect(ex, index);
				}
			}
			
			//長さに関する制約
			int array_dim = field_1.dims - i;
			
			IntExpr length1 = (IntExpr) this.ctx.mkSelect(this.ctx.mkArrayConst("length_" + array_dim + "d_" + array_type, alias_ex.getSort(), this.ctx.mkIntSort()), alias_ex);
			IntExpr length2 = (IntExpr) this.ctx.mkSelect(this.ctx.mkArrayConst("length_" + array_dim + "d_" + array_type, ex.getSort(), this.ctx.mkIntSort()), ex);
			this.add_constraint(this.ctx.mkEq(length1, length2));
		}
		for(int i = 0; i <= field_2_indexs.size(); i++){
			Expr alias_ex = field_2_alias_expr;
			if(i>0){
				for(IntExpr index : field_2_indexs.subList(0, i)){
					alias_ex = this.ctx.mkSelect(alias_ex, index);
				}
			}
			Expr ex = field_2_expr;
			if(i>0){
				for(IntExpr index : field_2_indexs.subList(0, i)){
					ex = this.ctx.mkSelect(ex, index);
				}
			}
			
			//長さに関する制約
			int array_dim = field_2.dims - i;
			
			IntExpr length1 = (IntExpr) this.ctx.mkSelect(this.ctx.mkArrayConst("length_" + array_dim + "d_" + array_type, alias_ex.getSort(), this.ctx.mkIntSort()), alias_ex);
			IntExpr length2 = (IntExpr) this.ctx.mkSelect(this.ctx.mkArrayConst("length_" + array_dim + "d_" + array_type, ex.getSort(), this.ctx.mkIntSort()), ex);
			this.add_constraint(this.ctx.mkEq(length1, length2));
		}
		
		
		
		//この型を持つ変数の値が変わっても他方の述語は満たされる
		System.out.println("check refinement type equality 1");
		this.solver.push();
		
		field_1.add_refinement_constraint(this, class_Expr_1, new ArrayList<IntExpr>(indexs_1.subList(0, field_1.class_object_dims_sum())), true);
		
		
		
		if(field_2_indexs.size() == 0){
			this.add_constraint(this.ctx.mkEq(field_2_expr, expr_1));
			
			field_2.assert_refinement(this, class_Expr_2, new ArrayList<IntExpr>(indexs_2.subList(0, field_2.class_object_dims_sum())));
		}else{
			field_2.add_refinement_constraint(this, class_Expr_2, new ArrayList<IntExpr>(indexs_2.subList(0, field_2.class_object_dims_sum())), true);
			
			
			this.add_constraint(this.ctx.mkEq(field_2.get_Expr_assign(this), field_2.assign_value(indexs_2, expr_1, class_Expr_2, this)));
			
			field_2.temp_num++;

			field_2.assert_refinement(this, class_Expr_2, new ArrayList<IntExpr>(indexs_2.subList(0, field_2.class_object_dims_sum())));
			
			field_2.temp_num--;
		}
		
		this.solver.pop();
		
		//他方の篩型を持つ変数の値が変わってもこの型の述語は満たされる
		System.out.println("check refinement type equality 2");
		this.solver.push();
		
		field_2.add_refinement_constraint(this, class_Expr_2, new ArrayList<IntExpr>(indexs_2.subList(0, field_2.class_object_dims_sum())), true);
		
		
		
		if(field_1_indexs.size() == 0){
			this.add_constraint(this.ctx.mkEq(field_1_expr, expr_2));
			
			field_1.assert_refinement(this, class_Expr_1, new ArrayList<IntExpr>(indexs_1.subList(0, field_1.class_object_dims_sum())));
		}else{
			field_1.add_refinement_constraint(this, class_Expr_1, new ArrayList<IntExpr>(indexs_1.subList(0, field_1.class_object_dims_sum())), true);
			
			
			this.add_constraint(this.ctx.mkEq(field_1.get_Expr_assign(this), field_1.assign_value(indexs_1, expr_2, class_Expr_1, this)));
			
			field_1.temp_num++;

			field_1.assert_refinement(this, class_Expr_1, new ArrayList<IntExpr>(indexs_1.subList(0, field_1.class_object_dims_sum())));
			
			field_1.temp_num--;
		}
		
		this.solver.pop();
		
		//リフレッシュした値は元に戻す
		this.solver.pop();
		for(Field f : refresh_fields){
			f.temp_num -= 9999;
		}

	}
}
