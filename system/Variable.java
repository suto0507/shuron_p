package system;


import com.microsoft.z3.*;

import system.parsers.refinement_type_clause;
import system.parsers.type;
import system.parsers.modifiers;

import java.util.ArrayList;
import java.util.List;

public class Variable extends Field{
	/*
	
	Field class_object; //Variableでは、篩型のスコープ関連で使ったりする
	*/
	
	
		
	public BoolExpr alias;
	public BoolExpr alias_refined;
	public boolean out_loop_v;//ループの外で定義された変数
	
	public Field arg_field;//この変数がメソッド呼び出しの引数であるとき、引数として渡されたField
	
	
	//新しくフィールドを作る時には、alias_in_helper_or_consutructorとalias_2d_in_helper_or_consutructorは同じ引数から初期化する
	public Variable(int id, String field_name, String type, int dims, refinement_type_clause refinement_type_clause, modifiers modifiers, Field class_object, BoolExpr alias_in_helper_or_consutructor) throws Exception{
		this.id = id;
		this.internal_id = id;
		this.temp_num = - 1;
		this.field_name = field_name;
		this.type = type;
		this.dims = dims;
		this.refinement_type_clause = refinement_type_clause;
		this.modifiers = modifiers;
		this.class_object = class_object;
		this.new_array = false;

		this.alias_in_helper_or_consutructor = alias_in_helper_or_consutructor;
		this.alias_2d_in_helper_or_consutructor = alias_in_helper_or_consutructor;
	}
	
	public Variable(){}
	
	@Override
	public Variable clone_e() throws Exception{
		Variable ret = new  Variable(this.id, this.field_name, this.type, this.dims, this.refinement_type_clause, this.modifiers, this.class_object, this.alias_in_helper_or_consutructor);
		ret.temp_num = this.temp_num;
		ret.alias = this.alias;
		ret.alias_refined = this.alias_refined;
		ret.out_loop_v = this.out_loop_v;
		ret.alias_2d_in_helper_or_consutructor = this.alias_2d_in_helper_or_consutructor;//新しくフィールドを作る時には、alias_in_helper_or_consutructorとalias_2d_in_helper_or_consutructorは同じ引数から初期化する
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
		}else if(this.type.equals("int")&&this.dims>0){ //配列
			String ret = field_name + "_temp_" + this.id + "_" + this.temp_num;
			Sort arraysort = cs.ctx.mkIntSort();
			for(int i = 0; i < this.dims-1; i++){
				arraysort = cs.ctx.mkArraySort(cs.ctx.mkIntSort(), arraysort);
			}
			return cs.ctx.mkArrayConst(ret, cs.ctx.mkIntSort(), arraysort);
		}else if(this.type.equals("boolean")&&this.dims>0){
			String ret = field_name + "_temp_" + this.id + "_"  + this.temp_num;
			Sort arraysort = cs.ctx.mkBoolSort();
			for(int i = 0; i < this.dims-1; i++){
				arraysort = cs.ctx.mkArraySort(cs.ctx.mkIntSort(), arraysort);
			}
			return cs.ctx.mkArrayConst(ret, cs.ctx.mkIntSort(), arraysort);
		}else if(this.dims>0){
			String ret = field_name + "_temp_" + this.id + "_"  + this.temp_num;
			Sort arraysort = cs.ctx.mkUninterpretedSort("Ref");
			for(int i = 0; i < this.dims-1; i++){
				arraysort = cs.ctx.mkArraySort(cs.ctx.mkIntSort(), arraysort);
			}
			return cs.ctx.mkArrayConst(ret, cs.ctx.mkIntSort(), arraysort);
		}
		throw new Exception("unexpect variable");
	}
	
	public Expr get_full_Expr(ArrayList<IntExpr> indexs, Check_status cs) throws Exception{
		Expr ex = this.get_Expr(cs);
		for(int i=0; i<this.dims&&indexs.size()>0; i++){
			ex = cs.ctx.mkSelect((ArrayExpr) ex, indexs.get(0));
			indexs.remove(0);
		}
		return ex;
	}
	
	public Expr get_full_Expr_assign(ArrayList<IntExpr> indexs, Check_status cs) throws Exception{
		Expr ex = this.get_Expr_assign(cs);
		for(int i=0; i<this.dims&&indexs.size()>0; i++){
			ex = cs.ctx.mkSelect((ArrayExpr) ex, indexs.get(0));
			indexs.remove(0);
		}
		return ex;
	}
	
	public int class_object_dims_sum(){
		return 0;
	}
	
	public int dims_sum(){
		return this.dims;
	}
	
	//長さに関する制約も追加する
	public Expr assign_value(ArrayList<IntExpr> indexs, Expr value, Check_status cs) throws Exception{
		Expr expr = value;
		for(int i = indexs.size()-1; i >= 0; i--){
			IntExpr index = indexs.get(i);
			ArrayList<IntExpr> indexs_sub = new ArrayList<IntExpr>(indexs.subList(0, i));
			ArrayExpr array = (ArrayExpr) get_full_Expr((ArrayList<IntExpr>) indexs_sub.clone(), cs);
			ArrayExpr array_assign = (ArrayExpr) get_full_Expr_assign((ArrayList<IntExpr>) indexs_sub.clone(), cs);
			expr = cs.ctx.mkStore(array, index, expr);
			
			//長さに関する制約
			int array_dim = this.dims_sum() - i;
			String array_type;
			if(this.type.equals("int")){
				array_type = "int";
			}else if(this.type.equals("boolean")){
				array_type = "boolean";
			}else{
				array_type = "ref";
			}
			IntExpr length = (IntExpr) cs.ctx.mkSelect(cs.ctx.mkArrayConst("length_" + array_dim + "d_" + array_type, array.getSort(), cs.ctx.mkIntSort()), array);
			IntExpr length_assign = (IntExpr) cs.ctx.mkSelect(cs.ctx.mkArrayConst("length_" + array_dim + "d_" + array_type, array_assign.getSort(), cs.ctx.mkIntSort()), array_assign);
			
			cs.add_constraint(cs.ctx.mkEq(length, length_assign));
		}
		
		
		return expr;
	}
	
	@Override
	public Expr get_Expr_assign(Check_status cs) throws Exception{
		this.temp_num ++;
		Expr ex =  this.get_Expr(cs);
		this.temp_num --;
		return ex;
	}
	
	public boolean is_this_field(){
		if(this.field_name.equals("this")){
			return true;
		}else{
			return false;
		}
	}
	
	//インデックスをフレッシュとして、class_objectのExpr用に作る
	public Pair<Expr, ArrayList<IntExpr>> fresh_index_full_expr(Check_status cs) throws Exception{
		Expr expr = this.get_Expr(cs);
		ArrayList<IntExpr> indexs = new ArrayList<IntExpr>();
		for(int i = 0; i < this.dims; i++){
			String ret = "tmpIndex" + cs.Check_status_share.get_tmp_num();
			IntExpr index = cs.ctx.mkIntConst(ret);
			indexs.add(index);
			expr = cs.ctx.mkSelect(expr, index);
		}
		
		return new Pair(expr, indexs);
	}
	
}
