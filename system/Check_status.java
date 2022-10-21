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
	public ArrayList<Pair<BoolExpr, Pair<Field, ArrayList<IntExpr>>>> helper_assigned_fields;
	
	public Check_status(compilation_unit cu){
		variables = new ArrayList<Variable>();
		this.Check_status_share = new Check_status_share(cu);
		this.ctx = new Context(new HashMap<>());
		this.solver = ctx.mkSolver();
		this.local_refinements = new ArrayList<Pair<String, refinement_type>>();
		this.fields = new ArrayList<Field>();
		//this.assignables = new ArrayList<Field>();
		this.right_side_status = new Right_side_status();
		quantifiers = new ArrayList<Pair<String, Expr>>();
		helper_assigned_fields = new ArrayList<Pair<BoolExpr, Pair<Field, ArrayList<IntExpr>>>>();
	}
	
	Check_status(){
		
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
		
		variable_definition vd = this.Check_status_share.compilation_unit.search_field(class_object.type, ident);
		
		if(vd == null){
			return null;
		}
		
		String field_name = ident + "_" + vd.class_type_name;
		
		for(Field v :fields){
			if(field_name.equals(v.field_name + "_" + v.class_type_name)&&v.class_object.equals(class_object, cs)){
				return v;
			}
		}
		
		Field f = new Field(this.Check_status_share.get_tmp_num(), ident, vd.variable_decls.type_spec.type.type, vd.variable_decls.type_spec.dims, vd.variable_decls.type_spec.refinement_type_clause, vd.modifiers, class_object, vd.class_type_name);
		
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
	
	public Variable add_variable(String variable, String type, int dims, refinement_type_clause refinement_type_clause, modifiers modifiers) throws Exception{
		Variable v = new Variable(this.Check_status_share.get_tmp_num(), variable, type, dims, refinement_type_clause, modifiers, this.this_field);
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
			
		cs.helper_assigned_fields = new ArrayList<Pair<BoolExpr, Pair<Field, ArrayList<IntExpr>>>>();
		for(Pair<BoolExpr, Pair<Field, ArrayList<IntExpr>>> assigned_field : this.helper_assigned_fields){
			cs.helper_assigned_fields.add(assigned_field);
		}
		
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
	
	
	public void constructor_refinement_check() throws Exception{
		
		
		System.out.println("check all refinement type predicates");
		class_declaration cd = this.Check_status_share.compilation_unit.search_class(this.this_field.class_type_name);
		for(Field f : this.fields){
			if(f.class_object!=null && f.class_object.equals(this_field, this) && f.refinement_type_clause!=null){
				if(f.refinement_type_clause.refinement_type!=null){
					f.refinement_type_clause.refinement_type.assert_refinement(this, f, this.ctx.mkSelect((ArrayExpr) f.get_Expr(this), this.this_field.get_Expr(this)), this.this_field, this.this_field.get_Expr(this), new ArrayList<IntExpr>());
				}else if(f.refinement_type_clause.ident!=null){
					refinement_type rt = this.search_refinement_type(f.class_object.type, f.refinement_type_clause.ident);
					if(rt!=null){
						rt.assert_refinement(this, f, this.ctx.mkSelect((ArrayExpr) f.get_Expr(this), this.this_field.get_Expr(this)), this.this_field, this.this_field.get_Expr(this), new ArrayList<IntExpr>());
					}else{
						throw new Exception("can't find refinement type " + f.refinement_type_clause.ident);
					}
				}
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
}
