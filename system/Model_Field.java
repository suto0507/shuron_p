package system;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;

import system.parsers.class_declaration;
import system.parsers.method_decl;
import system.parsers.modifiers;
import system.parsers.refinement_type;
import system.parsers.refinement_type_clause;
import system.parsers.spec_expression;
import system.parsers.represents_clause;

public class Model_Field extends Field{
	
	spec_expression represents_spec_expression;
	
	//�V�����t�B�[���h����鎞�ɂ́Aalias_in_helper_or_consutructor��alias_2d_in_helper_or_consutructor�͓����������珉��������
	public Model_Field(int id, String field_name, String type, int dims, refinement_type_clause refinement_type_clause, modifiers modifiers, Field class_object, String class_type_name, BoolExpr alias_in_helper_or_consutructor, ArrayList<Model_Field> model_fields) throws Exception{
		this.id = id;
		this.internal_id = id;
		this.temp_num = 0;
		this.field_name = field_name;
		this.type = type;
		this.dims = dims;
		this.refinement_type_clause = refinement_type_clause;
		this.modifiers = modifiers;
		this.class_object = class_object;
		this.assinable_cnst_indexs = new ArrayList<Pair<BoolExpr,List<List<IntExpr>>>>();
		this.class_type_name = class_type_name;
		this.new_array = false;
		this.alias_in_helper_or_consutructor = alias_in_helper_or_consutructor;
		this.alias_2d_in_helper_or_consutructor = alias_in_helper_or_consutructor;
		this.model_fields = model_fields;
	}
	
	public Model_Field clone_e() throws Exception{
		Model_Field ret = new Model_Field(this.internal_id, this.field_name, this.type, this.dims, this.refinement_type_clause, this.modifiers, this.class_object, class_type_name, alias_in_helper_or_consutructor, model_fields);
		ret.temp_num = this.temp_num;
		ret.class_object_expr = this.class_object_expr;
		ret.assinable_cnst_indexs = this.assinable_cnst_indexs;
		ret.index = this.index;
		
		ret.final_initialized = final_initialized;
		ret.alias_2d_in_helper_or_consutructor = this.alias_2d_in_helper_or_consutructor;//�V�����t�B�[���h����鎞�ɂ́Aalias_in_helper_or_consutructor��alias_2d_in_helper_or_consutructor�͓����������珉��������
		
		ret.represents_spec_expression = this.represents_spec_expression;
		return ret;
	}
	
	public void set_repersents(Check_status cs){
		represents_clause rc = cs.Check_status_share.compilation_unit.search_represents_clause(this.class_object.type, this.field_name, cs.this_field.type);
		if(rc != null){
			represents_spec_expression = rc.spec_expression;
		}
	}
	
	public Expr get_Expr(Expr class_expr, ArrayList<IntExpr> indexs, Check_status cs) throws Exception{
		Expr expr = get_Expr(cs);
		if(this.represents_spec_expression!=null){
			Expr full_expr = cs.ctx.mkSelect(expr, class_expr);
			
			Expr pre_instance_expr = cs.instance_expr;
			Field pre_instance_Field = cs.instance_Field;
			ArrayList<IntExpr> pre_instance_indexs = cs.instance_indexs;
			
			cs.instance_expr = class_expr;
			cs.instance_Field = this.class_object;
			cs.instance_indexs = (ArrayList<IntExpr>) indexs.clone();
			
			cs.add_constraint(cs.ctx.mkEq(full_expr, this.represents_spec_expression.check(cs).expr));
			
			cs.instance_expr = pre_instance_expr;
			cs.instance_Field = pre_instance_Field;
			cs.instance_indexs = pre_instance_indexs;
		}
		
		return expr;
	}
	
	//class_Field��⿌^�����t�B�[���h�A�ϐ������N���X
	//add_once�́A��x����helper��in_cnstructor�̐���𖳎�����⿌^�̏q���add����//�܂�A⿌^�̏q��̒��ɋL�q���ꂽ�t�B�[���h��⿌^�͖������������Ɏg��
	public void add_refinement_constraint(Check_status cs, Expr class_Expr, ArrayList<IntExpr> indexs, boolean add_once) throws Exception{
		
		Expr ex = cs.ctx.mkSelect(get_Expr(class_Expr, indexs, cs), class_Expr);//������get_Expr��represents�̐����ǉ�����
		
		if(this.refinement_type_clause.refinement_type!=null){
			this.refinement_type_clause.refinement_type.add_refinement_constraint(cs, this, ex, this.class_object, class_Expr, indexs, add_once);
		}else if(this.refinement_type_clause.ident!=null){
			refinement_type rt = cs.search_refinement_type(this.class_object.type, this.refinement_type_clause.ident);
			if(rt!=null){
				rt.add_refinement_constraint(cs, this, ex, this.class_object, class_Expr, indexs, add_once);
			}else{
                throw new Exception("can't find refinement type " + this.refinement_type_clause.ident);
            }
		}
		
	}
	
	public void assert_refinement(Check_status cs, Expr class_Expr, ArrayList<IntExpr> indexs) throws Exception{
		
		Expr ex = cs.ctx.mkSelect(get_Expr(class_Expr, indexs, cs), class_Expr);//������get_Expr��represents�̐����ǉ�����
		
		if(this.refinement_type_clause.refinement_type!=null){
			this.refinement_type_clause.refinement_type.assert_refinement(cs, this, ex, this.class_object, class_Expr, indexs);
		}else if(this.refinement_type_clause.ident!=null){
			refinement_type rt = cs.search_refinement_type(this.class_object.type, this.refinement_type_clause.ident);
			if(rt!=null){
				rt.assert_refinement(cs, this, ex, this.class_object, class_Expr, indexs);
			}else{
                throw new Exception("can't find refinement type " + this.refinement_type_clause.ident);
            }
		}
		
	}
	
	
}
