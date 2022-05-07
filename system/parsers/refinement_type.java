package system.parsers;


import com.microsoft.z3.*;

import system.Check_status;
import system.Field;
import system.Parser;
import system.Parser_status;
import system.Source;
import system.Variable;

public class refinement_type implements Parser<String>{
	type type;
	String ident;
	predicates predicates;
	
	public String parse(Source s,Parser_status ps)throws Exception{
		String st;
		st = new string("{").parse(s,ps);
		st = st + new spaces().parse(s,ps);
		this.type = new type();
		st = st + type.parse(s,ps);
		st = st + new spaces().parse(s,ps);
		this.ident = new ident().parse(s,ps);
		st = st + this.ident;
		st = st + new spaces().parse(s,ps);
		st = st + new string("|").parse(s,ps);
		st = st + new spaces().parse(s,ps);
		predicates = new predicates();
		st = st + this.predicates.parse(s,ps);
		st = st + new spaces().parse(s,ps);
		st = st + new string("}").parse(s,ps);
		
		return st;
	}
	
	public void assert_refinement(Check_status cs, Field refined_Field, Expr refined_Expr) throws Exception{
		cs.in_refinement_predicate = true;
		cs.refinement_type_value = ident;
		cs.refined_Field = refined_Field;
		cs.refined_Expr = refined_Expr;
		BoolExpr expr = this.predicates.check(cs);
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
			refinement_type rt = cs.search_refinement_type(refined_Field.class_object.type, type.type);
			if(rt!=null){
				rt.assert_refinement(cs, refined_Field, refined_Expr);
			}else{
				throw new Exception("can't find refinement type " + type.type);
			}
		}
	}
	
	public void add_refinement_constraint(Check_status cs, Field refined_Field, Expr refined_Expr) throws Exception{
		
		if(cs.in_constructor){//コンストラクタ内では篩型は保証されない
			return;
		}
		
		cs.in_refinement_predicate = true;
		cs.refinement_type_value = ident;
		cs.refined_Field = refined_Field;
		cs.refined_Expr = refined_Expr;
		BoolExpr expr = this.predicates.check(cs);
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
			refinement_type rt = cs.search_refinement_type(refined_Field.class_object.type, type.type);
			if(rt!=null){
				rt.add_refinement_constraint(cs, refined_Field, refined_Expr);
			}else{
				throw new Exception("can't find refinement type " + type.type);
			}
		}
	}
	
	// subtype <= this
	public void check_subtype(Variable refined_variable,  refinement_type sub_type, Check_status cs) throws Exception{
		cs.in_refinement_predicate = true;
		
		sub_type.add_refinement_constraint(cs, refined_variable, refined_variable.get_Expr(cs));
		this.assert_refinement(cs, refined_variable, refined_variable.get_Expr(cs));
		
		
		
		//BoolExpr expr = cs.ctx.mkImplies(sub_expr,this_expr);
		//cs.assert_constraint(expr);
		cs.in_refinement_predicate = false;
	}
	
	//使わないかも
	public String base_type(String class_name, compilation_unit cu) throws Exception{
		if(this.type.type.equals("boolean") || this.type.type.equals("int")){
			return this.type.type;
		}else if(cu.search_class(this.type.type)!=null){
			return this.type.type;
		}else{
			
			
			refinement_type rt = cu.search_refinement_type(class_name, type.type);
			if(rt!=null){
				return rt.base_type(class_name, cu);
			}else{
				throw new Exception("can't find base type " + type.type);
			}
		}
	}

}

