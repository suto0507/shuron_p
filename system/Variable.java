package system;


import com.microsoft.z3.*;

import system.parsers.refinement_type_clause;
import system.parsers.type;
import system.parsers.modifiers;

import java.util.List;

public class Variable extends Field{
	/*
	int id;
	int temp_num;
	String field_name;
	String type;
	int dims;
	refinement_type_clause refinement_type_clause;
	soturon.Parsers.modifiers modifiers;
	Expr assign_Expr;
	
	
	Field class_object; //Variableでは、篩型のスコープ関連で使ったりする
	*/
	
	public Variable(int id, String field_name, String type, int dims, refinement_type_clause refinement_type_clause, modifiers modifiers, Field class_object) throws Exception{
		this.id = id;
		this.temp_num = - 1;
		this.field_name = field_name;
		this.type = type;
		this.dims = dims;
		this.refinement_type_clause = refinement_type_clause;
		this.modifiers = modifiers;
		this.class_object = class_object;
		if(this.dims>0 && this.refinement_type_clause!=null){
			throw new Exception("Cannot use refinement type for array");
		}
	}
	
	@Override
	public Variable clone_e() throws Exception{
		Variable ret = new  Variable(this.id, this.field_name, this.type, this.dims, this.refinement_type_clause, this.modifiers, this.class_object);
		ret.temp_num = this.temp_num;
		return ret;
	}
	
	@Override
	public Expr get_Expr(Check_status cs) throws Exception{
		if(this.temp_num==-1)throw new Exception("this variable " + this.field_name +  " did not initialized");
		if(this.type.equals("int")&&this.dims==0){
			String ret = field_name + "_temp_" + this.id + "_" + this.temp_num;
			return cs.ctx.mkIntConst(ret);
			
			
		}else if(this.type.equals("boolean")&&this.dims==0){
			String ret = field_name + "_temp_" + this.id + "_"  + this.temp_num;
			return cs.ctx.mkBoolConst(ret);
		}else if(this.type.equals("void")){
			//throw new Exception("void variable ?");
			return null;
		}else if(this.dims==0){
			//クラス
			String ret = field_name + "_temp_" + this.id + "_"  + this.temp_num;
			return cs.ctx.mkConst(ret, cs.ctx.mkUninterpretedSort("Ref"));
		}else if(this.type.equals("int")&&this.dims==1){ //配列
			String ret = field_name + "_temp_" + this.id + "_" + this.temp_num;
			return cs.ctx.mkArrayConst(ret, cs.ctx.mkIntSort(), cs.ctx.mkIntSort());
		}else if(this.type.equals("boolean")&&this.dims==1){
			String ret = field_name + "_temp_" + this.id + "_"  + this.temp_num;
			return cs.ctx.mkArrayConst(ret, cs.ctx.mkIntSort(), cs.ctx.mkBoolSort());
		}else if(this.dims==1){
			String ret = field_name + "_temp_" + this.id + "_"  + this.temp_num;
			return cs.ctx.mkArrayConst(ret, cs.ctx.mkIntSort(), cs.ctx.mkUninterpretedSort("Ref"));
		}
		throw new Exception("unexpect variable");
	}
	@Override
	public Expr get_Expr_assign(Check_status cs) throws Exception{
		this.temp_num++;
		Expr ex =  this.get_Expr(cs);
		this.temp_num--;
		return ex;
	}
	
	public boolean is_this_field(){
		if(this.field_name.equals("this")){
			return true;
		}else{
			return false;
		}
	}
	
}
