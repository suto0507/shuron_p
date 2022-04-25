package system;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.*;

import system.parsers.refinement_type_clause;
import system.parsers.modifiers;
import system.parsers.refinement_type;

public class Field {
	public int id;
	public int temp_num;
	public String field_name;
	public String type;
	public int dims;
	public refinement_type_clause refinement_type_clause;
	public modifiers modifiers;
	public Expr assign_Expr;//postfix_exprのcheck_assignで設定、assignの方でしか使わなくなるかも
	
	public Field class_object;
	public IntExpr class_object_index;
	public Expr class_object_expr;
	public List<IntExpr> assinable_indexs;
	
	public IntExpr index;
	public Expr assign_now_array_Expr;
	
	//配列のlength
	public IntExpr length;
	
	//コンストラクタのfinalの初期化状態
	public boolean final_initialized;
	
	
	public Field(int id,String field_name, String type, int dims, refinement_type_clause refinement_type_clause, modifiers modifiers, Field class_object, IntExpr class_object_index) throws Exception{
		this.id = id;
		this.temp_num = 0;
		this.field_name = field_name;
		this.type = type;
		this.dims = dims;
		this.refinement_type_clause = refinement_type_clause;
		this.modifiers = modifiers;
		this.class_object = class_object;
		this.class_object_index = class_object_index;
		if(this.dims>0 && this.refinement_type_clause!=null){
			throw new Exception("Cannot use refinement type for array");
		}
		this.assinable_indexs = new ArrayList<IntExpr>();
	}
	
	public Field(){}
	
	
	public Field clone_e() throws Exception{
		Field ret = new  Field(this.id, this.field_name, this.type, this.dims, this.refinement_type_clause, this.modifiers, this.class_object, this.class_object_index);
		ret.temp_num = this.temp_num;
		ret.class_object_index = this.class_object_index;
		ret.assinable_indexs = this.assinable_indexs;
		ret.final_initialized = final_initialized;
		return ret;
	}
	
	
	public Expr get_Expr(Check_status cs) throws Exception{
		if(this.type.equals("int")&&this.dims==0){
			String ret = field_name + "_temp_" + this.id + "_"  + this.temp_num;
			return cs.ctx.mkArrayConst(ret, cs.ctx.mkUninterpretedSort("Ref"), cs.ctx.mkIntSort());
		}else if(this.type.equals("boolean")&&this.dims==0){
			String ret = field_name + "_temp_" + this.id + "_"  + this.temp_num;
			return cs.ctx.mkArrayConst(ret, cs.ctx.mkUninterpretedSort("Ref"), cs.ctx.mkBoolSort());
		}else if(this.type.equals("void")){
			//throw new Exception("void variable ?");
			return null;
		}else if(this.dims==0){
			//クラス
			String ret = field_name + "_temp_" + this.id + "_"  + this.temp_num;
			return cs.ctx.mkArrayConst(ret, cs.ctx.mkUninterpretedSort("Ref"), cs.ctx.mkUninterpretedSort("Ref"));
		}else if(this.type.equals("int")&&this.dims==1){ //配列
			String ret = field_name + "_temp_" + this.id + "_" + this.temp_num;
			return cs.ctx.mkArrayConst(ret, cs.ctx.mkUninterpretedSort("Ref"), cs.ctx.mkArraySort(cs.ctx.mkIntSort(), cs.ctx.mkIntSort()));
		}else if(this.type.equals("boolean")&&this.dims==1){
			String ret = field_name + "_temp_" + this.id + "_"  + this.temp_num;
			return cs.ctx.mkArrayConst(ret, cs.ctx.mkUninterpretedSort("Ref"), cs.ctx.mkArraySort(cs.ctx.mkIntSort(), cs.ctx.mkBoolSort()));
		}else if(this.dims==1){
			String ret = field_name + "_temp_" + this.id + "_"  + this.temp_num;
			return cs.ctx.mkArrayConst(ret, cs.ctx.mkUninterpretedSort("Ref"), cs.ctx.mkArraySort(cs.ctx.mkIntSort(), cs.ctx.mkUninterpretedSort("Ref")));
		}
		throw new Exception("unexpect variable");
	}
	
	public Expr get_Expr_assign(Check_status cs) throws Exception{
		this.temp_num++;
		Expr ex =  this.get_Expr(cs);
		this.temp_num--;
		return ex;
	}
	
	
	public boolean equals(Field f, Check_status cs){
		if(this.field_name.equals(f.field_name) && ((this.class_object==null&&f.class_object==null) || this.class_object.equals(f.class_object, cs) ) ){
			if(f.class_object_index==null && this.class_object_index==null){
				return true;
			}else if(f.class_object_index==null || this.class_object_index==null){
				return false;
			}
			
			
			BoolExpr expr;
			BoolExpr arg_expr = cs.ctx.mkEq(f.class_object_index, this.class_object_index);
			if(cs.pathcondition!=null){
				expr = cs.ctx.mkAnd(cs.pathcondition, cs.ctx.mkNot(arg_expr));
			}else{
				expr = cs.ctx.mkNot(arg_expr);
			}
			
			cs.solver.push();
			cs.solver.add(expr);
	        if(cs.solver.check() != Status.SATISFIABLE) {
	        	cs.solver.pop();
	        	return true;
	        }
			cs.solver.pop();
			
		}
		
		return false;
	}
	
	public void tmp_plus(Check_status cs) throws Exception{
		if(this.modifiers!=null && this.modifiers.is_final){
			return;
		}
		this.temp_num++;
		//refinement_type
		Field f = this;
		if(f.refinement_type_clause!=null){
			if(f.refinement_type_clause.refinement_type!=null){
				f.refinement_type_clause.refinement_type.add_refinement_constraint(cs, f, f.get_Expr(cs));
			}else{
				refinement_type rt = cs.search_refinement_type(f.class_object.type, f.refinement_type_clause.ident);
				if(rt!=null){
					rt.add_refinement_constraint(cs, f, f.get_Expr(cs));
				}
			}
		}
	}
	
	public boolean is_this_field(){
		return this.class_object.is_this_field();
	}
}
