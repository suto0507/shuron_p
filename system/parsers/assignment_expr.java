package system.parsers;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.Status;

import system.Check_return;
import system.Check_status;
import system.F_Assign;
import system.Field;
import system.Helper_assigned_field;
import system.Pair;
import system.Parser;
import system.Parser_status;
import system.Source;
import system.Variable;

public class assignment_expr implements Parser<String>{
	implies_expr implies_expr;
	postfix_expr postfix_expr;
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		Source s_backup = s.clone();
		try{
			String st2 = "";
			postfix_expr pe = new postfix_expr();
			st2 = st2 + pe.parse(s, ps);
			st2 = st2 + new spaces().parse(s, ps);
			st2 = st2 + new assignment_op().parse(s, ps);
			st2 = st2 + new spaces().parse(s, ps);
			implies_expr ie = new implies_expr();
			st2 = st2 + ie.parse(s, ps);
			this.postfix_expr = pe;
			this.implies_expr = ie;
			st = st + st2;
		}catch (Exception e){
			s.revert(s_backup);
			this.implies_expr = new implies_expr();
			st = st + this.implies_expr.parse(s, ps);
		}
		
		
		return st;
	}
	
	public Check_return check(Check_status cs) throws Exception{
		Check_return assign_cr = null;
		
		if(this.postfix_expr != null){
			assign_cr = this.postfix_expr.check_assign(cs);
			//������Ă������ǂ����̌���
			if(!(assign_cr.field instanceof Variable)){//�t�B�[���h���ǂ����H
				//assign���Ă�����
				
				BoolExpr ex;
				
				
				ex = assign_cr.field.assign_index_expr(assign_cr.class_expr, assign_cr.indexs, cs);
				
				
				//���ł�������Ă���
				ex = cs.ctx.mkOr(ex, cs.assinable_cnst_all);
				
				//�R���X�g���N�^
				if(!(cs.in_constructor&&cs.this_field.equals(assign_cr.class_expr))){
					System.out.println("check assign");
					cs.assert_constraint(ex);
				}
				
				//final���ǂ���
				if(assign_cr.field.modifiers!=null && assign_cr.field.modifiers.is_final){
					if(cs.in_constructor&&cs.this_field.equals(assign_cr.class_expr)&&assign_cr.field.final_initialized==false){
						assign_cr.field.final_initialized = true;
					}else{
						throw new Exception("Cannot be assigned to " + assign_cr.field.field_name);
					}
				}
			}
			
			//���ӂ�Expr
			
			if(cs.in_helper){
				//helper���\�b�h�A�R���X�g���N�^�ł́A�z������O�Ɍ��؂��K�v�ȏꍇ������
				if(assign_cr.indexs.size() < assign_cr.field.dims){
					assign_cr.field.assert_all_array_assign_in_helper(0, 1, assign_cr.class_expr, cs.ctx.mkBool(true), new ArrayList<IntExpr>(), cs);
				}
			}
			
		}
		Check_return rc = this.implies_expr.check(cs);
		
		
		
		if(this.postfix_expr != null){
			//1�����ȏ�̔z��Ƃ��ăG�C���A�X�����ꍇ�ɂ́A����ȍ~�z���������O��⿌^�̌��؂��s��Ȃ���΂Ȃ�Ȃ�
			if(assign_cr.field.hava_refinement_type() && assign_cr.field.have_index_access(cs) 
					&& rc.field != null && rc.field.hava_refinement_type() && rc.field.have_index_access(cs) 
					&& assign_cr.field.dims - assign_cr.indexs.size() >= 1){
				
				if(cs.in_helper){
					if(assign_cr.field instanceof Variable){
						assign_cr.field.alias_1d_in_helper = cs.ctx.mkOr(assign_cr.field.alias_1d_in_helper, cs.get_pathcondition());
					}
					if(rc.field instanceof Variable){
						rc.field.alias_1d_in_helper = cs.ctx.mkOr(rc.field.alias_1d_in_helper, cs.get_pathcondition());
					}
				}else if(cs.in_constructor){
					if(!(assign_cr.field instanceof Variable) && cs.this_field.get_Expr(cs).equals(assign_cr.class_expr)){
						assign_cr.field.alias_in_consutructor_or_2d_in_helper = cs.ctx.mkOr(assign_cr.field.alias_in_consutructor_or_2d_in_helper, cs.get_pathcondition());
					}
					if(!(rc.field instanceof Variable) && cs.this_field.get_Expr(cs).equals(rc.class_expr)){
						rc.field.alias_in_consutructor_or_2d_in_helper = cs.ctx.mkOr(rc.field.alias_in_consutructor_or_2d_in_helper, cs.get_pathcondition());
					}
				}
			}
			//2�����ȏ�̔z��Ƃ��ăG�C���A�X�����ꍇ�ɂ́A����ȍ~⿌^�𖞂����Ȃ���΂����Ȃ�
			if(assign_cr.field.hava_refinement_type() && assign_cr.field.have_index_access(cs) 
					&& rc.field != null && rc.field.hava_refinement_type() && rc.field.have_index_access(cs) 
					&& assign_cr.field.dims - assign_cr.indexs.size() >= 2){
				
				if(cs.in_helper){
					if(assign_cr.field instanceof Variable){
						assign_cr.field.alias_in_consutructor_or_2d_in_helper = cs.ctx.mkOr(assign_cr.field.alias_in_consutructor_or_2d_in_helper, cs.get_pathcondition());
					}
					if(rc.field instanceof Variable){
						rc.field.alias_in_consutructor_or_2d_in_helper = cs.ctx.mkOr(rc.field.alias_in_consutructor_or_2d_in_helper, cs.get_pathcondition());
					}
				}
			}
			
			
			//�l��������
			assign_cr.field.assign_value(assign_cr.class_expr, assign_cr.indexs, rc.expr, cs);
			
			
			//refinement_type
			//⿌^�����z��ƃG�C���A�X�������[�J���z��́A�v�f�̕ύX�͂ł��Ȃ�
			if(assign_cr.field.dims>0 && !(assign_cr.field.hava_refinement_type() && assign_cr.field.have_index_access(cs))
					 && assign_cr.field.dims==assign_cr.indexs.size() && assign_cr.field instanceof Variable){
				Expr alias_refined;
				if(((Variable) assign_cr.field).alias_refined == null){
					alias_refined = cs.ctx.mkBool(false);
				}else{
					alias_refined = ((Variable) assign_cr.field).alias_refined;
				}
				cs.assert_constraint(cs.ctx.mkNot(alias_refined));
			}
			
			//�z���⿌^�����S���ǂ���
			cs.check_array_alias(assign_cr.field, assign_cr.class_expr, assign_cr.indexs, rc.field, rc.class_expr, rc.indexs);
			

	
			//⿌^�̌���
			System.out.println("check refinement type");
			if(assign_cr.field.hava_refinement_type()){
				if(cs.in_helper){
					if(assign_cr.field.dims >= 2 && assign_cr.field.have_index_access(cs)){//2�����ȏ�̔z��Ƃ��ăG�C���A�X���Ă���ꍇ�ɂ́A⿌^�̌��؂����Ȃ��Ƃ����Ȃ�
						cs.solver.push();
						cs.add_constraint(assign_cr.field.alias_in_consutructor_or_2d_in_helper);

						assign_cr.field.assert_refinement(cs, assign_cr.class_expr);
						cs.solver.pop();
					}
					
					Helper_assigned_field assigned_field = new Helper_assigned_field(cs.get_pathcondition(), assign_cr.field, assign_cr.class_expr);
					cs.helper_assigned_fields.add(assigned_field);
				}else if(cs.in_constructor && cs.this_field.get_Expr(cs).equals(assign_cr.class_expr)){
					BoolExpr condition = cs.this_alias;//this���ǂ����ɓn������ɂ́A⿌^�̌��؂�����K�v������
					if(assign_cr.field.dims >= 1 && assign_cr.field.have_index_access(cs)){//�z��Ƃ��ăG�C���A�X���Ă���ꍇ�ɂ́A⿌^�̌��؂����Ȃ��Ƃ����Ȃ�
						condition = cs.ctx.mkOr(condition, assign_cr.field.alias_in_consutructor_or_2d_in_helper);
					}
					
					cs.solver.push();
					cs.add_constraint(assign_cr.field.alias_in_consutructor_or_2d_in_helper);

					assign_cr.field.assert_refinement(cs, assign_cr.class_expr);
					cs.solver.pop();
				}else{
					assign_cr.field.assert_refinement(cs, assign_cr.class_expr);
				}
			}
			//�z�񂪃G�C���A�X�����Ƃ��ɁA�E�ӂ̔z���⿌^�̌��� �@�@���߂ẴG�C���A�X�ł���\���ł���Ƃ���������
			if(cs.in_helper){
				if(assign_cr.field.hava_refinement_type() && assign_cr.field.have_index_access(cs) 
						&& rc.field != null && rc.field.hava_refinement_type() && rc.field.have_index_access(cs) 
						&& assign_cr.field.dims - assign_cr.indexs.size() >= 2){
					rc.field.assert_refinement(cs, rc.class_expr);
				}
			}else if(cs.in_constructor && cs.this_field.get_Expr(cs).equals(rc.class_expr)){
				if(assign_cr.field.hava_refinement_type() && assign_cr.field.have_index_access(cs) 
						&& rc.field != null && rc.field.hava_refinement_type() && rc.field.have_index_access(cs) 
						&& assign_cr.field.dims - assign_cr.indexs.size() >= 1){
					rc.field.assert_refinement(cs, rc.class_expr);
				}
			}
			
			
			return new Check_return(assign_cr.field.get_Expr_with_indexs(assign_cr.class_expr, assign_cr.indexs, cs), assign_cr.field, assign_cr.indexs, assign_cr.class_expr);
		}else{
			return rc;
		}
	}
	
	public boolean have_index_access(Check_status cs){
 		if(this.postfix_expr!=null) return implies_expr.have_index_access(cs) || postfix_expr.have_index_access(cs);
		return implies_expr.have_index_access(cs);
	}
	
	public Check_return loop_assign(Pair<List<F_Assign>,BoolExpr>assigned_fields, Check_status cs) throws Exception{
		Check_return cr_l = null;
		if(this.postfix_expr!=null){
			cr_l = this.postfix_expr.loop_assign(assigned_fields, cs);
			
			boolean find_field = false;
			for(F_Assign f_i : assigned_fields.fst){
				if(f_i.field.equals(cr_l.field, cs) ){//����������ǉ�����
					find_field = true;
					ArrayList<Pair<Expr,List<IntExpr>>> indexs =  new ArrayList<Pair<Expr,List<IntExpr>>>();
					indexs.add(new Pair(cr_l.class_expr, cr_l.indexs));
					f_i.cnst_array.add(new Pair<BoolExpr,List<Pair<Expr,List<IntExpr>>>>(cs.get_pathcondition(), indexs));
					break;
				}
			}
			//������Ȃ�������V�����t�B�[���h���ƒǉ�����
			if(!find_field){
				List<Pair<BoolExpr,List<Pair<Expr,List<IntExpr>>>>> b_is = new ArrayList<Pair<BoolExpr,List<Pair<Expr,List<IntExpr>>>>>();
				ArrayList<Pair<Expr,List<IntExpr>>> indexs = new ArrayList<Pair<Expr,List<IntExpr>>>();
				indexs.add(new Pair(cr_l.class_expr, cr_l.indexs));
				b_is.add(new Pair(cs.get_pathcondition(), indexs));
				assigned_fields.fst.add(new F_Assign(cr_l.field, b_is));
			}
			
		}
		

		
		Check_return cr_r =  this.implies_expr.loop_assign(assigned_fields, cs);
		if(this.postfix_expr!=null){
			//1�����ȏ�̔z��Ƃ��ăG�C���A�X�����ꍇ�ɂ́A����ȍ~�z���������O��⿌^�̌��؂��s��Ȃ���΂Ȃ�Ȃ�
			if(cr_l.field.hava_refinement_type() && cr_l.field.have_index_access(cs) 
					&& cr_r.field != null && cr_r.field.hava_refinement_type() && cr_r.field.have_index_access(cs) 
					&& cr_l.field.dims - cr_l.indexs.size() >= 1){
				
				if(cs.in_helper){
					if(cr_l.field instanceof Variable){
						cr_l.field.alias_1d_in_helper = cs.ctx.mkOr(cr_l.field.alias_1d_in_helper, cs.get_pathcondition());
					}
					if(cr_r.field instanceof Variable){
						cr_r.field.alias_1d_in_helper = cs.ctx.mkOr(cr_r.field.alias_1d_in_helper, cs.get_pathcondition());
					}
				}else if(cs.in_constructor){
					if(!(cr_l.field instanceof Variable) && cs.this_field.get_Expr(cs).equals(cr_l.class_expr)){
						cr_l.field.alias_in_consutructor_or_2d_in_helper = cs.ctx.mkOr(cr_l.field.alias_in_consutructor_or_2d_in_helper, cs.get_pathcondition());
					}
					if(!(cr_r.field instanceof Variable) && cs.this_field.get_Expr(cs).equals(cr_r.class_expr)){
						cr_r.field.alias_in_consutructor_or_2d_in_helper = cs.ctx.mkOr(cr_r.field.alias_in_consutructor_or_2d_in_helper, cs.get_pathcondition());
					}
				}
			}
			//2�����ȏ�̔z��Ƃ��ăG�C���A�X�����ꍇ�ɂ́A����ȍ~⿌^�𖞂����Ȃ���΂����Ȃ�
			if(cr_l.field.hava_refinement_type() && cr_l.field.have_index_access(cs) 
					&& cr_r.field != null && cr_r.field.hava_refinement_type() && cr_r.field.have_index_access(cs) 
					&& cr_l.field.dims - cr_l.indexs.size() >= 2){
				
				if(cs.in_helper){
					if(cr_l.field instanceof Variable){
						cr_l.field.alias_in_consutructor_or_2d_in_helper = cs.ctx.mkOr(cr_l.field.alias_in_consutructor_or_2d_in_helper, cs.get_pathcondition());
					}
					if(cr_r.field instanceof Variable){
						cr_r.field.alias_in_consutructor_or_2d_in_helper = cs.ctx.mkOr(cr_r.field.alias_in_consutructor_or_2d_in_helper, cs.get_pathcondition());
					}
				}
			}
			return cr_l;
		}
		return cr_r;
	}

		
}

