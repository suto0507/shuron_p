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
	public void assert_refinement(Check_status cs, Field refined_Field, Expr refined_Expr, Field class_Field, Expr class_Expr, ArrayList<IntExpr> indexs) throws Exception{
		
		
		
		//バックアップ
	    Field pre_refined_Field = cs.refined_Field;
	    Expr pre_refined_Expr = cs.refined_Expr;
	    String pre_refinement_type_value = cs.refinement_type_value;
	    boolean pre_in_refinement_predicate = cs.in_refinement_predicate;
	    Expr pre_instance_expr = cs.instance_expr;
		Field pre_instance_Field = cs.instance_Field;
		ArrayList<IntExpr> pre_instance_indexs = cs.instance_indexs;
		boolean pre_use_only_helper_method = cs.use_only_helper_method;

	    //篩型の処理のための事前準備
		cs.instance_expr = class_Expr;
		cs.instance_Field = class_Field;
		cs.instance_indexs = (ArrayList<IntExpr>) indexs.clone();
		
		cs.in_refinement_predicate = true;
		cs.refinement_type_value = ident;
		cs.refined_Field = refined_Field;
		cs.refined_Expr = refined_Expr;
		cs.use_only_helper_method = true;
		
		String pre_class_type_name = cs.instance_Field.type;
		cs.instance_Field.type = this.class_type_name;
		
		BoolExpr expr = this.predicate.check(cs);
		cs.instance_Field.type = pre_class_type_name;
		
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
				rt.assert_refinement(cs, refined_Field, refined_Expr, class_Field, class_Expr, indexs);
			}else{
				throw new Exception("can't find refinement type " + type.type);
			}
		}
		
		cs.refined_Expr = pre_refined_Expr;
	    cs.refined_Field = pre_refined_Field;
	    cs.refinement_type_value = pre_refinement_type_value;
	    cs.in_refinement_predicate = pre_in_refinement_predicate;
	    cs.instance_expr = pre_instance_expr;
		cs.instance_Field = pre_instance_Field;
		cs.instance_indexs = pre_instance_indexs;
		cs.use_only_helper_method = pre_use_only_helper_method;
	}
	
	public void add_refinement_constraint(Check_status cs, Field refined_Field, Expr refined_Expr, Field class_Field, Expr class_Expr, ArrayList<IntExpr> indexs) throws Exception{
		add_refinement_constraint(cs, refined_Field, refined_Expr, class_Field, class_Expr, indexs, false);
	}
	
	//class_Fieldは篩型を持つフィールド、変数を持つクラス
	//add_onceは、一度だけhelperやin_cnstructorの制約を無視して篩型の述語をaddする//つまり、篩型の述語の中に記述されたフィールドの篩型は無視したい時に使う
	public void add_refinement_constraint(Check_status cs, Field refined_Field, Expr refined_Expr, Field class_Field, Expr class_Expr, ArrayList<IntExpr> indexs, boolean add_once) throws Exception{
		if(!add_once){
			if(cs.in_helper)return;//helperメソッドの中では、フィールドの篩型が成り立つことを前提とできない
			if((cs.in_constructor && !(refined_Field instanceof Variable) && refined_Field.class_object != null && refined_Field.class_object.equals(cs.this_field, cs)))return;//コンストラクタ内では篩型は保証されない
		}
		
		
		//バックアップ
	    Field pre_refined_Field = cs.refined_Field;
	    Expr pre_refined_Expr = cs.refined_Expr;
	    String pre_refinement_type_value = cs.refinement_type_value;
	    boolean pre_in_refinement_predicate = cs.in_refinement_predicate;
	    Expr pre_instance_expr = cs.instance_expr;
		Field pre_instance_Field = cs.instance_Field;
		ArrayList<IntExpr> pre_instance_indexs = cs.instance_indexs;

	    //篩型の処理のための事前準備
		cs.instance_expr = class_Expr;
		cs.instance_Field = class_Field;
		cs.instance_indexs = (ArrayList<IntExpr>) indexs.clone();
		
		cs.in_refinement_predicate = true;
		cs.refinement_type_value = ident;
		cs.refined_Field = refined_Field;
		cs.refined_Expr = refined_Expr;
		
		String pre_class_type_name = cs.instance_Field.type;
		cs.instance_Field.type = this.class_type_name;
		
		BoolExpr expr = this.predicate.check(cs);
		
		cs.instance_Field.type = pre_class_type_name;
		
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
				rt.add_refinement_constraint(cs, refined_Field, refined_Expr, class_Field, class_Expr, indexs, add_once);
			}else{
				throw new Exception("can't find refinement type " + type.type);
			}
		}
		
		cs.refined_Expr = pre_refined_Expr;
	    cs.refined_Field = pre_refined_Field;
	    cs.refinement_type_value = pre_refinement_type_value;
	    cs.in_refinement_predicate = pre_in_refinement_predicate;
	    cs.instance_expr = pre_instance_expr;
		cs.instance_Field = pre_instance_Field;
		cs.instance_indexs = pre_instance_indexs;
	}
	
	// subtype <= this
	// addしてしまうので、必要であればcs.solver.push()をしておく必要はある。
	public void check_subtype(Variable refined_variable, Field class_Field, Expr class_Expr, ArrayList<IntExpr> indexs, refinement_type sub_type, Field sub_type_class_Field, Expr sub_type_class_Expr, ArrayList<IntExpr> sub_type_indexs, Check_status cs) throws Exception{
		cs.in_refinement_predicate = true;
		
		
		
		sub_type.add_refinement_constraint(cs, refined_variable, refined_variable.get_Expr(cs), class_Field , class_Expr, indexs);
		this.assert_refinement(cs, refined_variable, refined_variable.get_Expr(cs), class_Field , class_Expr, indexs);
		
		
		
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

