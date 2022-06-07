package system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.microsoft.z3.*;

import system.parsers.*;

public class Check_status {
	public Check_status_share Check_status_share;
	public BoolExpr pathcondition;
	public List<Variable> variables;
	//List<Variable> before_if_variables;
	public List<Field> fields;
	public List<invariant> invariants;
	public Context ctx;
	public Solver solver;
	public Field this_field;
	public List<Pair<String, refinement_type>> local_refinements;
	
	public boolean in_refinement_predicate;
	public Field refined_Field;
	public Expr refined_Expr;
	public String refinement_type_value;
	//篩型を持つフィールドを持つクラス
	public Field refined_class_Field;
	public IntExpr refined_class_Field_index;
	public Expr refined_class_Expr;
	
	public boolean in_method_call;
	public Expr call_expr;
	public Field call_field;
	public IntExpr call_field_index;
	public List<Variable> called_method_args;
	public Check_status old_status;
	public Variable result;
	
	//返り値
	public Variable return_v;
	public Expr return_expr;//事後条件用
	public List<Expr> return_exprs;
	public boolean after_return;
	//BoolExpr return_pathcondition;
	public List<BoolExpr> return_pathconditions;
	public Check_status this_old_status;
	
	//assignable
	public List<Pair<Field, BoolExpr>> assignables;//nullだったら何でも代入していい、要素0は\nothing???
	
	//JML節の中の可視性の確認
	public boolean ban_private_visibility;
	public boolean ban_default_visibility;
	
	public Right_side_status right_side_status;
	
	public int refinement_deep, refinement_deep_limmit;
	
	public boolean in_constructor;
	
	public Check_status(compilation_unit cu){
		variables = new ArrayList<Variable>();
		this.Check_status_share = new Check_status_share(cu);
		this.ctx = new Context(new HashMap<>());
		this.solver = ctx.mkSolver();
		this.local_refinements = new ArrayList<Pair<String, refinement_type>>();
		this.fields = new ArrayList<Field>();
		this.return_exprs = new ArrayList<Expr>();
		this.return_pathconditions = new ArrayList<BoolExpr>();
		//this.assignables = new ArrayList<Field>();
		this.right_side_status = new Right_side_status();
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
	
	public Field search_field(String ident, Field class_object, IntExpr class_object_index, Check_status cs) throws Exception{
		
		variable_definition vd = this.Check_status_share.compilation_unit.search_field(class_object.type, ident);
		
		if(vd == null){
			return null;
		}
		
		String field_name = ident + "_" + vd.class_type_name;
		
		for(Field v :fields){
			if(field_name.equals(v.field_name)&&v.class_object.equals(class_object)){
				if(v.class_object_index==null && class_object_index==null){
					return v;
				}else if(v.class_object_index!=null && class_object_index!=null){//どちらかがnullは一致しないもの
				
					BoolExpr expr;
					BoolExpr arg_expr = cs.ctx.mkEq(v.class_object_index, class_object_index);
					if(cs.pathcondition!=null){
						expr = cs.ctx.mkAnd(cs.pathcondition, cs.ctx.mkNot(arg_expr));
					}else{
						expr = cs.ctx.mkNot(arg_expr);
					}

					cs.solver.push();
					cs.solver.add(expr);
					if(cs.solver.check() != Status.SATISFIABLE) {
						cs.solver.pop();
						return v;
					}else{
						cs.solver.pop();
					}
				}
			}
		}
		
		Field f = new Field(this.Check_status_share.get_tmp_num(),field_name , vd.variable_decls.type_spec.type.type, vd.variable_decls.type_spec.dims, vd.variable_decls.type_spec.refinement_type_clause, vd.modifiers, class_object, class_object_index);
		this.fields.add(f);
		return f;
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
	
	/*
	public Field get_field(String ident, Field class_object, IntExpr class_object_index, Check_status cs) throws Exception{
		for(Field v :fields){
			if(ident.equals(v.field_name)&&( (v.class_object==null && class_object==null) ||v.class_object.equals(class_object))){
				if(v.class_object_index==null && class_object_index==null){
					return v;
				}else if(v.class_object_index!=null && class_object_index!=null){
				
					BoolExpr expr;
					BoolExpr arg_expr = cs.ctx.mkEq(v.class_object_index, class_object_index);
					if(cs.pathcondition!=null){
						expr = cs.ctx.mkAnd(cs.pathcondition, cs.ctx.mkNot(arg_expr));
					}else{
						expr = cs.ctx.mkNot(arg_expr);
					}

					cs.solver.push();
					cs.solver.add(expr);
					if(cs.solver.check() != Status.SATISFIABLE) {
						cs.solver.pop();
						return v;
					}else{
						cs.solver.pop();
					}

				}
			}
		}
		throw new Exception("can't find " + ident + "\n");
		//return null;
	}
	*/
	
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
		
		this.solver.push();
		if(this.pathcondition!=null){
			this.solver.add(this.pathcondition);
		}
		if(solver.check() == Status.SATISFIABLE) {

        }else{
        	throw new Exception("Unreachable.");
        }
		this.solver.pop();
	}
	
	public void assert_constraint(BoolExpr arg_expr) throws Exception{
		BoolExpr expr;
		if(this.pathcondition!=null){
			expr = this.ctx.mkAnd(this.pathcondition, this.ctx.mkNot(arg_expr));
		}else{
			expr = this.ctx.mkNot(arg_expr);
		}
		System.out.println("assert " + expr);
		
		if(solver.check() == Status.SATISFIABLE) {

        }else{
        	throw new Exception("Unreachable.");
        }

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
	
	/*
	public Field add_field(String field_name, Field class_object, IntExpr class_object_index) throws Exception{
		variable_definition vd = this.Check_status_share.compilation_unit.search_field(class_object.type, field_name);
		if(vd != null){
			Field f = new Field(this.Check_status_share.get_tmp_num(),field_name , vd.variable_decls.type_spec.type.type, vd.variable_decls.type_spec.dims, vd.variable_decls.type_spec.refinement_type_clause, vd.modifiers, class_object, class_object_index);
			this.fields.add(f);
			return f;
		}else{
			//System.out.println(class_object + "dont have" + field_name);
			return null;
		}
	}
	*/

	
	public Check_status clone(){
		Check_status cs = new Check_status();
		cs.Check_status_share = this.Check_status_share;
		cs.ctx = this.ctx;
		cs.pathcondition = this.pathcondition;
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
		cs.call_expr = this.call_expr;
		cs.call_field = this.call_field;
		cs.call_field_index = this.call_field_index;
		
		cs.return_v = this.return_v;
		cs.return_expr = this.return_expr;
		cs.return_exprs = this.return_exprs;
		cs.after_return = this.after_return;
		cs.return_pathconditions = this.return_pathconditions;
		cs.this_old_status = this.this_old_status;
		
		cs.assignables = this.assignables;
		
		cs.right_side_status = this.right_side_status;
		
		cs.refinement_deep = this.refinement_deep;
		cs.refinement_deep_limmit = this.refinement_deep_limmit;
		
		cs.refined_class_Field = this.refined_class_Field;
		cs.refined_class_Field_index = this.refined_class_Field_index;
		cs.refined_class_Expr = this.refined_class_Expr;
		cs.in_constructor = this.in_constructor;
		
		return cs;
	}
	
	public void clone_list() throws Exception{
		if(this.variables!=null){
			List<Variable> v_tmp = this.variables;
			this.variables = new ArrayList<Variable>();
			for(Variable v : v_tmp){
				Variable new_v = v.clone_e();
				new_v.id = this.Check_status_share.get_tmp_num();
				this.variables.add(new_v);
			}
		}

		if(this.fields!=null){
			List<Field> f_tmp = this.fields;
			this.fields = new ArrayList<Field>();
			for(Field f : f_tmp){
				Field new_f = f.clone_e();
				new_f.id = this.Check_status_share.get_tmp_num();
				this.fields.add(new_f);
			}
		}
		
		if(this.called_method_args!=null){
			List<Variable> v_tmp = this.called_method_args;
			this.called_method_args = new ArrayList<Variable>();
			for(Variable v : v_tmp){
				Variable new_v = v.clone_e();
				new_v.id = this.Check_status_share.get_tmp_num();
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
        	throw new Exception("Unreachable.");
        }
		this.solver.pop();
	}
	
	
	public void constructor_refinement_check() throws Exception{
		System.out.println("check all refinement type predicates");
		for(Field f : this.fields){
			if(f.class_object!=null && f.class_object.equals(this_field, this) && f.refinement_type_clause!=null){
				if(f.refinement_type_clause.refinement_type!=null){
					f.refinement_type_clause.refinement_type.assert_refinement(this, f, this.ctx.mkSelect((ArrayExpr) f.get_Expr(this), this.this_field.get_Expr(this)));
				}else if(f.refinement_type_clause.ident!=null){
					refinement_type rt = this.search_refinement_type(f.class_object.type, f.refinement_type_clause.ident);
					if(rt!=null){
						rt.assert_refinement(this, f, this.ctx.mkSelect((ArrayExpr) f.get_Expr(this), this.this_field.get_Expr(this)));
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
