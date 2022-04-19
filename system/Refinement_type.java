package system;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;

import system.parsers.predicates;

public class Refinement_type {
	String ident, type, type_name;
	system.parsers.predicates predicates;

	
	public Refinement_type(String ident, system.parsers.predicates predicates, String type, String type_name){
		this.ident = ident;
		this.predicates = predicates;
		this.type = type;
		this.type_name = type_name;
	}
	
	public void assert_refinement(Check_status cs, Field refined_Field, Expr refined_Expr) throws Exception{
		cs.in_refinement_predicate = true;
		cs.refinement_type_value = ident;
		cs.refined_Field = refined_Field;
		cs.refined_Expr = refined_Expr;
		BoolExpr expr = this.predicates.check(cs);
		cs.assert_constraint(expr);
		cs.in_refinement_predicate = false;
		if(this.type.equals("boolean")){
			if(!(refined_Field.type.equals("boolean"))){
				System.out.println("this variable is not boolean");
			}
		}else if(this.type.equals("int")){
			if(!(refined_Field.type.equals("int"))){
				System.out.println("this variable is not int");
			}
		}else if(this.type.equals(refined_Field.type)){
			//クラス型
		}else{
			Refinement_type rt = cs.get_refinement_type(type);
			if(rt!=null){
				rt.assert_refinement(cs, refined_Field, refined_Expr);
			}else{
				throw new Exception("cant find refinement type " + type);
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
		if(this.type.equals("boolean")){
			if(!(refined_Field.type.equals("boolean"))){
				System.out.println("this variable is not boolean");
			}
		}else if(this.type.equals("int")){
			if(!(refined_Field.type.equals("int"))){
				System.out.println("this variable is not int");
			}
		}else if(this.type.equals(refined_Field.type)){
			//クラス型
		}else{
			Refinement_type rt = cs.get_refinement_type(type);
			if(rt!=null){
				rt.add_refinement_constraint(cs, refined_Field, refined_Expr);
			}else{
				throw new Exception("cant find refinement type " + type);
			}
		}
	}
}
