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
	predicate predicate;
	public String class_type_name;
	
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
		predicate = new predicate();
		st = st + this.predicate.parse(s,ps);
		st = st + new spaces().parse(s,ps);
		st = st + new string("}").parse(s,ps);

		this.class_type_name = ps.class_type_name;
		
		return st;
	}
	
	//class_Field��⿌^�����t�B�[���h�A�ϐ������N���X
	public void assert_refinement(Check_status cs, Field refined_Field, Expr refined_Expr, Field class_Field, Expr class_Expr) throws Exception{
		
		//�o�b�N�A�b�v
	    Field pre_refined_Field = cs.refined_Field;
	    Expr pre_refined_Expr = cs.refined_Expr;
	    String pre_refinement_type_value = cs.refinement_type_value;
	    boolean pre_in_refinement_predicate = cs.in_refinement_predicate;
	    Expr pre_refined_class_Expr = cs.refined_class_Expr;
	    Field pre_refined_class_Field = cs.refined_class_Field;
	    
	    //⿌^�̏����̂��߂̎��O����
        cs.refined_class_Expr = class_Expr;
        cs.refined_class_Field = class_Field;
		
		cs.in_refinement_predicate = true;
		cs.refinement_type_value = ident;
		cs.refined_Field = refined_Field;
		cs.refined_Expr = refined_Expr;
		
		String pre_class_type_name = cs.refined_class_Field.type;
		cs.refined_class_Field.type = this.class_type_name;
		
		BoolExpr expr = this.predicate.check(cs);
		
		cs.refined_class_Field.type = pre_class_type_name;
		
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
			//�N���X�^
		}else{
			refinement_type rt = cs.search_refinement_type(refined_Field.class_object.type, type.type);
			if(rt!=null){
				rt.assert_refinement(cs, refined_Field, refined_Expr, class_Field, class_Expr);
			}else{
				throw new Exception("can't find refinement type " + type.type);
			}
		}
		
		cs.refined_Expr = pre_refined_Expr;
	    cs.refined_Field = pre_refined_Field;
	    cs.refinement_type_value = pre_refinement_type_value;
	    cs.in_refinement_predicate = pre_in_refinement_predicate;
	    cs.refined_class_Expr = pre_refined_class_Expr;
	    cs.refined_class_Field = pre_refined_class_Field;
	}
	
	
	//class_Field��⿌^�����t�B�[���h�A�ϐ������N���X
	public void add_refinement_constraint(Check_status cs, Field refined_Field, Expr refined_Expr, Field class_Field, Expr class_Expr) throws Exception{
		
		if(cs.in_constructor){//�R���X�g���N�^���ł�⿌^�͕ۏ؂���Ȃ�
			return;
		}
		
		//�o�b�N�A�b�v
	    Field pre_refined_Field = cs.refined_Field;
	    Expr pre_refined_Expr = cs.refined_Expr;
	    String pre_refinement_type_value = cs.refinement_type_value;
	    boolean pre_in_refinement_predicate = cs.in_refinement_predicate;
	    Expr pre_refined_class_Expr = cs.refined_class_Expr;
	    Field pre_refined_class_Field = cs.refined_class_Field;
	    
	    //⿌^�̏����̂��߂̎��O����
        cs.refined_class_Expr = class_Expr;
        cs.refined_class_Field = class_Field;
		
		cs.in_refinement_predicate = true;
		cs.refinement_type_value = ident;
		cs.refined_Field = refined_Field;
		cs.refined_Expr = refined_Expr;
		
		String pre_class_type_name = cs.refined_class_Field.type;
		cs.refined_class_Field.type = this.class_type_name;
		
		BoolExpr expr = this.predicate.check(cs);
		
		cs.refined_class_Field.type = pre_class_type_name;
		
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
			//�N���X�^
		}else{
			refinement_type rt = cs.search_refinement_type(refined_Field.class_object.type, type.type);
			if(rt!=null){
				rt.add_refinement_constraint(cs, refined_Field, refined_Expr, class_Field, class_Expr);
			}else{
				throw new Exception("can't find refinement type " + type.type);
			}
		}
		
		cs.refined_Expr = pre_refined_Expr;
	    cs.refined_Field = pre_refined_Field;
	    cs.refinement_type_value = pre_refinement_type_value;
	    cs.in_refinement_predicate = pre_in_refinement_predicate;
	    cs.refined_class_Expr = pre_refined_class_Expr;
	    cs.refined_class_Field = pre_refined_class_Field;
	}
	
	// subtype <= this
	// add���Ă��܂��̂ŁA�K�v�ł����cs.solver.push()�����Ă����K�v�͂���B
	public void check_subtype(Variable refined_variable,  refinement_type sub_type, Field class_Field, Expr class_Expr, Check_status cs) throws Exception{
		cs.in_refinement_predicate = true;
		
		
		
		sub_type.add_refinement_constraint(cs, refined_variable, refined_variable.get_Expr(cs), class_Field , class_Expr);
		this.assert_refinement(cs, refined_variable, refined_variable.get_Expr(cs), class_Field , class_Expr);
		
		
		
		//BoolExpr expr = cs.ctx.mkImplies(sub_expr,this_expr);
		//cs.assert_constraint(expr);
		cs.in_refinement_predicate = false;
	}
	
	//�g��Ȃ�����
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

