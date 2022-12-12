package system;


import com.microsoft.z3.*;

import system.parsers.refinement_type_clause;
import system.parsers.type;
import system.parsers.modifiers;

import java.util.ArrayList;
import java.util.List;

public class Variable extends Field{
	/*
	
	Field class_object; //Variable�ł́A⿌^�̃X�R�[�v�֘A�Ŏg�����肷��
	*/
	
	
		
	public BoolExpr alias;
	public BoolExpr alias_refined;
	public boolean out_loop_v;//���[�v�̊O�Œ�`���ꂽ�ϐ�
	public boolean is_arg;//���ؒ��̃��\�b�h�̈���
	
	public Field arg_field;//���̕ϐ������\�b�h�Ăяo���̈����ł���Ƃ��A�����Ƃ��ēn���ꂽField
	
	
	//�V�����t�B�[���h����鎞�ɂ́Aalias_in_helper_or_consutructor��alias_2d_in_helper_or_consutructor�͓����������珉��������
	public Variable(int id, String field_name, String type, int dims, refinement_type_clause refinement_type_clause, modifiers modifiers, String class_type_name, ArrayList<Pair<Expr, BoolExpr>> alias_in_helper_or_consutructor) throws Exception{
		this.id = id;
		this.internal_id = id;
		this.temp_num = - 1;
		this.field_name = field_name;
		this.type = type;
		this.dims = dims;
		this.refinement_type_clause = refinement_type_clause;
		this.modifiers = modifiers;
		this.new_array = false;
		this.class_type_name = class_type_name;
		this.alias_in_helper_or_consutructor = alias_in_helper_or_consutructor;
		this.alias_2d_in_helper_or_consutructor = alias_in_helper_or_consutructor;
		
		this.model_fields = new ArrayList<Model_Field>();
	}
	
	public Variable(){}
	
	@Override
	public Variable clone_e() throws Exception{
		Variable ret = new  Variable(this.internal_id, this.field_name, this.type, this.dims, this.refinement_type_clause, this.modifiers, this.class_type_name, this.alias_in_helper_or_consutructor);
		ret.temp_num = this.temp_num;
		ret.alias = this.alias;
		ret.alias_refined = this.alias_refined;
		ret.out_loop_v = this.out_loop_v;
		ret.alias_2d_in_helper_or_consutructor = this.alias_2d_in_helper_or_consutructor;//�V�����t�B�[���h����鎞�ɂ́Aalias_in_helper_or_consutructor��alias_2d_in_helper_or_consutructor�͓����������珉��������
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
			//�N���X
			String ret = field_name + "_temp_" + this.id + "_"  + this.temp_num;
			return cs.ctx.mkConst(ret, cs.ctx.mkUninterpretedSort("Ref"));
		}else if(this.dims>0){ //�z��
			String ret = field_name + "_temp_" + this.id + "_" + this.temp_num;
			return cs.ctx.mkConst(ret, cs.ctx.mkUninterpretedSort("ArrayRef"));
		}
		throw new Exception("unexpect variable");
	}
	
	
	//�l���X�V����
	//�z��̗v�f�ւ̑���łȂ��Ȃ�΁Atmp_num���X�V����
	public void assign_value(Expr class_expr, ArrayList<IntExpr> indexs, Expr value, Check_status cs) throws Exception{
		if(indexs.size()>0){
			Expr expr = this.get_Expr(cs);
			for(int i = 0; i < indexs.size()-1; i++){
				expr = cs.array_arrayref.index_access_array(expr, indexs.get(i), cs);
			}
			Array array = null;
			if(indexs.size()<this.dims){
				array = cs.array_arrayref;
			}else{
				if(this.type.equals("int")){
					array = cs.array_int;
				}else if(this.type.equals("boolean")){
					array = cs.array_boolean;
				}else{
					array = cs.array_ref;
				}
			}
			array.update_array(expr, indexs.get(indexs.size()-1), value, cs);
			
			//�z��ւ̑���ł��Amodel�t�B�[���h�͕ς��
			for(Model_Field mf : this.model_fields){
				mf.tmp_plus_with_data_group(class_expr, cs);
			}
		}else{
			cs.add_constraint(cs.ctx.mkEq(this.get_Expr_assign(cs), value));
			this.tmp_plus_with_data_group(class_expr, cs);
		}
	}
	
	
	
	@Override
	public Expr get_Expr_assign(Check_status cs) throws Exception{
		this.temp_num ++;
		Expr ex =  this.get_Expr(cs);
		this.temp_num --;
		return ex;
	}
	
}
