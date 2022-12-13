package system.parsers;


import java.util.ArrayList;

import com.microsoft.z3.*;

import system.Check_status;
import system.Field;
import system.Pair;
import system.Parser;
import system.Parser_status;
import system.Source;
import system.Variable;

public class refinement_type implements Parser<String>{
	type type;
	int dims;
	String ident;
	predicate predicate;
	public String class_type_name;
	
	public String parse(Source s,Parser_status ps)throws Exception{
		String st;
		dims = 0;
		st = new string("{").parse(s,ps);
		st = st + new spaces().parse(s,ps);
		this.type = new type();
		st = st + type.parse(s,ps);
		st = st + new spaces().parse(s,ps);
		
		Source s_backup = s.clone();
		try{
			dims ds = new dims();
			st += ds.parse(s, ps);
			this.dims = ds.dims;
			st = st + new spaces().parse(s,ps);
		}catch (Exception e){
			s.revert(s_backup);
		}
		
		this.ident = new ident().parse(s,ps);
		st = st + this.ident;
		st = st + new spaces().parse(s,ps);
		st = st + new string("|").parse(s,ps);
		st = st + new spaces().parse(s,ps);
		predicate = new predicate();
		st = st + this.predicate.parse(s,ps);
		st = st + new spaces().parse(s,ps);
		st = st + new string("}").parse(s,ps);

		this.class_type_name = ps.class_type_name;
		
		return st;
	}
	
	//class_Fieldは篩型を持つフィールド、変数を持つクラス
	public void assert_refinement(Check_status cs, Field refined_Field, Expr refined_Expr, Expr class_Expr) throws Exception{
		
		
		
		//バックアップ
	    Field pre_refined_Field = cs.refined_Field;
	    Expr pre_refined_Expr = cs.refined_Expr;
	    String pre_refinement_type_value = cs.refinement_type_value;
	    boolean pre_in_refinement_predicate = cs.in_refinement_predicate;
	    Expr pre_instance_expr = cs.instance_expr;
		String pre_instance_class_name = cs.instance_class_name;
		
		boolean pre_use_only_helper_method = cs.use_only_helper_method;
		boolean pre_ban_private_visibility = cs.ban_private_visibility;
		boolean pre_ban_default_visibility = cs.ban_default_visibility;

	    //篩型の処理のための事前準備
		cs.instance_expr = class_Expr;
		cs.instance_class_name = this.class_type_name;
		
		cs.in_refinement_predicate = true;
		cs.refinement_type_value = ident;
		cs.refined_Field = refined_Field;
		cs.refined_Expr = refined_Expr;
		cs.use_only_helper_method = true;
		
		BoolExpr expr = this.predicate.check(cs);

		//可視性について
		cs.ban_default_visibility = false;
		if(refined_Field.modifiers != null && refined_Field.modifiers.is_private){
			cs.ban_private_visibility = false;
		}else{
			cs.ban_private_visibility = true;
		}
		
		cs.assert_constraint(expr);
		cs.in_refinement_predicate = false;
		if(this.type.type.equals("boolean")){
			if(!(refined_Field.type.equals("boolean"))){
				//System.out.println("this variable is not boolean");
				throw new Exception("this variable is not boolean");
			}
		}else if(this.type.type.equals("int")){
			if(!(refined_Field.type.equals("int"))){
				//System.out.println("this variable is not int");
				throw new Exception("this variable is not int");
			}
		}else if(this.type.type.equals(refined_Field.type)){
			//クラス型
		}else{
			refinement_type rt = cs.search_refinement_type(this.class_type_name, type.type);
			if(rt!=null){
				rt.assert_refinement(cs, refined_Field, refined_Expr, class_Expr);
			}else{
				throw new Exception("can't find refinement type " + type.type);
			}
		}
		
		cs.refined_Expr = pre_refined_Expr;
	    cs.refined_Field = pre_refined_Field;
	    cs.refinement_type_value = pre_refinement_type_value;
	    cs.in_refinement_predicate = pre_in_refinement_predicate;
	    cs.instance_expr = pre_instance_expr;
	    cs.instance_class_name = pre_instance_class_name;
		cs.use_only_helper_method = pre_use_only_helper_method;
		cs.ban_private_visibility = pre_ban_private_visibility;
		cs.ban_default_visibility = pre_ban_default_visibility;
	}
	
	public void add_refinement_constraint(Check_status cs, Field refined_Field, Expr refined_Expr, Expr class_Expr) throws Exception{
		add_refinement_constraint(cs, refined_Field, refined_Expr, class_Expr, false);
	}
	
	//class_Fieldは篩型を持つフィールド、変数を持つクラス
	//add_onceは、一度だけhelperやin_cnstructorの制約を無視して篩型の述語をaddする//つまり、篩型の述語の中に記述されたフィールドの篩型は無視したい時に使う
	public void add_refinement_constraint(Check_status cs, Field refined_Field, Expr refined_Expr, Expr class_Expr, boolean add_once) throws Exception{
		if(!add_once){
			if(cs.in_helper)return;//helperメソッドの中では、フィールドの篩型が成り立つことを前提とできない
			if((cs.in_constructor && !(refined_Field instanceof Variable) && !class_Expr.equals(cs.this_field.get_Expr(cs))))return;//コンストラクタ内では篩型は保証されない
		}
		
		
		//バックアップ
	    Field pre_refined_Field = cs.refined_Field;
	    Expr pre_refined_Expr = cs.refined_Expr;
	    String pre_refinement_type_value = cs.refinement_type_value;
	    boolean pre_in_refinement_predicate = cs.in_refinement_predicate;
	    Expr pre_instance_expr = cs.instance_expr;
		String pre_instance_class_name = cs.instance_class_name;
		
		boolean pre_use_only_helper_method = cs.use_only_helper_method;
		boolean pre_ban_private_visibility = cs.ban_private_visibility;
		boolean pre_ban_default_visibility = cs.ban_default_visibility;

	    //篩型の処理のための事前準備
		cs.instance_expr = class_Expr;
		cs.instance_class_name = this.class_type_name;
		
		cs.in_refinement_predicate = true;
		cs.refinement_type_value = ident;
		cs.refined_Field = refined_Field;
		cs.refined_Expr = refined_Expr;
		cs.use_only_helper_method = true;
		
		
		BoolExpr expr = this.predicate.check(cs);
		
		
		//可視性について
		cs.ban_default_visibility = false;
		if(refined_Field.modifiers != null && refined_Field.modifiers.is_private){
			cs.ban_private_visibility = false;
		}else{
			cs.ban_private_visibility = true;
		}
		
		cs.add_constraint(expr);
		cs.in_refinement_predicate = false;
		if(this.type.type.equals("boolean")){
			if(!(refined_Field.type.equals("boolean"))){
				//System.out.println("this variable is not boolean");
				throw new Exception("this variable is not boolean");
			}
		}else if(this.type.type.equals("int")){
			if(!(refined_Field.type.equals("int"))){
				//System.out.println("this variable is not int");
				throw new Exception("this variable is not int");
			}
		}else if(this.type.type.equals(refined_Field.type)){
			//クラス型
		}else{
			refinement_type rt = cs.search_refinement_type(this.class_type_name, type.type);
			if(rt!=null){
				rt.add_refinement_constraint(cs, refined_Field, refined_Expr, class_Expr, add_once);
			}else{
				throw new Exception("can't find refinement type " + type.type);
			}
		}
		
		cs.refined_Expr = pre_refined_Expr;
	    cs.refined_Field = pre_refined_Field;
	    cs.refinement_type_value = pre_refinement_type_value;
	    cs.in_refinement_predicate = pre_in_refinement_predicate;
	    cs.instance_expr = pre_instance_expr;
	    cs.instance_class_name = pre_instance_class_name;
		cs.use_only_helper_method = pre_use_only_helper_method;
		cs.ban_private_visibility = pre_ban_private_visibility;
		cs.ban_default_visibility = pre_ban_default_visibility;
	}
	
	// subtype <= this
	// addしてしまうので、必要であればcs.solver.push()をしておく必要はある。
	public void check_subtype(Variable refined_variable, Expr class_Expr, refinement_type sub_type, Check_status cs) throws Exception{
		cs.in_refinement_predicate = true;
		
		
		
		sub_type.add_refinement_constraint(cs, refined_variable, refined_variable.get_Expr(cs), class_Expr);
		this.assert_refinement(cs, refined_variable, refined_variable.get_Expr(cs), class_Expr);
		
		
		
		//BoolExpr expr = cs.ctx.mkImplies(sub_expr,this_expr);
		//cs.assert_constraint(expr);
		cs.in_refinement_predicate = false;
	}
	
	//使わないかも
	public Pair<String, Integer> base_type(compilation_unit cu) throws Exception{
		if(this.type.type.equals("boolean") || this.type.type.equals("int") || cu.search_class(this.type.type)!=null){
			return new Pair(this.type.type, this.dims);
		}else{
			
			
			refinement_type rt = cu.search_refinement_type(this.class_type_name, type.type);
			if(rt!=null){
				return rt.base_type(cu);
			}else{
				throw new Exception("can't find base type " + type.type);
			}
		}
	}
	
	
	public boolean have_index_access(Check_status cs){
		boolean have = predicate.have_index_access(cs);
		
		refinement_type rt = cs.search_refinement_type(class_type_name, type.type);
		if(rt!=null){
			return have || rt.have_index_access(cs);
		}else{
			return have;
		}

	}

}

