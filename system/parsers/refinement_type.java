package system.parsers;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;

import system.Check_status;
import system.Field;
import system.Parser;
import system.Parser_status;
import system.Refinement_type;
import system.Source;

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
			Refinement_type rt = cs.get_refinement_type(type.type);
			if(rt!=null){
				rt.assert_refinement(cs, refined_Field, refined_Expr);
			}else{
				throw new Exception("cant find refinement type " + type.type);
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
			Refinement_type rt = cs.get_refinement_type(type.type);
			if(rt!=null){
				rt.add_refinement_constraint(cs, refined_Field, refined_Expr);
			}else{
				throw new Exception("cant find refinement type " + type.type);
			}
		}
	}

}

