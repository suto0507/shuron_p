package system.parsers;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.Status;

import system.Check_status;
import system.Field;
import system.Pair;
import system.Parser;
import system.Parser_status;
import system.Source;
import system.Variable;
import system.parsers.spec_case_seq.F_Assign;

public class postfix_expr implements Parser<String>{
	primary_expr primary_expr;
	//�v�f����1�ȏ�Ȃ��낪�܂܂��
	ArrayList<primary_suffix> primary_suffixs;
	public String parse(Source s,Parser_status ps)throws Exception{
		this.primary_suffixs = new ArrayList<primary_suffix>();
		String st = "";
		this.primary_expr = new primary_expr();
		st = st + this.primary_expr.parse(s,ps);
		Source s_backup = s.clone();
		try{
			while(true){
				s_backup = s.clone();
				primary_suffix ee = new primary_suffix();
				st = st + ee.parse(s, ps);
				this.primary_suffixs.add(ee);
			}
		}catch (Exception e){
			s.revert(s_backup);
		}
		
		return st;
	}
	
	public Expr check(Check_status cs) throws Exception{
		Expr ex = null;
		
		Field f = null;
		String ident = null;
		boolean is_refine_value = false;
		if(this.primary_expr.is_this){
			if(this.primary_suffixs.size() == 0 && cs.in_constructor){//this�P�̂̃R���X�g���N�^�[�ł̎g�p
				cs.constructor_refinement_check();
			}
			f = cs.this_field;
			ex = f.get_Expr(cs);
			//�֐��Ăяo��
			if(cs.in_method_call){
				f = cs.call_field;
				ex = cs.call_expr;
			}else if(cs.in_refinement_predicate){
				f = cs.refined_class_Field;
				ex = cs.refined_class_Expr;
			}
		}else if(this.primary_expr.ident!=null){
			if(cs.in_refinement_predicate==true && this.primary_expr.ident.equals(cs.refinement_type_value)){
				f = cs.refined_Field;
				ex = cs.refined_Expr;
				is_refine_value = true;
			}else{
				if(cs.in_method_call){//�֐��Ăяo��
					Field searched_field = cs.search_field(primary_expr.ident, cs.call_field, cs.call_field_index ,cs);
					if(cs.search_called_method_arg(primary_expr.ident)){
						f = cs.get_called_method_arg(primary_expr.ident);
						ex = f.get_Expr(cs);
					}else if(searched_field != null){
						f = searched_field;
						ex = cs.ctx.mkSelect((ArrayExpr) f.get_Expr(cs),cs.call_expr);
					}else if(this.primary_suffixs.size() > 0 && this.primary_suffixs.get(0).is_method){
						ident = this.primary_expr.ident;
						f = cs.call_field;
						ex = cs.call_expr;
					}else{
						throw new Exception(cs.call_field.type + " don't have " + this.primary_expr.ident);
					}
					
				}else if(cs.in_refinement_predicate){//⿌^
					Field searched_field = cs.search_field(primary_expr.ident, cs.refined_class_Field, cs.refined_class_Field_index ,cs);
					if((cs.refined_class_Field==null||cs.refined_class_Field.equals(cs.this_field, cs))&&cs.search_variable(this.primary_expr.ident)){
						f = cs.get_variable(this.primary_expr.ident);
						ex = f.get_Expr(cs);
					}else if(searched_field != null){
						f = searched_field;
						ex = cs.ctx.mkSelect((ArrayExpr) f.get_Expr(cs),cs.refined_class_Expr);
					}else if(this.primary_suffixs.size() > 0 && this.primary_suffixs.get(0).is_method){
						ident = this.primary_expr.ident;
						f = cs.refined_class_Field;
						ex = cs.refined_class_Expr;
					}else{
						throw new Exception(cs.refined_class_Field.type + " don't have " + this.primary_expr.ident);
					}
					
				}else{
					Field searched_field = cs.search_field(primary_expr.ident, cs.this_field, null ,cs);
					if(cs.search_variable(primary_expr.ident)){
						f = cs.get_variable(primary_expr.ident);
						ex = f.get_Expr(cs);
					}else if(searched_field != null){
						f = searched_field;
						ex = cs.ctx.mkSelect((ArrayExpr) f.get_Expr(cs),cs.this_field.get_Expr(cs));
					}else if(this.primary_suffixs.size() > 0 && this.primary_suffixs.get(0).is_method){
						ident = this.primary_expr.ident;
						f = cs.this_field;
						ex = f.get_Expr(cs);
					}else{
						throw new Exception(cs.this_field.type + " don't have " + this.primary_expr.ident);
					}
				}

				if(cs.in_refinement_predicate==true){
					if(f.modifiers.is_final==false){//⿌^�̒��ł�final�ł���K�v������
						throw new Exception("can use only final variable in refinement type");
					}
				}
				//JML�߂ł̎g���Ȃ������̊m�F
				if(cs.ban_default_visibility){
					if(f.modifiers!=null&&f.modifiers.is_privte==false){
						throw new Exception("can not use default visibility variable");
					}
				}
				if(cs.ban_private_visibility){
					if(f.modifiers!=null&&f.modifiers.is_privte==true){
						throw new Exception("can not use private visibility variable");
					}
				}
			}
		}else if(this.primary_expr.bracket_expression!=null){
			if(this.primary_suffixs.size() == 0){
				ex = this.primary_expr.bracket_expression.check(cs);
			}else{
				//���Ԃ񌵂���
				
			}
		}else if(this.primary_expr.java_literal!=null){
			if(this.primary_suffixs.size() == 0){
				ex = this.primary_expr.java_literal.check(cs);
			}else{
				throw new Exception("literal dont have suffix");
				
			}
		}else if(this.primary_expr.jml_primary!=null){
			if(cs.in_method_call){
				if(this.primary_expr.jml_primary.is_result){
					f = cs.result;
					ex = f.get_Expr(cs);
				}else{
					//������������Ɠ������R�Ō�����
					ex = this.primary_expr.jml_primary.old_expression.spec_expression.check(cs.old_status);
				}
			}else{
				if(this.primary_expr.jml_primary.is_result){
					f = cs.return_v;
					ex = cs.return_expr;
					//return ex;
				}else{
					//������������Ɠ������R�Ō�����
					ex = this.primary_expr.jml_primary.old_expression.spec_expression.check(cs.this_old_status);
				}
			}
		}else if(this.primary_expr.new_expr!=null){
			f = this.primary_expr.new_expr.check(cs);
			ex = f.get_Expr(cs);
		}else{
			//return null;
		}
		
		if(f!=null && f.refinement_type_clause!=null && is_refine_value==false){//⿌^
			add_refinement_constraint(cs, f, ex, cs.in_method_call, cs.call_expr, cs.call_field, cs.call_field_index);
		}
		
		
		//suffix�ɂ���
		IntExpr f_index = null;
		
		for(int i = 0; i < this.primary_suffixs.size(); i++){
			primary_suffix ps = this.primary_suffixs.get(i);
			if(ps.is_field){
				if(this.primary_suffixs.size() > i+1 && this.primary_suffixs.get(0).is_method){
					ident = ps.ident;
				}else{
					Field pre_f = f;
					Expr pre_ex = ex;
					IntExpr pre_f_index = f_index;
					
					Field searched_field = cs.search_field(ps.ident, f, f_index, cs);
					if(searched_field != null){
						f = searched_field;
						ex = cs.ctx.mkSelect((ArrayExpr)f.get_Expr(cs), ex);
					}else{
						throw new Exception(f.type + " don't have " + ps.ident);
					}
					
					if(f.refinement_type_clause!=null){//⿌^
						add_refinement_constraint(cs, f, ex, true, pre_ex, pre_f, pre_f_index);
					}
					
					if(cs.in_refinement_predicate==true){//⿌^�̒��ł�final�ł���K�v������
						if(f.modifiers.is_final==false){
							throw new Exception("can use only final variable in refinement type");
						}
					}
					
					//JML�߂ł̎g���Ȃ������̊m�F
					if(cs.ban_default_visibility){
						if(f.modifiers!=null&&f.modifiers.is_privte==false){
							throw new Exception("can not use default visibility variable");
						}
					}
					if(cs.ban_private_visibility){
						if(f.modifiers!=null&&f.modifiers.is_privte==true){
							throw new Exception("can not use private visibility variable");
						}
					}
				}
				
				f_index = null;
			}else if(ps.is_index){
				f_index = (IntExpr) ps.expression.check(cs);
				ex = cs.ctx.mkSelect((ArrayExpr) ex, f_index);
				ident = null;
				if(cs.in_refinement_predicate==true){//⿌^�̒��ł͔z��͎g���Ȃ�
					throw new Exception("can not use array in refinement type");
				}
				
			}else if(ps.is_method){
				//�֐��̌Ăяo��
				f = method(cs, ident,f, ex, ps, f_index);
				ex = f.get_Expr(cs);
				cs.in_method_call = false;
				ident = null;
				f_index = null;
			}
		}

		
		return ex;
	}
	
	public Field check_assign(Check_status cs) throws Exception{
		Expr ex = null;
		Field f = null;
		if(this.primary_suffixs.size() == 0){
			if(this.primary_expr.is_this){
				//����̓A�E�g
				throw new Exception("can't assign this");
			}if(this.primary_expr.ident!=null){

				Field searched_field = cs.search_field(primary_expr.ident, cs.this_field, null ,cs);
				if(cs.search_variable(primary_expr.ident)){
					f = cs.get_variable(primary_expr.ident);
					f.class_object_expr = cs.this_field.get_Expr(cs);
				}else if(searched_field != null){
					f = searched_field;
					f.class_object_expr = cs.this_field.get_Expr(cs);
				}else{
					throw new Exception(cs.this_field.type + " don't have " + this.primary_expr.ident);
				}

			}else if(this.primary_expr.java_literal!=null){
				throw new Exception("can't assign java literal");
			}else{
				throw new Exception("can't write in lef side");
			}
			return f;

		}else{

			String ident = null;
			if(this.primary_expr.is_this){
				f = cs.this_field;
				ex = f.get_Expr(cs);
			}else if(this.primary_expr.ident!=null){

				Field searched_field = cs.search_field(primary_expr.ident, cs.this_field, null, cs);
				if(cs.search_variable(primary_expr.ident)){
					f = cs.get_variable(primary_expr.ident);
					f.class_object_expr = cs.this_field.get_Expr(cs);
					ex = f.get_Expr(cs);
				}else if(searched_field != null){
					f = searched_field;
					f.class_object_expr = cs.this_field.get_Expr(cs);
					ex = cs.ctx.mkSelect((ArrayExpr) f.get_Expr(cs), cs.this_field.get_Expr(cs));
				}else if(this.primary_suffixs.size() > 0 && this.primary_suffixs.get(0).is_method){
					ident = this.primary_expr.ident;
					f = cs.this_field;
					ex = f.get_Expr(cs);
				}else{
					throw new Exception(cs.this_field.type + " don't have " + this.primary_expr.ident);
				}

				
			}else if(this.primary_expr.bracket_expression!=null){
				//���Ԃ񌵂���
			}else if(this.primary_expr.java_literal!=null){
				//System.out.println("literal dont have suffix");
				throw new Exception("literal dont have suffix");
			}else{
				//new expr
				return null;
			}
			//suffix�ɂ���
			IntExpr f_index = null;
			for(int i = 0; i < this.primary_suffixs.size(); i++){
				primary_suffix ps = this.primary_suffixs.get(i);
				if(ps.is_field){
					if(this.primary_suffixs.size() > i+1 && this.primary_suffixs.get(0).is_method){
						ident = ps.ident;
					}else{
						Field searched_field = cs.search_field(ps.ident, f, f_index, cs);
						if(searched_field != null){
							f = searched_field;
							f.class_object_expr = ex;
							ex = cs.ctx.mkSelect((ArrayExpr)f.get_Expr(cs), ex);
						}else{
							throw new Exception(f.type + " don't have " + ps.ident);
						}

					}
					
					f_index = null;
					
				}else if(ps.is_index){
					f_index = (IntExpr)ps.expression.check(cs);
					
					BoolExpr index_bound = cs.ctx.mkGe(f_index, cs.ctx.mkInt(0));
					if(f.length!=null){
						index_bound = cs.ctx.mkAnd(index_bound, cs.ctx.mkGt(f.length, f_index));
					}
					System.out.println("check index out of bounds");
					cs.assert_constraint(index_bound);
					
					f.index = f_index;
					if(i != this.primary_suffixs.size()-1){
						ex = cs.ctx.mkSelect((ArrayExpr) ex, f_index);
					}
					ident = null;
				}else if(ps.is_method){
					//�֐��̌Ăяo��
					if(i == this.primary_suffixs.size()-1){
						throw new Exception("The left-hand side of an assignment must be a variable");
					}
					f = method(cs, ident,f, ex, ps, f_index);
					ex = f.get_Expr(cs);
					cs.in_method_call = false;
					ident = null;
					f_index = null;
				}
			}
			return f;
		}
	}

	public Field method(Check_status cs, String ident, Field f, Expr ex, primary_suffix ps , IntExpr f_index)throws Exception{
		
		//�R���X�g���N�^�ł̎��C���X�^���X�̊֐��Ăяo��
		if(cs.in_constructor && f.equals(cs.this_field, cs)){
			cs.constructor_refinement_check();
		}
		
		class_declaration cd = cs.Check_status_share.compilation_unit.search_class(f.type);
		method_decl md = cs.Check_status_share.compilation_unit.search_method(f.type, ident);
		if(md == null){
			throw new Exception("can't find method " + ident);
		}
		
		if(md.formals.param_declarations.size()!=ps.expression_list.expressions.size()){
			throw new Exception("wrong number of arguments");
		}
		
		
		//�����̏���
		List<Expr> method_arg_valuse = new ArrayList<Expr>();
		for(int j = 0; j < md.formals.param_declarations.size(); j++){
			method_arg_valuse.add(ps.expression_list.expressions.get(j).check(cs));
		}
		
		//�֐����̏���
		cs.in_method_call = true;
		
		cs.called_method_args = new ArrayList<Variable>();
		
		
		
		cs.call_expr = ex;
		cs.call_field = f;
		cs.call_field_index = f_index;
		
		for(int j = 0; j < md.formals.param_declarations.size(); j++){
			param_declaration pd = md.formals.param_declarations.get(j);
			modifiers m = new modifiers();
			m.is_final = pd.is_final;
			Variable v = new Variable(cs.Check_status_share.get_tmp_num(), pd.ident, pd.type_spec.type.type, pd.type_spec.dims, pd.type_spec.refinement_type_clause, m, f);
			cs.called_method_args.add(v);
			v.temp_num = 0;
			//�����ɒl��R�Â���
			cs.add_constraint(cs.ctx.mkEq(v.get_Expr(cs), method_arg_valuse.get(j)));
			//⿌^
			if(v.refinement_type_clause!=null){
				if(v.refinement_type_clause.refinement_type!=null){
					v.refinement_type_clause.refinement_type.assert_refinement(cs, v, v.get_Expr(cs));
				}else{
					refinement_type rt = cs.search_refinement_type(v.class_object.type, v.refinement_type_clause.ident);
					if(rt!=null){
						rt.assert_refinement(cs, v, v.get_Expr(cs));
					}else{
						throw new Exception("can't find refinement type " + v.refinement_type_clause.ident);
					}
				}
			}
		}
		//���O����
		//BoolExpr pre_invariant_expr = null;
		BoolExpr require_expr = null;

		if(md.method_specification != null){
			require_expr = md.method_specification.requires_expr(cs);
			cs.assert_constraint(require_expr);
		}
		
		//old data
		Check_status csc = cs.clone();
		csc.clone_list();
		cs.old_status = csc;
		
		//assign
		if(md.method_specification != null){
			
			Pair<List<F_Assign>, BoolExpr> assign_cnsts = md.method_specification.assignables(cs);
			for(F_Assign fa : assign_cnsts.fst){
				BoolExpr assign_expr;
				//�t�B�[���h�ւ̑��
				if(fa.cnst!=null){
					assign_expr = cs.ctx.mkImplies(fa.cnst, fa.field.assinable_cnst);
				}else{
					assign_expr = cs.ctx.mkBool(true);
				}
				//�z��̗v�f�ɑ��
				if(fa.cnst_array.size()>0){
					IntExpr index_expr = cs.ctx.mkIntConst("tmpIdex" + cs.Check_status_share.tmp_num);
					BoolExpr call_assign_expr = fa.assign_index_expr(index_expr, cs);
					BoolExpr field_assign_expr = fa.field.assign_index_expr(index_expr, cs);
					assign_expr = cs.ctx.mkAnd(assign_expr, cs.ctx.mkImplies(call_assign_expr, field_assign_expr));
				}else{
					assign_expr = cs.ctx.mkAnd(assign_expr, cs.ctx.mkBool(true));
				}
				//���ł�������Ă���
				assign_expr = cs.ctx.mkOr(assign_expr, cs.assinable_cnst_all);
				
				System.out.println("check assign");
				cs.assert_constraint(assign_expr);
				
				//���ۂɑ�����鐧���ǉ�����
				System.out.println("assign " + fa.field.field_name);
				//�t�B�[���h�ւ̑��
				if(fa.cnst!=null){
					Expr tmp_Expr = fa.field.get_Expr_tmp(cs);
					BoolExpr expr = cs.ctx.mkEq(fa.field.get_full_Expr_assign(cs), 
							cs.ctx.mkITE(cs.ctx.mkOr(fa.cnst, cs.assinable_cnst_all), tmp_Expr, fa.field.get_full_Expr(cs)));
					cs.add_constraint(expr);
					fa.field.temp_num++;
					
				}
				//�z��̗v�f�ɑ��
				if(fa.cnst_array.size()>0){
					Expr tmp_Expr = fa.field.get_Expr_tmp(cs);
					
					IntExpr index_expr = cs.ctx.mkIntConst("tmpIdex" + cs.Check_status_share.tmp_num);
					Expr old_element = cs.ctx.mkSelect((ArrayExpr) fa.field.get_full_Expr(cs), index_expr);
					Expr new_element = cs.ctx.mkSelect((ArrayExpr) fa.field.get_full_Expr_assign(cs), index_expr);
					BoolExpr expr = cs.ctx.mkImplies(cs.ctx.mkNot(cs.ctx.mkOr(fa.assign_index_expr(index_expr, cs), cs.assinable_cnst_all)), cs.ctx.mkEq(old_element, new_element));
					cs.add_constraint(expr);
					fa.field.temp_num++;
					
				}
				
				
			}
		}else{
			//assignable���܂߂��C�ӂ̎d�l��������Ă��Ȃ��֐�
			for(Field f_a : cs.fields){
				f_a.temp_num++;
			}
		}
		
		//�Ԃ�l
		modifiers m_tmp = new modifiers();
		Variable result = new Variable(cs.Check_status_share.get_tmp_num(), "return_tmp", md.type_spec.type.type, md.type_spec.dims, md.type_spec.refinement_type_clause, m_tmp, f);
		result.temp_num++;
		cs.result = result;
		if(result.refinement_type_clause!=null){
			if(result.refinement_type_clause.refinement_type!=null){
				result.refinement_type_clause.refinement_type.add_refinement_constraint(cs, result, result.get_Expr(cs));
			}else{
				refinement_type rt = cs.search_refinement_type(result.class_object.type, result.refinement_type_clause.ident);
				if(rt!=null){
					rt.add_refinement_constraint(cs, result, result.get_Expr(cs));
				}else{
					throw new Exception("can't find refinement type " + result.refinement_type_clause.ident);
				}
			}
		}
		
		//�������
		BoolExpr post_invariant_expr = null;
		if(cd.class_block.invariants!=null&&cd.class_block.invariants.size()>0){
			for(invariant inv : cd.class_block.invariants){
				if(post_invariant_expr == null){
					post_invariant_expr = (BoolExpr) inv.check(cs);
				}else{
					post_invariant_expr = cs.ctx.mkAnd(post_invariant_expr, (BoolExpr)inv.check(cs));
				}
			}
			cs.add_constraint(post_invariant_expr);
		}
		BoolExpr ensures_expr = null;

		if(md.method_specification != null){
			ensures_expr = md.method_specification.ensures_expr(cs);
			cs.assert_constraint(ensures_expr);
		}
		
		
		return result;
	}

	void add_refinement_constraint(Check_status cs,Field f, Expr ex, boolean refined_class_condition, Expr class_Expr, Field class_Field, IntExpr class_Field_index) throws Exception{
	    System.out.println(f.field_name + " has refinement type");

	    //�o�b�N�A�b�v
	    Field refined_Field = cs.refined_Field;
	    Expr refined_Expr = cs.refined_Expr;
	    String refinement_type_value = cs.refinement_type_value;
	    boolean in_refinement_predicate = cs.in_refinement_predicate;
	    Expr refined_class_Expr = cs.refined_class_Expr;
	    Field refined_class_Field = cs.refined_class_Field;
	    IntExpr refined_class_Field_index = cs.refined_class_Field_index;
	    
	    //⿌^�̏����̂��߂̎��O����
	    if(cs.in_method_call){
	        cs.refined_class_Expr = cs.call_expr;
	        cs.refined_class_Field = cs.call_field;
	        cs.refined_class_Field_index = cs.call_field_index;
	    }
	    cs.refinement_deep++;
	    if(cs.refinement_deep <= cs.refinement_deep_limmit){
	        if(f.refinement_type_clause.refinement_type!=null){
	            f.refinement_type_clause.refinement_type.add_refinement_constraint(cs, f, ex);
	        }else if(f.refinement_type_clause.ident!=null){
	        	refinement_type rt = cs.search_refinement_type(f.class_object.type, f.refinement_type_clause.ident);
	            if(rt!=null){
	                rt.add_refinement_constraint(cs, f, ex);
	            }else{
	                throw new Exception("can't find refinement type " + f.refinement_type_clause.ident);
	            }
	        }
	    }else{
	        throw new Exception("The depth of refinement type's verification has reached its limit.");
	    }
	    
	    cs.refined_Expr = refined_Expr;
	    cs.refined_Field = refined_Field;
	    cs.refinement_type_value = refinement_type_value;
	    cs.in_refinement_predicate = in_refinement_predicate;
	    
	    cs.refined_class_Expr = refined_class_Expr;
	    cs.refined_class_Field = refined_class_Field;
	    cs.refined_class_Field_index = refined_class_Field_index;
	    cs.refinement_deep--;
	}
}

