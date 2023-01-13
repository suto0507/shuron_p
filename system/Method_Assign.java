package system;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.Sort;

//���\�b�h�ł̑���Ɋւ���
//�����r��
public class Method_Assign {
	
	public BoolExpr all_assign_condition;//���̎��O�����͏�����Ă�����
	public ArrayList<Case_Assign> case_assigns;
	public ArrayList<Field> fields;//��������t�B�[���h�@�z��̗v�f�ւ̑���͊܂܂Ȃ�
	
	
	public Method_Assign(BoolExpr all_assign_condition, ArrayList<Case_Assign> case_assigns){
		this.all_assign_condition = all_assign_condition;
		this.case_assigns = case_assigns;
		
		fields = new ArrayList<Field>();
		for(Case_Assign ca : case_assigns){
			for(Check_return cr : ca.field_assigns){
				if(!fields.contains(cr.field)){
					fields.add(cr.field);
				}
			}
		}
	}
	
	//�z��Ɋւ���
	BoolExpr assign_condition(Expr array_ref, IntExpr index, Check_status cs) throws Exception{
		BoolExpr cannot_assign = cs.ctx.mkFalse();
		for(Case_Assign ca : case_assigns){
			cannot_assign = cs.ctx.mkOr(cannot_assign, ca.cannot_assign_condition(array_ref, index, cs));
		}
		
		
		BoolExpr can_assign = cs.ctx.mkOr(cs.ctx.mkNot(cannot_assign), all_assign_condition);
		return can_assign;
	}
	
	//�t�B�[���h�Ɋւ���
	BoolExpr assign_condition(Field f, Expr class_expr, Check_status cs){
		BoolExpr cannot_assign = cs.ctx.mkFalse();
		for(Case_Assign ca : case_assigns){
			cannot_assign = cs.ctx.mkOr(cannot_assign, ca.cannot_assign_condition(f, class_expr, cs));
		}
		
		
		BoolExpr can_assign = cs.ctx.mkOr(cs.ctx.mkNot(cannot_assign), all_assign_condition);
		return can_assign;
	}
	
	
	//���̃��\�b�h�ɂ�����������s��
	void assign_all(Check_status cs) throws Exception{
		//���ꂼ��̑���Ɋւ��āA������Ă������������
		for(Case_Assign ca : case_assigns){
			for(Check_return cr : ca.field_assigns){
				if(cr.indexs!=null && cr.indexs.size()>0){//�z��̑��
					Expr cr_array_ref = cr.field.get_Expr_with_indexs(cr.class_expr, new ArrayList<IntExpr>(cr.indexs.subList(0, cr.indexs.size()-1)), cs);
					IntExpr cr_index = cr.indexs.get(cr.indexs.size()-1);
					BoolExpr can_assign = assign_condition(cr_array_ref, cr_index, cs);
					
					Array array = null;
					if(cr.indexs.size()<cr.field.dims){
						array = cs.array_arrayref;
					}else{
						if(cr.field.type.equals("int")){
							array = cs.array_int;
						}else if(cr.field.type.equals("boolean")){
							array = cs.array_boolean;
						}else{
							array = cs.array_ref;
						}
					}
					Expr pre_value = array.index_access_array(cr_array_ref, cr_index, cs);
					Expr fresh_value = cs.ctx.mkITE(can_assign, cs.ctx.mkConst("freshValue_" + cs.Check_status_share.get_tmp_num(), array.elements_Sort), pre_value);
					array.update_array(cr_array_ref, cr_index, fresh_value, cs);
					
					//�z��ւ̑���ł��Amodel�t�B�[���h�͕ς��
					for(Model_Field mf : cr.field.model_fields){
						mf.tmp_plus_with_data_group(can_assign, cs);
					}
				}else{//�t�B�[���h�ւ̑��
					BoolExpr can_assign = assign_condition(cr.field, cr.class_expr, cs);
					
					Expr expr;
					if(cr.field instanceof Variable){
						Expr pre_expr = cr.field.get_Expr(cs);
						expr = cs.ctx.mkITE(can_assign, cr.field.get_fresh_value(cs), pre_expr);
					}else{
						Expr pre_expr = cs.ctx.mkSelect(cr.field.get_Expr(cs), cr.class_expr);
						expr = cs.ctx.mkStore(cr.field.get_Expr(cs), cr.class_expr, cs.ctx.mkITE(can_assign, cr.field.get_fresh_value(cs), pre_expr));
					}
					cs.add_constraint(cs.ctx.mkEq(cr.field.get_Expr_assign(cs), expr));
					cr.field.tmp_plus_with_data_group(cr.class_expr, cs);
				}
			}
		}
	}
}


