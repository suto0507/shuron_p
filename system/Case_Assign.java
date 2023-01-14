package system;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;

public class Case_Assign{
	public BoolExpr pre_condition;//���O����
	public List<Check_return> field_assigns;//������s���t�B�[���h�A�t�B�[���h�����C���X�^���X�A�C���f�b�N�X
	
	public Case_Assign(BoolExpr pre_condition, List<Check_return> field_assigns){
		this.pre_condition = pre_condition;
		this.field_assigns = field_assigns;
	}
	
	//�z��Ɋւ���
	BoolExpr cannot_assign_condition(Expr array_ref, IntExpr index, Check_status cs) throws Exception{
		BoolExpr condition = cs.ctx.mkFalse();
		for(Check_return cr : field_assigns){
			if(cr.indexs.size() > 0){
				Expr cr_array_ref = cr.field.get_Expr_with_indexs(cr.class_expr, new ArrayList<IntExpr>(cr.indexs.subList(0, cr.indexs.size()-1)), cs);
				IntExpr cr_index = cr.indexs.get(cr.indexs.size()-1);
				condition = cs.ctx.mkOr(condition, cs.ctx.mkAnd(cs.ctx.mkEq(cr_array_ref, array_ref), cs.ctx.mkEq(cr_index, index)));
			}
		}
		
		condition = cs.ctx.mkNot(condition);//��v���Ă��Ȃ�
		condition = cs.ctx.mkAnd(condition, pre_condition);
		return condition;
	}
	
	//�t�B�[���h�Ɋւ���
	BoolExpr cannot_assign_condition(Field f, Expr class_expr, Check_status cs){
		BoolExpr condition = cs.ctx.mkFalse();
		for(Check_return cr : field_assigns){
			if(cr.indexs.size() == 0 && cr.field.equals(f, cs)){
				condition = cs.ctx.mkOr(condition, cs.ctx.mkEq(class_expr, cr.class_expr));
			}
		}
		
		condition = cs.ctx.mkNot(condition);//��v���Ă��Ȃ�
		condition = cs.ctx.mkAnd(condition, pre_condition);
		return condition;
	}
}
