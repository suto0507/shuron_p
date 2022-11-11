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
		Field v = null;
		Expr assign_expr = null;//���ӂ�Expr
		Expr assign_field_expr = null;//⿌^�������Ă������̂��߂Ɏg�� this.a.b.x[1]��this.a.b.x�̕���
		Expr assign_expr_full = null;//���ӂ�Expr��full�o�[�W�����@ �Ԃ�l�Ƃ��ĕԂ�
		ArrayList<IntExpr> indexs = null;//���ӂ�indexs
		ArrayList<IntExpr> v_indexs = null;//���ӂ�indexs this.a[1][2].b.x[3][4]��3,4�̕���
		Expr v_class_object_expr = null;
		if(this.postfix_expr != null){
			v = this.postfix_expr.check_assign(cs);
			//������Ă������ǂ����̌���
			if(v.is_this_field()){//�t�B�[���h���ǂ����H
				//assign���Ă�����
				
				BoolExpr ex;
				
				
				ex = v.assign_index_expr(v.index, cs);
				
				
				//���ł�������Ă���
				ex = cs.ctx.mkOr(ex, cs.assinable_cnst_all);
				
				//�R���X�g���N�^
				if(!(cs.in_constructor&&v.class_object.equals(cs.this_field, cs))){
					System.out.println("check assign");
					cs.assert_constraint(ex);
				}
				
				//final���ǂ���
				if(v.modifiers!=null && v.modifiers.is_final){
					if(cs.in_constructor&&v.class_object.equals(cs.this_field, cs)&&v.final_initialized==false){
						v.final_initialized = true;
					}else{
						throw new Exception("Cannot be assigned to " + v.field_name);
					}
				}
			}
			
			//���ӂ�Expr
			indexs = v.index;
			
			v_indexs = new ArrayList<IntExpr>(v.index.subList(v.class_object_dims_sum(), v.index.size()));
			
			v_class_object_expr = v.class_object_expr;
			
			
			if(cs.in_helper || (cs.in_constructor && !(v instanceof Variable) && v.class_object != null && v.class_object.equals(cs.this_field, cs))){
				//helper���\�b�h�A�R���X�g���N�^�ł́A�z������O�Ɍ��؂��K�v�ȏꍇ������
				if(v.refinement_type_clause!=null && indexs.size() < v.dims_sum()){
					cs.solver.push();
					
					//�G�C���A�X���Ă���Ƃ������ł���
					cs.add_constraint(v.alias_in_helper_or_consutructor);
					
					Field old_v = cs.this_old_status.search_internal_id(v.internal_id);

					Expr old_expr = null;
					Expr expr = null;
					if(v instanceof Variable){
						expr = v.get_Expr(cs);
					}else{
						old_expr = cs.ctx.mkSelect(old_v.get_Expr(cs), v_class_object_expr);
						expr = cs.ctx.mkSelect(v.get_Expr(cs), v_class_object_expr);
					}
					
					//���\�b�h�̍ŏ��ł�⿌^���������Ă��邱�Ƃ����肵�Ă���
					//�t�B�[���h����
					if(cs.in_helper && !(v instanceof Variable)){
						if(old_v.refinement_type_clause.refinement_type!=null){
							old_v.refinement_type_clause.refinement_type.add_refinement_constraint(cs.this_old_status, old_v, old_expr, old_v.class_object, v_class_object_expr, new ArrayList<IntExpr>(indexs.subList(0, v.class_object_dims_sum())), true);
						}else if(old_v.refinement_type_clause.ident!=null){
							refinement_type rt = cs.search_refinement_type(old_v.class_object.type, old_v.refinement_type_clause.ident);
							if(rt!=null){
								rt.add_refinement_constraint(cs.this_old_status, old_v, old_expr, old_v.class_object, v_class_object_expr, new ArrayList<IntExpr>(indexs.subList(0, v.class_object_dims_sum())), true);
							}else{
				                throw new Exception("can't find refinement type " + old_v.refinement_type_clause.ident);
				            }
						}
					}
					
					
					if(v.refinement_type_clause.refinement_type!=null){
						v.refinement_type_clause.refinement_type.assert_refinement(cs, v, expr, v.class_object, v_class_object_expr, new ArrayList<IntExpr>(indexs.subList(0, v.class_object_dims_sum())));
					}else if(v.refinement_type_clause.ident!=null){
						refinement_type rt = cs.search_refinement_type(v.class_object.type, v.refinement_type_clause.ident);
						if(rt!=null){
							rt.assert_refinement(cs, v, expr, v.class_object, v_class_object_expr, new ArrayList<IntExpr>(indexs.subList(0, v.class_object_dims_sum())));
						}else{
			                throw new Exception("can't find refinement type " + v.refinement_type_clause.ident);
			            }
					}
					
					cs.solver.pop();
				}
				
				
			}
			
		}
		Check_return rc = this.implies_expr.check(cs);
		Expr implies_tmp = rc.expr;
		
		
		
		if(this.postfix_expr != null){
			//1�����ȏ�̔z��Ƃ��ăG�C���A�X�����ꍇ�ɂ́A����ȍ~�z���������O��⿌^�̌��؂��s��Ȃ���΂Ȃ�Ȃ�
			if(v.refinement_type_clause!=null && rc.field != null && rc.field.refinement_type_clause!=null && indexs.size()+1 <= v.dims_sum() ){
				if(cs.in_helper){
					if(v instanceof Variable)v.alias_in_helper_or_consutructor = cs.ctx.mkOr(v.alias_in_helper_or_consutructor, cs.get_pathcondition());
					if(rc.field instanceof Variable)rc.field.alias_in_helper_or_consutructor = cs.ctx.mkOr(rc.field.alias_in_helper_or_consutructor, cs.get_pathcondition());
				}else if(cs.in_constructor){
					if(!(v instanceof Variable) && v.class_object != null && v.class_object.equals(cs.this_field, cs)){
						v.alias_in_helper_or_consutructor = cs.ctx.mkOr(v.alias_in_helper_or_consutructor, cs.get_pathcondition());
					}
					if(!(rc.field instanceof Variable) && rc.field.class_object != null && rc.field.class_object.equals(cs.this_field, cs)){
						rc.field.alias_in_helper_or_consutructor = cs.ctx.mkOr(rc.field.alias_in_helper_or_consutructor, cs.get_pathcondition());
					}
				}
			}
			//2�����ȏ�̔z��Ƃ��ăG�C���A�X�����ꍇ�ɂ́A����ȍ~⿌^�𖞂����Ȃ���΂����Ȃ�
			if(v.refinement_type_clause!=null && rc.field != null && rc.field.refinement_type_clause!=null && indexs.size()+2 <= v.dims_sum() ){
				if(cs.in_helper){
					if(v instanceof Variable)v.alias_2d_in_helper_or_consutructor = cs.ctx.mkOr(v.alias_2d_in_helper_or_consutructor, cs.get_pathcondition());
					if(rc.field instanceof Variable)rc.field.alias_2d_in_helper_or_consutructor = cs.ctx.mkOr(rc.field.alias_2d_in_helper_or_consutructor, cs.get_pathcondition());
				}else if(cs.in_constructor){
					if(!(v instanceof Variable) && v.class_object != null && v.class_object.equals(cs.this_field, cs)){
						v.alias_2d_in_helper_or_consutructor = cs.ctx.mkOr(v.alias_2d_in_helper_or_consutructor, cs.get_pathcondition());
					}
					if(!(rc.field instanceof Variable) && rc.field.class_object != null && rc.field.class_object.equals(cs.this_field, cs)){
						rc.field.alias_2d_in_helper_or_consutructor = cs.ctx.mkOr(rc.field.alias_2d_in_helper_or_consutructor, cs.get_pathcondition());
					}
				}
			}
			
			assign_expr = v.get_Expr_assign(cs);
			
			if(v instanceof Variable){
				assign_field_expr = v.get_Expr_assign(cs);
			}else{
				assign_field_expr = cs.ctx.mkSelect(v.get_Expr_assign(cs), v_class_object_expr);
			}
			
			assign_expr_full = assign_field_expr;
			for(IntExpr index : v_indexs){
				cs.ctx.mkSelect(assign_expr_full, index);
			}
			
			
			BoolExpr expr = cs.ctx.mkEq(assign_expr, v.assign_value(indexs, implies_tmp, cs));
			cs.add_constraint(expr);
			
			
			
			v.temp_num++;
			
			
			//refinement_type
			//⿌^�����z��ƃG�C���A�X�������[�J���z��́A�v�f�̕ύX�͂ł��Ȃ�
			if(v.dims>0 && !(v.refinement_type_clause!=null && v.refinement_type_clause.have_index_access(v.class_object.type, cs))
					 && v.dims_sum()==indexs.size() && v instanceof Variable){
				Expr alias_refined;
				if(((Variable) v).alias_refined == null){
					alias_refined = cs.ctx.mkBool(false);
				}else{
					alias_refined = ((Variable) v).alias_refined;
				}
				cs.assert_constraint(cs.ctx.mkNot(alias_refined));
			}
			
			//�z���⿌^�����S���ǂ���
			Expr rc_assign_field_expr = null;
			Expr rc_class_field_expr = null;
			if(rc.field!=null){
				rc_assign_field_expr = rc.field.get_full_Expr(new ArrayList<IntExpr>(rc.indexs.subList(0, rc.field.class_object_dims_sum())), cs);
				rc_class_field_expr = rc.field.class_object.get_full_Expr((ArrayList<IntExpr>) rc.indexs.clone(), cs);
			}
			cs.check_array_alias(v, assign_field_expr, v_class_object_expr, indexs, rc.field, rc_assign_field_expr, rc_class_field_expr, rc.indexs);
			
			
			
			
			

			BoolExpr pathcondition;
			if(cs.pathcondition==null){
				pathcondition = cs.ctx.mkBool(true);
			}else{
				pathcondition = cs.pathcondition;
			}
			
			
			
			//⿌^�̌���
			if((v.refinement_type_clause!=null && cs.in_helper)
					|| (v.refinement_type_clause!=null && (cs.in_constructor && !(v instanceof Variable) && v.class_object.equals(cs.this_field, cs)))){//helper���\�b�h�A�R���X�g���N�^�̒��ł́A⿌^�̌��؂���񂵂ɂ���
				if(v.dims >= 2){//2�����ȏ�̔z��Ƃ��ăG�C���A�X���Ă���ꍇ�ɂ́A⿌^�̌��؂����Ȃ��Ƃ����Ȃ�
					cs.solver.push();
					cs.add_constraint(v.alias_2d_in_helper_or_consutructor);
					if(v.refinement_type_clause.refinement_type!=null){
						v.refinement_type_clause.refinement_type.assert_refinement(cs, v, assign_field_expr, v.class_object, v_class_object_expr, new ArrayList<IntExpr>(v.index.subList(0, v.class_object_dims_sum())));
					}else if(v.refinement_type_clause.ident!=null){
						refinement_type rt = cs.search_refinement_type(v.class_object.type, v.refinement_type_clause.ident);
						if(rt!=null){
							rt.assert_refinement(cs, v, assign_field_expr, v.class_object, v_class_object_expr, new ArrayList<IntExpr>(v.index.subList(0, v.class_object_dims_sum())));
						}else{
			                throw new Exception("can't find refinement type " + v.refinement_type_clause.ident);
			            }
					}
					cs.solver.pop();
				}
				if(cs.in_helper){
					Helper_assigned_field assigned_field = new Helper_assigned_field(pathcondition, v, v_class_object_expr, new ArrayList<IntExpr>(v.index.subList(0, v.class_object_dims_sum())));
					cs.helper_assigned_fields.add(assigned_field);
				}
			}else if(v.refinement_type_clause!=null){
				if(v.refinement_type_clause.refinement_type!=null){
					v.refinement_type_clause.refinement_type.assert_refinement(cs, v, assign_field_expr, v.class_object, v_class_object_expr, new ArrayList<IntExpr>(v.index.subList(0, v.class_object_dims_sum())));
				}else if(v.refinement_type_clause.ident!=null){
					refinement_type rt = cs.search_refinement_type(v.class_object.type, v.refinement_type_clause.ident);
					if(rt!=null){
						rt.assert_refinement(cs, v, assign_field_expr, v.class_object, v_class_object_expr, new ArrayList<IntExpr>(v.index.subList(0, v.class_object_dims_sum())));
					}else{
		                throw new Exception("can't find refinement type " + v.refinement_type_clause.ident);
		            }
				}
			}
			
			return new Check_return(assign_expr_full, v, indexs);
		}else{
			return rc;
		}
	}
	
	public boolean have_index_access(Check_status cs){
 		if(this.postfix_expr!=null) return implies_expr.have_index_access(cs) || postfix_expr.have_index_access(cs);
		return implies_expr.have_index_access(cs);
	}
	
	public Check_return loop_assign(Pair<List<Pair<Field,List<List<IntExpr>>>>,Boolean>assigned_fields, Check_status cs) throws Exception{
		Check_return cr_l = null;
		if(this.postfix_expr!=null){
			cr_l = this.postfix_expr.loop_assign(assigned_fields, cs);
			
			boolean find_field = false;
			for(Pair<Field,List<List<IntExpr>>> f_i : assigned_fields.fst){
				if(f_i.fst == cr_l.field){//����������ǉ�����
					find_field = true;
					f_i.snd.add(cr_l.indexs);
					break;
				}
			}
			//������Ȃ�������V�����t�B�[���h���ƒǉ�����
			if(!find_field){
				List<List<IntExpr>> f_indexs_snd = new ArrayList<List<IntExpr>>();
				f_indexs_snd.add(cr_l.indexs);
				Pair<Field,List<List<IntExpr>>> f_i = new Pair<Field,List<List<IntExpr>>>(cr_l.field, f_indexs_snd);
				assigned_fields.fst.add(f_i);
			}
			
		}
		

		
		Check_return cr_r =  this.implies_expr.loop_assign(assigned_fields, cs);
		if(this.postfix_expr!=null){
			if(cs.in_constructor || cs.in_helper){
				
				//1�����ȏ�̔z��Ƃ��ăG�C���A�X�����ꍇ�ɂ́A����ȍ~�z���������O��⿌^�̌��؂��s��Ȃ���΂Ȃ�Ȃ�
				if(cr_l.field.refinement_type_clause!=null && cr_r.field != null && cr_r.field.refinement_type_clause != null && cr_l.indexs.size()+1 <= cr_l.field.dims_sum() ){
					if(cs.in_helper){
						if(cr_l.field instanceof Variable)cr_l.field.alias_in_helper_or_consutructor = cs.ctx.mkOr(cr_l.field.alias_in_helper_or_consutructor, cs.get_pathcondition());
						if(cr_r.field instanceof Variable)cr_r.field.alias_in_helper_or_consutructor = cs.ctx.mkOr(cr_r.field.alias_in_helper_or_consutructor, cs.get_pathcondition());
					}else if(cs.in_constructor){
						if(!(cr_l.field instanceof Variable) && cr_l.field.class_object != null && cr_l.field.class_object.equals(cs.this_field, cs)){
							cr_l.field.alias_in_helper_or_consutructor = cs.ctx.mkOr(cr_l.field.alias_in_helper_or_consutructor, cs.get_pathcondition());
						}
						if(!(cr_r.field instanceof Variable) && cr_r.field.class_object != null && cr_r.field.class_object.equals(cs.this_field, cs)){
							cr_r.field.alias_in_helper_or_consutructor = cs.ctx.mkOr(cr_r.field.alias_in_helper_or_consutructor, cs.get_pathcondition());
						}
					}
				}
				//2�����ȏ�̔z��Ƃ��ăG�C���A�X�����ꍇ�ɂ́A����ȍ~⿌^�𖞂����Ȃ���΂����Ȃ�
				if(cr_l.field.refinement_type_clause!=null && cr_r.field != null && cr_r.field.refinement_type_clause != null && cr_l.indexs.size()+2 <= cr_l.field.dims_sum() ){
					if(cs.in_helper){
						if(cr_l.field instanceof Variable)cr_l.field.alias_2d_in_helper_or_consutructor = cs.ctx.mkOr(cr_l.field.alias_2d_in_helper_or_consutructor, cs.get_pathcondition());
						if(cr_r.field instanceof Variable)cr_r.field.alias_2d_in_helper_or_consutructor = cs.ctx.mkOr(cr_r.field.alias_2d_in_helper_or_consutructor, cs.get_pathcondition());
					}else if(cs.in_constructor){
						if(!(cr_l.field instanceof Variable) && cr_l.field.class_object != null && cr_l.field.class_object.equals(cs.this_field, cs)){
							cr_l.field.alias_2d_in_helper_or_consutructor = cs.ctx.mkOr(cr_l.field.alias_2d_in_helper_or_consutructor, cs.get_pathcondition());
						}
						if(!(cr_r.field instanceof Variable) && cr_r.field.class_object != null && cr_r.field.class_object.equals(cs.this_field, cs)){
							cr_r.field.alias_2d_in_helper_or_consutructor = cs.ctx.mkOr(cr_r.field.alias_2d_in_helper_or_consutructor, cs.get_pathcondition());
						}
					}
				}
			}
			return cr_l;
		}
		return cr_r;
	}

		
}

