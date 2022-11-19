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
	
	//メソッドの検証時の引数
	//これはメソッド呼び出しの時は使われない
	public void check(Check_status cs) throws Exception{
		if(cs.search_variable(this.ident)==false){
			modifiers modi = new modifiers();
			modi.is_final = this.is_final;
			modi.is_privte = false;
			modi.is_spec_public = false;
			Variable v = cs.add_variable(this.ident, this.type_spec.type.type, this.type_spec.dims, this.type_spec.refinement_type_clause, modi, cs.ctx.mkBool(true));
			v.temp_num=0;
			if(v.hava_refinement_type()){
				v.add_refinement_constraint(cs, cs.this_field.get_Expr(cs), new ArrayList<IntExpr>(), true);
			}
			
			v.alias = cs.ctx.mkBool(true); //引数はエイリアスしている可能性がある。
		}else{
			//System.out.println("this name is used");
			throw new Exception("this name is used");
		}
	}

}
