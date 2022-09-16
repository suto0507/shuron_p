package system.parsers;

import java.util.ArrayList;

import com.microsoft.z3.IntExpr;

import system.Check_status;
import system.Parser;
import system.Parser_status;
import system.Source;
import system.Variable;

public class param_declaration implements Parser<String>{
	boolean is_final;
	type_spec type_spec;
	String ident;
	
	param_declaration(){
		this.is_final = false;
	}
	
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		Source s_backup = s.clone();
		try{
			st = st + new string("final").parse(s, ps);
			st = st + new spaces().parse(s, ps);
			this.is_final = true;
		}catch (Exception e){
			s.revert(s_backup);
		}
		type_spec ts = new type_spec();
		st = st + ts.parse(s, ps);
		this.type_spec = ts;
		st = st + new spaces().parse(s, ps);
		this.ident = new ident().parse(s, ps);
		st = st + this.ident;
		
		return st;
	}
	
	//����̓��\�b�h�Ăяo���̎��͎g���Ȃ�
	public void check(Check_status cs) throws Exception{
		if(cs.search_variable(this.ident)==false){
			modifiers modi = new modifiers();
			modi.is_final = this.is_final;
			modi.is_privte = false;
			modi.is_spec_public = false;
			Variable v = cs.add_variable(this.ident, this.type_spec.type.type, this.type_spec.dims, this.type_spec.refinement_type_clause, modi);
			v.temp_num=0;
			if(v.refinement_type_clause!=null){
				if(v.refinement_type_clause.refinement_type!=null){
					v.refinement_type_clause.refinement_type.add_refinement_constraint(cs, v, v.get_Expr(cs), cs.this_field, cs.this_field.get_Expr(cs), new ArrayList<IntExpr>());
				}else{
					refinement_type rt = cs.search_refinement_type(v.class_object.type, v.refinement_type_clause.ident);
					if(rt!=null){
						rt.add_refinement_constraint(cs, v, v.get_Expr(cs), cs.this_field, cs.this_field.get_Expr(cs), new ArrayList<IntExpr>());
					}else{
						throw new Exception("cant find refinement type " + v.refinement_type_clause.ident);
					}
				}
			}
		}else{
			//System.out.println("this name is used");
			throw new Exception("this name is used");
		}
	}

}
