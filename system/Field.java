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
	
	public Field class_object;
	public Expr class_object_expr;
	
	public BoolExpr assinable_cnst;//フィールドに代入できる条件
	public List<Pair<BoolExpr,List<List<IntExpr>>>> assinable_cnst_indexs;//配列の要素に代入できる条件
	
	public List<IntExpr> index; //代入するときの右辺値のindexを表してっるっぽい？メソッドには使えないので要確認
	
	//配列のlength
	public List<Pair<List<IntExpr>, IntExpr>> length;
	
	//コンストラクタのfinalの初期化状態
	public boolean final_initialized;
	
	//宣言されたクラスの名前
	public String class_type_name;
	
	
	public Field(int id, String field_name, String type, int dims, refinement_type_clause refinement_type_clause, modifiers modifiers, Field class_object, String class_type_name) throws Exception{
		this.id = id;
		this.temp_num = 0;
		this.field_name = field_name;
		this.type = type;
		this.dims = dims;
		this.refinement_type_clause = refinement_type_clause;
		this.modifiers = modifiers;
		this.class_object = class_object;
		if(this.dims>0 && this.refinement_type_clause!=null){
			throw new Exception("Cannot use refinement type for array");
		}
		this.assinable_cnst_indexs = new ArrayList<Pair<BoolExpr,List<List<IntExpr>>>>();
		this.class_type_name = class_type_name;
	}
	
	public Field(){}
	
	
	public Field clone_e() throws Exception{
		Field ret = new Field(this.id, this.field_name, this.type, this.dims, this.refinement_type_clause, this.modifiers, this.class_object, class_type_name);
		ret.temp_num = this.temp_num;
		ret.class_object_expr = this.class_object_expr;
		ret.assinable_cnst = this.assinable_cnst;
		ret.assinable_cnst_indexs = this.assinable_cnst_indexs;
		ret.index = this.index;
		ret.length = this.length;
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
	
	//メソッド呼び出しの時に使う。indexsは、a[x].array[2]だったら{x,2}になるようなもの
	//indexsは変更されるので、必要であればcloneしたものを渡す
	public Expr get_full_Expr(ArrayList<IntExpr> indexs, Check_status cs) throws Exception{
		Expr ex = this.get_Expr(cs);
		Expr class_ex = this.class_object.get_full_Expr(indexs, cs);
		Expr full_ex;
		full_ex = cs.ctx.mkSelect((ArrayExpr)ex, class_ex);
		for(int i=0; i<this.dims&&indexs.size()>0; i++){
			full_ex = cs.ctx.mkSelect((ArrayExpr) full_ex, indexs.get(0));
			indexs.remove(0);
		}
		return full_ex;
	}
	
	public Expr get_full_Expr_assign(ArrayList<IntExpr> indexs, Check_status cs) throws Exception{
		Expr ex = this.get_Expr_assign(cs);
		Expr class_ex = this.class_object.get_full_Expr(indexs, cs);
		Expr full_ex;
		full_ex = cs.ctx.mkSelect((ArrayExpr)ex, class_ex);
		for(int i=0; i<this.dims&&indexs.size()>0; i++){
			full_ex = cs.ctx.mkSelect((ArrayExpr) full_ex, indexs.get(0));
			indexs.remove(0);
		}
		return full_ex;
	}
	
	public int class_object_dims_sum(){
		return this.class_object.dims_sum();
	}
	
	public int dims_sum(){
		return this.dims + this.class_object.dims_sum();
	}
	
	//自分と同じ型のフレッシュなExprを返す
	public Expr get_Expr_tmp(Check_status cs) throws Exception{
		if(this.type.equals("int")&&this.dims==0){
			String ret = "tmpInt" + cs.Check_status_share.tmp_num;
			return cs.ctx.mkIntConst(ret);
		}else if(this.type.equals("boolean")&&this.dims==0){
			String ret = "tmpBool" + cs.Check_status_share.tmp_num;
			return cs.ctx.mkBoolConst(ret);
		}else if(this.type.equals("void")){
			//throw new Exception("void variable ?");
			return null;
		}else if(this.dims==0){
			//クラス
			String ret = "tmpRef" + cs.Check_status_share.tmp_num;;
			return cs.ctx.mkConst(ret, cs.ctx.mkUninterpretedSort("Ref"));
		}else if(this.type.equals("int")&&this.dims==1){ //配列
			String ret = "tmpIntArray" + cs.Check_status_share.tmp_num;
			return cs.ctx.mkArrayConst(ret, cs.ctx.mkIntSort(), cs.ctx.mkIntSort());
		}else if(this.type.equals("boolean")&&this.dims==1){
			String ret = "tmpBoolArray" + cs.Check_status_share.tmp_num;
			return cs.ctx.mkArrayConst(ret, cs.ctx.mkIntSort(), cs.ctx.mkBoolSort());
		}else if(this.dims==1){
			String ret = "tmpRefArray" + cs.Check_status_share.tmp_num;;
			return cs.ctx.mkArrayConst(ret, cs.ctx.mkIntSort(), cs.ctx.mkUninterpretedSort("Ref"));
		}
		throw new Exception("unexpect variable");
	}
	
	
	public boolean equals(Field f, Check_status cs){
		if(this.field_name.equals(f.field_name) && ((this.class_object==null&&f.class_object==null) || this.class_object.equals(f.class_object, cs) ) ){
			return true;
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
	
	
	public BoolExpr assign_index_expr(List<IntExpr> index_expr, Check_status cs){
		BoolExpr equal_cnsts = cs.ctx.mkBool(false);
		BoolExpr not_equal_cnsts = cs.ctx.mkBool(true);
		for(Pair<BoolExpr,List<List<IntExpr>>> assinable_cnst_index :assinable_cnst_indexs){
			BoolExpr equal = cs.ctx.mkBool(false);
			for(List<IntExpr> index : assinable_cnst_index.snd){
				if(index.size()>0 && index.size() == index_expr.size()){
					BoolExpr index_equal = null;
					for(int i = 0; i<index.size(); i++){
						if(index_equal == null){
							index_equal = cs.ctx.mkEq(index_expr.get(i), index.get(i));
						}else{
							index_equal = cs.ctx.mkAnd(index_equal, cs.ctx.mkEq(index_expr.get(i), index.get(i)));
						}
					}
					equal = cs.ctx.mkOr(equal, index_equal);
				}
			}
			BoolExpr not_equal = cs.ctx.mkNot(equal);
			equal_cnsts = cs.ctx.mkOr(equal_cnsts, cs.ctx.mkAnd(equal, assinable_cnst_index.fst));
			not_equal_cnsts = cs.ctx.mkAnd(not_equal_cnsts, cs.ctx.mkImplies(not_equal, cs.ctx.mkNot(assinable_cnst_index.fst)));
		}
		
		return cs.ctx.mkAnd(equal_cnsts, not_equal_cnsts);
	}
}
