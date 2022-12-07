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
import system.Model_Field;
import system.Pair;
import system.Parser;
import system.Parser_status;
import system.Quantifier_Variable;
import system.Source;
import system.Variable;
import system.F_Assign;

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
	
	public Check_return check(Check_status cs) throws Exception{
		Expr ex = null;
		
		Field f = null;
		String ident = null;
		boolean is_refine_value = false;
		

		//suffix�ɂ���
		List<IntExpr> indexs = new ArrayList<IntExpr>();
		if(cs.instance_indexs!=null) indexs = (ArrayList<IntExpr>) cs.instance_indexs.clone();
		
		
		if(this.primary_expr.is_this){
			if(this.primary_suffixs.size() == 0 && cs.in_constructor){//this�P�̂̃R���X�g���N�^�[�ł̎g�p
				cs.constructor_refinement_check();
			}
			f = cs.instance_Field;
			ex = cs.instance_expr;
			
			
		}else if(this.primary_expr.ident!=null){
			
			Quantifier_Variable quantifier = cs.search_quantifier(this.primary_expr.ident, cs);
			if(quantifier != null){//�ʉ��q�Ƃ��đ�������Ă��邩
				f = quantifier;
				ex = f.get_Expr(cs);
			}else if(cs.in_refinement_predicate==true && this.primary_expr.ident.equals(cs.refinement_type_value)){
				f = cs.refined_Field;
				ex = cs.refined_Expr;
				is_refine_value = true;
			}else{
				//���[�J���ϐ�
				if(cs.in_method_call){//�֐��Ăяo��
					if(cs.search_called_method_arg(primary_expr.ident)){
						f = cs.get_called_method_arg(primary_expr.ident);
						ex = f.get_Expr(cs);
					}
					
				}else if(cs.in_refinement_predicate){//⿌^
					if((cs.instance_Field==null||cs.instance_Field.equals(cs.this_field, cs))&&cs.search_variable(this.primary_expr.ident)){
						f = cs.get_variable(this.primary_expr.ident);
						ex = f.get_Expr(cs);
					}
					
				}else{
					if(cs.search_variable(primary_expr.ident)){
						f = cs.get_variable(primary_expr.ident);
						ex = f.get_Expr(cs);
						if(cs.in_postconditions && ((Variable)f).is_arg && f.dims==0){//���\�b�h�̈����ɂ͈Öق�old���t��
							f = cs.this_old_status.get_variable(primary_expr.ident);
							ex = f.get_Expr(cs.this_old_status);
						}
					}
				}
				
				if(f==null){//���[�J���ϐ��ł͂Ȃ��ꍇ
					Field searched_field = cs.search_field(primary_expr.ident, cs.instance_Field ,cs);
					if(searched_field != null){//�t�B�[���h
						f = searched_field;
						ex = cs.ctx.mkSelect((ArrayExpr) f.get_Expr(cs), cs.instance_expr);
						if(cs.can_not_use_mutable){
							if(!f.modifiers.is_final){
								new Exception("method depends on mutable field in refinenment type predicate.");
							}
						}
						if(f.modifiers.is_final){
							f.set_initialize(cs.instance_expr, cs);
						}
						
					}else{
						Model_Field searched_model_field = cs.search_model_field(primary_expr.ident, cs.instance_Field ,cs);
						if(searched_model_field != null){//model�t�B�[���h
							f = searched_model_field;
							ex = cs.ctx.mkSelect((ArrayExpr) ((Model_Field)f).get_Expr(cs.instance_expr, (ArrayList<IntExpr>) indexs, cs),cs.instance_expr);
							if(cs.can_not_use_mutable){
								new Exception("method depends on model field in refinenment type predicate.");
							}
						}else if(this.primary_suffixs.size() > 0 && this.primary_suffixs.get(0).is_method){//���\�b�h
							ident = this.primary_expr.ident;
							f = cs.instance_Field;
							ex = cs.instance_expr;
						}else{
							throw new Exception(cs.instance_Field.type + " don't have " + this.primary_expr.ident);
						}
					}
				}

				if(cs.in_refinement_predicate==true){
					if(f.modifiers.is_final==false || f instanceof Model_Field){//⿌^�̒��ł�final�ł���K�v������
						throw new Exception("can use only final variable in refinement type");
					}
				}
				//JML�߂ł̎g���Ȃ������̊m�F
				if(cs.ban_default_visibility){
					if(f.modifiers!=null&&f.modifiers.is_private==false){
						throw new Exception("can not use default visibility variable");
					}
				}
				if(cs.ban_private_visibility){
					if(f.modifiers!=null&&f.modifiers.is_private==true){
						throw new Exception("can not use private visibility variable");
					}
				}
			}
		}else if(this.primary_expr.bracket_expression!=null){
			Check_return cr = this.primary_expr.bracket_expression.check(cs);
			f = cr.field;
			ex = cr.expr;
			indexs = cr.indexs;
		}else if(this.primary_expr.java_literal!=null){
			if(this.primary_suffixs.size() == 0){
				ex = this.primary_expr.java_literal.check(cs);
			}else{
				throw new Exception("literal don't have suffix");
				
			}
		}else if(this.primary_expr.jml_primary!=null){
			if(cs.in_method_call){
				if(this.primary_expr.jml_primary.is_result){
					f = cs.result;
					ex = f.get_Expr(cs);
				}else if(this.primary_expr.jml_primary.old_expression!=null){
					Check_return cr = this.primary_expr.jml_primary.old_expression.spec_expression.check(cs.old_status);
					f = cr.field;
					ex = cr.expr;
					indexs = cr.indexs;
				}else if(this.primary_expr.jml_primary.spec_quantified_expr!=null){
					ex = this.primary_expr.jml_primary.spec_quantified_expr.check(cs);
					if(this.primary_suffixs.size() != 0) throw new Exception("quantifier don't have suffix");
				}
			}else{
				if(this.primary_expr.jml_primary.is_result){
					f = cs.return_v;
					ex = cs.return_expr;
					//return ex;
				}else if(this.primary_expr.jml_primary.old_expression!=null){
					Check_return cr = this.primary_expr.jml_primary.old_expression.spec_expression.check(cs.this_old_status);
					f = cr.field;
					ex = cr.expr;
					indexs = cr.indexs;
				}else if(this.primary_expr.jml_primary.spec_quantified_expr!=null){
					ex = this.primary_expr.jml_primary.spec_quantified_expr.check(cs);
					if(this.primary_suffixs.size() != 0) throw new Exception("quantifier don't have suffix");
				}
			}
		}else if(this.primary_expr.new_expr!=null){
			f = this.primary_expr.new_expr.check(cs);
			ex = f.get_Expr(cs);
		}else{
			//return null;
		}
		
		if(f!=null && f.hava_refinement_type() && is_refine_value==false){//⿌^			
			add_refinement_constraint(cs, f, ex, cs.instance_Field, cs.instance_Field.get_Expr(cs), cs.instance_indexs);
		}
		
		
		
		for(int i = 0; i < this.primary_suffixs.size(); i++){
			primary_suffix ps = this.primary_suffixs.get(i);
			if(ps.is_field){
				if(this.primary_suffixs.size() > i+1 && this.primary_suffixs.get(i+1).is_method){
					ident = ps.ident;
				}else if(f.dims_sum() > indexs.size() && ps.ident.equals("length")){
					if(this.primary_suffixs.size()-1 != i) throw new Exception("length don't have suffix");
					int array_dim = f.dims_sum() - indexs.size();
					String array_type;
					if(f.type.equals("int")){
						array_type = "int";
					}else if(f.type.equals("boolean")){
						array_type = "boolean";
					}else{
						array_type = "ref";
					}
					ex = (IntExpr) cs.ctx.mkSelect(cs.ctx.mkArrayConst("length_" + array_dim + "d_" + array_type, ex.getSort(), cs.ctx.mkIntSort()), ex);
					f = null;
				}else{
					Field pre_f = f;
					Expr pre_ex = ex;
					
					Field searched_field = cs.search_field(ps.ident, f, cs);
					if(searched_field != null){
						f = searched_field;
						ex = cs.ctx.mkSelect((ArrayExpr)f.get_Expr(cs), ex);
						if(cs.can_not_use_mutable){
							if(!f.modifiers.is_final){
								new Exception("method depends on mutable field in refinenment type predicate.");
							}
						}

						if(f.modifiers.is_final){
							f.set_initialize(pre_ex, cs);
						}
					}else{
						Model_Field searched_model_field = cs.search_model_field(ps.ident, f, cs);
						if(searched_model_field != null){
							f = searched_model_field;
							ex = cs.ctx.mkSelect((ArrayExpr) ((Model_Field)f).get_Expr(ex, (ArrayList<IntExpr>) indexs, cs), ex);
							if(cs.can_not_use_mutable){
								new Exception("method depends on model field in refinenment type predicate.");
							}
						}else{
							throw new Exception(f.type + " don't have " + ps.ident);
						}
					}
					
					if(f.hava_refinement_type()){//⿌^
						add_refinement_constraint(cs, f, ex, pre_f, pre_ex, (ArrayList<IntExpr>) indexs);
					}
					
					if(cs.in_refinement_predicate==true){//⿌^�̒��ł�final�ł���K�v������
						if(f.modifiers.is_final==false || f instanceof Model_Field){
							throw new Exception("can use only final variable in refinement type");
						}
					}
					
					//JML�߂ł̎g���Ȃ������̊m�F
					if(cs.ban_default_visibility){
						if(f.modifiers!=null&&f.modifiers.is_private==false){
							throw new Exception("can not use default visibility variable");
						}
					}
					if(cs.ban_private_visibility){
						if(f.modifiers!=null&&f.modifiers.is_private==true){
							throw new Exception("can not use private visibility variable");
						}
					}
				}
				
			}else if(ps.is_index){
				IntExpr index =  (IntExpr) ps.expression.check(cs).expr;
				//�z���bound check
				int array_dim = f.dims_sum() - indexs.size();
				String array_type;
				if(f.type.equals("int")){
					array_type = "int";
				}else if(f.type.equals("boolean")){
					array_type = "boolean";
				}else{
					array_type = "ref";
				}
				IntExpr length = (IntExpr) cs.ctx.mkSelect(cs.ctx.mkArrayConst("length_" + array_dim + "d_" + array_type, ex.getSort(), cs.ctx.mkIntSort()), ex);
				
				BoolExpr index_bound = cs.ctx.mkGe(index, cs.ctx.mkInt(0));
				
				index_bound = cs.ctx.mkAnd(index_bound, cs.ctx.mkGt(length, index));
				
				
				if(!cs.in_jml_predicate){
					System.out.println("check index out of bounds");
					cs.assert_constraint(index_bound);
				}
				
				indexs.add(index);
				
				ex = cs.ctx.mkSelect((ArrayExpr) ex, index);
				ident = null;
				if(cs.in_refinement_predicate==true){//⿌^�̒��ł͔z��͎g���Ȃ�
					if(!f.equals(cs.refined_Field)){
						if(cs.search_quantifier(f.field_name, cs)==null){
							throw new Exception("can not use array in refinement type");
						}
					}
				}
				
			}else if(ps.is_method){
				//�֐��̌Ăяo��
				f = method(cs, ident, f, ex, (ArrayList<IntExpr>) indexs, ps);
				ex = f.get_Expr(cs);
				ident = null;
			}
		}

		
		return new Check_return(ex, f, (ArrayList<IntExpr>) indexs);
	}
	
	public Field check_assign(Check_status cs) throws Exception{
		Expr ex = null;
		Field f = null;
		ArrayList<IntExpr> indexs = new ArrayList<IntExpr>();
		
		if(this.primary_suffixs.size() == 0){
			if(this.primary_expr.is_this){
				//����̓A�E�g
				throw new Exception("can't assign this");
			}if(this.primary_expr.ident!=null){

				Field searched_field = cs.search_field(primary_expr.ident, cs.this_field, cs);
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
			f.index = indexs;
			return f;

		}else{

			String ident = null;
			if(this.primary_expr.is_this){
				f = cs.this_field;
				ex = f.get_Expr(cs);
			}else if(this.primary_expr.ident!=null){

				Field searched_field = cs.search_field(primary_expr.ident, cs.this_field, cs);
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
				Check_return cr = this.primary_expr.bracket_expression.check(cs);
				f = cr.field;
				ex = cr.expr;
				indexs = cr.indexs;
			}else if(this.primary_expr.java_literal!=null){
				//System.out.println("literal dont have suffix");
				throw new Exception("literal dont have suffix");
			}else{
				//new expr
				return null;
			}
			//suffix�ɂ���
			for(int i = 0; i < this.primary_suffixs.size(); i++){
				primary_suffix ps = this.primary_suffixs.get(i);
				if(ps.is_field){
					if(this.primary_suffixs.size() > i+1 && this.primary_suffixs.get(i+1).is_method){
						ident = ps.ident;
					}else{
						Field searched_field = cs.search_field(ps.ident, f, cs);
						if(searched_field != null){
							f = searched_field;
							f.class_object_expr = ex;
							ex = cs.ctx.mkSelect((ArrayExpr)f.get_Expr(cs), ex);
						}else{
							throw new Exception(f.type + " don't have " + ps.ident);
						}

					}
					
					
				}else if(ps.is_index){
					IntExpr index = (IntExpr)ps.expression.check(cs).expr;

					//�z���bound check
					int array_dim = f.dims_sum() - indexs.size();
					String array_type;
					if(f.type.equals("int")){
						array_type = "int";
					}else if(f.type.equals("boolean")){
						array_type = "boolean";
					}else{
						array_type = "ref";
					}
					IntExpr length = (IntExpr) cs.ctx.mkSelect(cs.ctx.mkArrayConst("length_" + array_dim + "d_" + array_type, ex.getSort(), cs.ctx.mkIntSort()), ex);
					
					BoolExpr index_bound = cs.ctx.mkGe(index, cs.ctx.mkInt(0));
					
					index_bound = cs.ctx.mkAnd(index_bound, cs.ctx.mkGt(length, index));
					
					System.out.println("check index out of bounds");
					if(!cs.in_jml_predicate)cs.assert_constraint(index_bound);
					
					
					
					
					indexs.add(index);
					
					ex = cs.ctx.mkSelect((ArrayExpr) ex, index);
					ident = null;
				}else if(ps.is_method){
					//�֐��̌Ăяo��
					if(i == this.primary_suffixs.size()-1){
						throw new Exception("The left-hand side of an assignment must be a variable");
					}
					f = method(cs, ident, f, ex, indexs, ps);
					ex = f.get_Expr(cs);
					cs.in_method_call = false;
					ident = null;
				}
			}
			f.index = indexs;
			return f;
		}
	}

	
	
	public Field method(Check_status cs, String ident, Field f, Expr ex, ArrayList<IntExpr> indexs, primary_suffix ps)throws Exception{
		
		System.out.println("call method " + ident);
		
		boolean pre_can_not_use_mutable = cs.can_not_use_mutable;
		if(cs.in_refinement_predicate) cs.can_not_use_mutable = true;
		boolean pre_in_refinement_predicate = cs.in_refinement_predicate;
		cs.in_refinement_predicate = false;
		
		
		class_declaration cd = cs.Check_status_share.compilation_unit.search_class(f.type);
		method_decl md = cs.Check_status_share.compilation_unit.search_method(f.type, ident);
		if(md == null){
			throw new Exception("can't find method " + ident);
		}
		
		if(md.formals.param_declarations.size()!=ps.expression_list.expressions.size()){
			throw new Exception("wrong number of arguments");
		}
		
		if(cs.in_jml_predicate && !md.modifiers.is_pure){
			throw new Exception("non pure method in jml predicate");
		}
		if(cs.use_only_helper_method && !md.modifiers.is_helper){
			throw new Exception("non helper method in invarinat or in refinement type");
		}
		
		//�R���X�g���N�^�ł̎��C���X�^���X�̊֐��Ăяo��
		if(cs.in_constructor && f.equals(cs.this_field, cs) && !md.modifiers.is_helper){
			cs.constructor_refinement_check();
		}
		
		
		//helper���\�b�h�̒���helper�łȂ����\�b�h���Ă񂾏ꍇ�A�S�Ă�⿌^�����؂���
		if(cs.in_helper && !md.modifiers.is_helper){
			cs.assert_all_refinement_type();
		}
		
		
		
		//�����̏���
		List<Check_return> method_arg_valuse = new ArrayList<Check_return>();
		for(int j = 0; j < md.formals.param_declarations.size(); j++){
			method_arg_valuse.add(ps.expression_list.expressions.get(j).check(cs));
		}
		//�֐����̏���
		cs.in_method_call = true;
		
		cs.called_method_args = new ArrayList<Variable>();
		
		Expr pre_instance_expr = cs.instance_expr;
		Field pre_instance_Field = cs.instance_Field;
		ArrayList<IntExpr> pre_instance_indexs = cs.instance_indexs;
		
		cs.instance_expr = ex;
		cs.instance_Field = f;
		cs.instance_indexs = (ArrayList<IntExpr>) indexs.clone();
		
		
		
		
		for(int j = 0; j < md.formals.param_declarations.size(); j++){
			param_declaration pd = md.formals.param_declarations.get(j);
			modifiers m = new modifiers();
			m.is_final = pd.is_final;
			Variable v = new Variable(cs.Check_status_share.get_tmp_num(), pd.ident, pd.type_spec.type.type, pd.type_spec.dims, pd.type_spec.refinement_type_clause, m, f, cs.ctx.mkBool(true));
			cs.called_method_args.add(v);
			v.temp_num = 0;
			v.alias = cs.ctx.mkBool(true);
			//�����ɒl��R�Â���
			cs.add_constraint(cs.ctx.mkEq(v.get_Expr(cs), method_arg_valuse.get(j).expr));
			if(!((v.type.equals("int") || v.type.equals("boolean")) && v.dims==0))v.arg_field =  method_arg_valuse.get(j).field;
			if(method_arg_valuse.get(j).field!=null) v.internal_id = method_arg_valuse.get(j).field.internal_id;
			
			//�z���⿌^�����S���ǂ���
			Expr method_arg_assign_field_expr = null;
			Expr method_arg_class_field_expr = null;
			if(method_arg_valuse.get(j).field!=null){
				method_arg_assign_field_expr = method_arg_valuse.get(j).field.get_full_Expr(new ArrayList<IntExpr>(method_arg_valuse.get(j).indexs.subList(0, method_arg_valuse.get(j).field.class_object_dims_sum())), cs);
				method_arg_class_field_expr = method_arg_valuse.get(j).field.class_object.get_full_Expr((ArrayList<IntExpr>) method_arg_valuse.get(j).indexs.clone(), cs);
			}
			cs.check_array_alias(v, v.get_Expr(cs), ex, indexs, method_arg_valuse.get(j).field, method_arg_assign_field_expr, method_arg_class_field_expr, method_arg_valuse.get(j).indexs);
			
			
			//⿌^�̌���
			if(v.hava_refinement_type()){
				v.assert_refinement(cs, ex, indexs);
			}
		}
		
		//���O����
		System.out.println("method call pre invarinats");
		BoolExpr pre_invariant_expr = null;
		if(cd.class_block.invariants!=null&&cd.class_block.invariants.size()>0 && !md.modifiers.is_helper){
			for(invariant inv : cd.class_block.invariants){
				if(pre_invariant_expr == null){
					pre_invariant_expr = (BoolExpr) inv.check(cs);
				}else{
					pre_invariant_expr = cs.ctx.mkAnd(pre_invariant_expr, (BoolExpr)inv.check(cs));
				}
			}
			cs.assert_constraint(pre_invariant_expr);
		}
		BoolExpr require_expr = null;

		System.out.println("method call requires");
		if(md.method_specification != null){
			require_expr = md.method_specification.requires_expr(cs);
			cs.assert_constraint(require_expr);
		}
		
		//old data
		Check_status csc = cs.clone();
		csc.clone_list();
		cs.old_status = csc;
		
		//assign
		System.out.println("method call assign");
		if(md.modifiers.is_pure){
			//�������Ȃ�
		}else if(md.method_specification != null){
			
			Pair<List<F_Assign>, BoolExpr> assign_cnsts = md.method_specification.assignables(cs);
			
			//helper���\�b�h�A�R���X�g���N�^�ł́A�z��͑���O�Ɍ��؂��K�v�ȏꍇ������
			for(F_Assign fa : assign_cnsts.fst){
				for(Pair<BoolExpr,List<List<IntExpr>>> b_indexs : fa.cnst_array){
					for(List<IntExpr> assign_indexs : b_indexs.snd){
						if(assign_indexs.size() < fa.field.dims_sum()){
							check_array_assign_in_helper_or_constructor(fa.field, assign_indexs, b_indexs.fst, cs.in_helper, cs.in_constructor, cs);
						}
					}
				}
			}
			//���ł�����ł���ꍇ
			for(Field field : cs.fields){
				check_array_assign_in_helper_or_constructor(field, field.class_object.fresh_index_full_expr(cs).snd, assign_cnsts.snd, cs.in_helper, cs.in_constructor, cs);
			}
			for(Variable variable : cs.called_method_args){//����
				Field field = null;
				if(variable.arg_field != null){
					field = variable.arg_field;
				}else{
					field = variable;
				}
				if(field instanceof Variable && field.dims > 0){//cs.fields�ɖ������� �@�@�@�@this��Variable�̃C���X�^���X
					check_array_assign_in_helper_or_constructor(field, new ArrayList<IntExpr>(), assign_cnsts.snd, cs.in_helper, cs.in_constructor, cs);
				}
			}
			
			
			
			for(F_Assign fa : assign_cnsts.fst){

				BoolExpr assign_expr = null;
				
				//�����̃t�B�[���h�ɑ������ꍇ�ɂ́A�R�Â���ꂽ�t�B�[���h�ɑ������
				if(fa.field instanceof Variable && ((Variable)fa.field).arg_field!=null && !((fa.field.type.equals("int") || fa.field.type.equals("boolean")) && fa.field.dims==0)){
					fa.field = ((Variable)fa.field).arg_field;
				}
				
				//�z��̗v�f�ɑ���ł��鐧��
				if(fa.cnst_array.size()>0){
					for(int i = 0; i <= fa.field.dims; i++){//�e�����Ɋւ���
						List<IntExpr> index_expr = new ArrayList<IntExpr>();
						for(int j = 0; j < i; j++){
							index_expr.add(cs.ctx.mkIntConst("tmpIdex" + cs.Check_status_share.tmp_num));
						}
						BoolExpr call_assign_expr = fa.assign_index_expr(index_expr, cs);
						BoolExpr field_assign_expr = fa.field.assign_index_expr(index_expr, cs);
						
						if(assign_expr == null){
							assign_expr = cs.ctx.mkImplies(call_assign_expr, field_assign_expr);
						}else{
							assign_expr = cs.ctx.mkAnd(assign_expr, cs.ctx.mkImplies(call_assign_expr, field_assign_expr));
						}
					}
				}else{
					assign_expr = cs.ctx.mkBool(true);
				}
				//���ł�������Ă���
				assign_expr = cs.ctx.mkOr(assign_expr, cs.assinable_cnst_all);
				
				System.out.println("check assign");
				cs.assert_constraint(assign_expr);
				
				
				
				//���ۂɑ�����鐧���ǉ�����
				System.out.println("assign " + fa.field.field_name);
				
				
				//�z��̗v�f�ɑ��
				//���̃t�B�[���h�ɂ��ǂ蒅���܂łɔz��A�N�Z�X�����Ă����ꍇ��
				if(fa.cnst_array.size()>0){
					fa.assign_fresh_value(cs);
					
				}
				
				
			}
			
			
			//assign_cnsts.fst�Ɋ܂܂�Ȃ����̂́Aassign_cnsts.snd�̂Ƃ��ɑ�����s��
			cs.assert_constraint(cs.ctx.mkImplies(assign_cnsts.snd, cs.assinable_cnst_all));
			for(Field field : cs.fields){
				cs.add_constraint(cs.ctx.mkImplies(cs.ctx.mkNot(assign_cnsts.snd), cs.ctx.mkEq(field.get_Expr(cs), field.get_Expr_assign(cs))));
				field.tmp_plus_with_data_group(assign_cnsts.snd, cs);
			}
			for(Variable variable : cs.called_method_args){//����
				Field field = null;
				if(variable.arg_field != null){
					field = variable.arg_field;
				}else{
					field = variable;
				}
				if(field instanceof Variable && field.dims > 0){//cs.fields�ɖ������� �@�@�@�@this��Variable�̃C���X�^���X
					cs.add_constraint(cs.ctx.mkImplies(cs.ctx.mkNot(assign_cnsts.snd), cs.ctx.mkEq(field.get_Expr(cs), field.get_Expr_assign(cs))));
					field.tmp_plus(cs);
				}
			}
			
		}else{//assignable���܂߂��C�ӂ̎d�l��������Ă��Ȃ��֐�
			
			//helper���\�b�h�A�R���X�g���N�^�ł́A����O�Ɍ��؂��K�v�ȏꍇ������
			//���ł�����ł���ꍇ
			for(Field field : cs.fields){
				check_array_assign_in_helper_or_constructor(field, field.class_object.fresh_index_full_expr(cs).snd, cs.ctx.mkBool(true), cs.in_helper, cs.in_constructor, cs);
			}
			for(Variable variable : cs.called_method_args){//����
				Field field = null;
				if(variable.arg_field != null){
					field = variable.arg_field;
				}else{
					field = variable;
				}
				if(field instanceof Variable && field.dims > 0){//cs.fields�ɖ������� �@�@�@�@this��Variable�̃C���X�^���X
					check_array_assign_in_helper_or_constructor(field, new ArrayList<IntExpr>(), cs.ctx.mkBool(true), cs.in_helper, cs.in_constructor, cs);
				}
			}

			
			for(Field f_a : cs.fields){
				f_a.tmp_plus_with_data_group(cs);
			}
			for(Variable variable : cs.called_method_args){//����
				Field field = null;
				if(variable.arg_field != null){
					field = variable.arg_field;
				}else{
					field = variable;
				}
				if(field instanceof Variable && field.dims > 0){//cs.fields�ɖ������� �@�@�@�@this��Variable�̃C���X�^���X
					field.tmp_plus(cs);
				}
			}
			
		}
		
		
		
		//helper���\�b�h��R���X�g���N�^�[�ɂ�����z��̃G�C���A�X
		update_alias_in_helper_or_constructor(9999999, cs.get_pathcondition(), cs, cs.in_helper, cs.in_constructor);
				
		
		//�Ԃ�l
		modifiers m_tmp = new modifiers();
		Variable result = new Variable(cs.Check_status_share.get_tmp_num(), "return_tmp", md.type_spec.type.type, md.type_spec.dims, md.type_spec.refinement_type_clause, m_tmp, f, cs.ctx.mkBool(true));
		result.alias = cs.ctx.mkBool(true); //�����̓G�C���A�X���Ă���\��������B
		result.temp_num++;
		cs.result = result;
		if(result.hava_refinement_type()){
			result.add_refinement_constraint(cs, ex, indexs, true);
		}
		
		//�������
		System.out.println("method call post invarinats");
		BoolExpr post_invariant_expr = null;
		if(cd.class_block.invariants!=null&&cd.class_block.invariants.size()>0 && !md.modifiers.is_helper){
			for(invariant inv : cd.class_block.invariants){
				if(post_invariant_expr == null){
					post_invariant_expr = (BoolExpr) inv.check(cs);
				}else{
					post_invariant_expr = cs.ctx.mkAnd(post_invariant_expr, (BoolExpr)inv.check(cs));
				}
			}
			cs.add_constraint(post_invariant_expr);
		}
		
		System.out.println("method call ensures");
		BoolExpr ensures_expr = null;
		
		if(md.method_specification != null){
			ensures_expr = md.method_specification.ensures_expr(cs);
			cs.add_constraint(ensures_expr);
		}

		cs.in_method_call = false;
		
		cs.instance_expr = pre_instance_expr;
		cs.instance_Field = pre_instance_Field;
		cs.instance_indexs = pre_instance_indexs;
		
		cs.can_not_use_mutable = pre_can_not_use_mutable;
		cs.in_refinement_predicate = pre_in_refinement_predicate;
		
		
		return result;
	}

	void add_refinement_constraint(Check_status cs,Field f, Expr ex, Field class_Field, Expr class_Expr, ArrayList<IntExpr> indexs) throws Exception{
	    System.out.println(f.field_name + " has refinement type");

	    
	        
	    cs.refinement_deep++;
	    if(cs.refinement_deep <= cs.refinement_deep_limmit){
	        f.add_refinement_constraint(cs, class_Expr, indexs);
	    }else{
	        throw new Exception("The depth of refinement type's verification has reached its limit.");
	    }
	    
	    
	    cs.refinement_deep--;
	}
	
	public boolean have_index_access(Check_status cs){
		boolean have = primary_expr.have_index_access(cs);
		for(primary_suffix ps : primary_suffixs){
			have = have || ps.have_index_access(cs);
		}
		return have;
	}
	
	public Check_return loop_assign(Pair<List<Pair<Field,List<List<IntExpr>>>>,Boolean>assigned_fields, Check_status cs) throws Exception{
		
		Field f = null;
		Expr ex = null;
		String ident = null;
		boolean is_refine_value = false;
		

		//suffix�ɂ���
		List<IntExpr> indexs = new ArrayList<IntExpr>();
		if(cs.instance_indexs!=null) indexs = (ArrayList<IntExpr>) cs.instance_indexs.clone();
		
		
		if(this.primary_expr.is_this){
			f = cs.instance_Field;
			ex = cs.instance_expr;
		}else if(this.primary_expr.ident!=null){
			//���[�J���ϐ�
			if(cs.in_method_call){//�֐��Ăяo��
				if(cs.search_called_method_arg(primary_expr.ident)){
					f = cs.get_called_method_arg(primary_expr.ident);
					ex = f.get_Expr(cs);
				}
				
			}else{
				if(cs.search_variable(primary_expr.ident)){
					f = cs.get_variable(primary_expr.ident);
					ex = f.get_Expr(cs);
				}
			}
			
			if(f==null){//���[�J���ϐ��ł͂Ȃ��ꍇ
				Field searched_field = cs.search_field(primary_expr.ident, cs.instance_Field ,cs);
				if(searched_field != null){//�t�B�[���h
					f = searched_field;
					ex = cs.ctx.mkSelect((ArrayExpr) f.get_Expr(cs),cs.instance_expr);
				}else if(this.primary_suffixs.size() > 0 && this.primary_suffixs.get(0).is_method){//���\�b�h
					ident = this.primary_expr.ident;
					f = cs.instance_Field;
					ex = cs.instance_expr;
				}else{
					throw new Exception(cs.instance_Field.type + " don't have " + this.primary_expr.ident);
				}
			}

			
		}else if(this.primary_expr.bracket_expression!=null){
			Check_return cr = this.primary_expr.bracket_expression.loop_assign(assigned_fields, cs);
			f = cr.field;
			ex = cr.expr;
			indexs = cr.indexs;
		}else if(this.primary_expr.java_literal!=null){
			if(this.primary_suffixs.size() == 0){
				ex = this.primary_expr.java_literal.check(cs);
			}else{
				throw new Exception("literal don't have suffix");
				
			}
		}else if(this.primary_expr.jml_primary!=null){
			//jml���͌��Ȃ�
		}else if(this.primary_expr.new_expr!=null){
			//�V�������I�u�W�F�N�g���֌W�Ȃ�
		}else{
			//return null;
		}
		
		for(int i = 0; i < this.primary_suffixs.size(); i++){
			primary_suffix ps = this.primary_suffixs.get(i);
			if(ps.is_field){
				if(this.primary_suffixs.size() > i+1 && this.primary_suffixs.get(i+1).is_method){
					ident = ps.ident;
				}else if(f.dims_sum() > indexs.size() && ps.ident.equals("length")){
					if(this.primary_suffixs.size()-1 != i) throw new Exception("length don't have suffix");
					f = null;
				}else{
					Field pre_f = f;
					Expr pre_ex = ex;
					
					Field searched_field = cs.search_field(ps.ident, f, cs);
					if(searched_field != null){
						f = searched_field;
						ex = cs.ctx.mkSelect((ArrayExpr)f.get_Expr(cs), ex);
						if(cs.can_not_use_mutable){
							if(!f.modifiers.is_final){
								new Exception("method depends on mutable field in refinenment type predicate.");
							}
						}
					}else{
						throw new Exception(f.type + " don't have " + ps.ident);
					}
				}
				
			}else if(ps.is_index){
				IntExpr index =  (IntExpr) ps.expression.loop_assign(assigned_fields, cs).expr;
				
				indexs.add(index);
				
				ex = cs.ctx.mkSelect((ArrayExpr) ex, index);
				ident = null;
				
			}else if(ps.is_method){
				//�֐��̌Ăяo��
				f = loop_assign_method(assigned_fields, cs, ident, f, ex, (ArrayList<IntExpr>) indexs, ps);
				ex = f.get_Expr(cs);
				ident = null;
			}
		}

		
		return new Check_return(ex, f, (ArrayList<IntExpr>) indexs);
	}
	
	public Field loop_assign_method(Pair<List<Pair<Field,List<List<IntExpr>>>>,Boolean>assigned_fields, Check_status cs, String ident, Field f, Expr ex, ArrayList<IntExpr> indexs, primary_suffix ps)throws Exception{
		
		
		class_declaration cd = cs.Check_status_share.compilation_unit.search_class(f.type);
		method_decl md = cs.Check_status_share.compilation_unit.search_method(f.type, ident);
		if(md == null){
			throw new Exception("can't find method " + ident);
		}
		
		
		//�����̏���
		List<Check_return> method_arg_valuse = new ArrayList<Check_return>();
		for(int j = 0; j < md.formals.param_declarations.size(); j++){
			method_arg_valuse.add(ps.expression_list.expressions.get(j).check(cs));
		}
		
		//�֐����̏���
		cs.in_method_call = true;
		
		cs.called_method_args = new ArrayList<Variable>();
		
		Expr pre_instance_expr = cs.instance_expr;
		Field pre_instance_Field = cs.instance_Field;
		ArrayList<IntExpr> pre_instance_indexs = cs.instance_indexs;
		
		cs.instance_expr = ex;
		cs.instance_Field = f;
		cs.instance_indexs = (ArrayList<IntExpr>) indexs.clone();
		
		for(int j = 0; j < md.formals.param_declarations.size(); j++){
			param_declaration pd = md.formals.param_declarations.get(j);
			modifiers m = new modifiers();
			m.is_final = pd.is_final;
			Variable v = new Variable(cs.Check_status_share.get_tmp_num(), pd.ident, pd.type_spec.type.type, pd.type_spec.dims, pd.type_spec.refinement_type_clause, m, f, cs.ctx.mkBool(true));
			cs.called_method_args.add(v);
			v.temp_num = 0;
			//�����ɒl��R�Â���
			cs.add_constraint(cs.ctx.mkEq(v.get_Expr(cs), method_arg_valuse.get(j).expr));
			if(!((v.type.equals("int") || v.type.equals("boolean")) && v.dims==0))v.arg_field =  method_arg_valuse.get(j).field;
			if(method_arg_valuse.get(j).field!=null) v.internal_id = method_arg_valuse.get(j).field.internal_id;
			
		}
		
		
		//assign
		if(md.method_specification != null){
			
			for(generic_spec_case gsc : md.method_specification.spec_case_seq.generic_spec_cases){
				BoolExpr require_expr = gsc.requires_expr(cs);
				List<assignable_clause> assignables = gsc.get_assignable();
				if(assignables == null){//���ł�������Ă���
					assigned_fields.snd = true;
				}else{
					for(assignable_clause ac : assignables){
						
						for(store_ref_expression sre : ac.store_ref_list.store_ref_expressions){
							Pair<Field, List<IntExpr>> f_indexs = sre.check(cs);
							
							boolean find_field = false;
							for(Pair<Field,List<List<IntExpr>>> f_i : assigned_fields.fst){
								if(f_i.fst == f_indexs.fst){//����������ǉ�����
									find_field = true;
									f_i.snd.add(f_indexs.snd);
									break;
								}
							}
							//������Ȃ�������V�����t�B�[���h���ƒǉ�����
							if(!find_field){
								List<List<IntExpr>> f_indexs_snd = new ArrayList<List<IntExpr>>();
								f_indexs_snd.add(f_indexs.snd);
								Pair<Field,List<List<IntExpr>>> f_i = new Pair<Field,List<List<IntExpr>>>(f_indexs.fst, f_indexs_snd);
								assigned_fields.fst.add(f_i);
							}
						}
					}
				}
			}
			
			
			
		}else{
			//assignable���܂߂��C�ӂ̎d�l��������Ă��Ȃ��֐�
			assigned_fields.snd = true;
		}
		
		//helper���\�b�h��R���X�g���N�^�[�ɂ�����z��̃G�C���A�X
		//loop_assign_method�ł�pre_in_helper�Ȃǂ͗p�ӂ��Ȃ�
		update_alias_in_helper_or_constructor(9999999, cs.get_pathcondition(), cs, cs.in_helper, cs.in_constructor);

		
		
		//�Ԃ�l
		modifiers m_tmp = new modifiers();
		Variable result = new Variable(cs.Check_status_share.get_tmp_num(), "return_tmp", md.type_spec.type.type, md.type_spec.dims, md.type_spec.refinement_type_clause, m_tmp, f, cs.ctx.mkBool(true));
		result.temp_num++;
		
		cs.in_method_call = false;
		
		cs.instance_expr = pre_instance_expr;
		cs.instance_Field = pre_instance_Field;
		cs.instance_indexs = pre_instance_indexs;
		
		return result;
	}
	
	//���\�b�h�Ăяo���̍ۂ�alias_in_helper_or_consutructor��alias_2d_in_helper_or_consutructor���X�V����
	//cs.in_helper�Ƃ��͌Ăяo���Ă��郁�\�b�h�̂��́@�Ăяo�����̃��\�b�h���ǂ��Ȃ̂����K�v�Ȃ̂ň����Ƃ��ēn��
	public void update_alias_in_helper_or_constructor(int dim, BoolExpr condition, Check_status cs, boolean in_helper, boolean in_constructor){
		//1�����ȏ�̔z��Ƃ��ăG�C���A�X�����ꍇ�ɂ́A����ȍ~�z���������O��⿌^�̌��؂��s��Ȃ���΂Ȃ�Ȃ�
		if(1 <= dim){
			if(in_helper){
				for(Variable v : cs.called_method_args){
					if(v.arg_field!=null && v.arg_field instanceof Variable && v.dims>=1 && v.hava_refinement_type()){
						v.arg_field.alias_in_helper_or_consutructor = cs.ctx.mkOr(v.arg_field.alias_in_helper_or_consutructor, condition);
					}
				}
			}else if(in_constructor){
				for(Field v : cs.fields){
					if(v.class_object != null && v.class_object.equals(cs.this_field, cs) && v.dims>=1 && v.hava_refinement_type()){
						v.alias_in_helper_or_consutructor = cs.ctx.mkOr(v.alias_in_helper_or_consutructor, condition);
					}
				}
			}
		}
		//2�����ȏ�̔z��Ƃ��ăG�C���A�X�����ꍇ�ɂ́A����ȍ~⿌^�𖞂����Ȃ���΂����Ȃ�
		if(2 <= dim){
			if(in_helper){
				for(Variable v : cs.called_method_args){
					if(v.arg_field!=null && v.arg_field instanceof Variable && v.dims>=2 && v.hava_refinement_type()){
						v.arg_field.alias_2d_in_helper_or_consutructor = cs.ctx.mkOr(v.arg_field.alias_2d_in_helper_or_consutructor, condition);
					}
				}
			}else if(in_constructor){
				for(Field v : cs.fields){
					if(v.class_object != null && v.class_object.equals(cs.this_field, cs) && v.dims>=2 && v.hava_refinement_type()){
						v.alias_2d_in_helper_or_consutructor = cs.ctx.mkOr(v.alias_2d_in_helper_or_consutructor, condition);
					}
				}
			}
		}
	}
	
	//helper���\�b�h��R���X�g���N�^�ŁA���\�b�h�Ăяo���ɂ����āA�z���������\��������ꍇ��⿌^�̃`�F�b�N
	public void check_array_assign_in_helper_or_constructor(Field field, List<IntExpr> indexs, BoolExpr condition, boolean in_helper, boolean in_constructor, Check_status cs) throws Exception{
		if(field.dims >= 1 && field.hava_refinement_type() && field.have_index_access(cs) 
				&& (cs.in_helper || (cs.in_constructor && !(field instanceof Variable) && field.class_object != null && field.class_object.equals(cs.this_field, cs)))){
			cs.solver.push();
	
			//�G�C���A�X���Ă���Ƃ������ł���
			cs.add_constraint(field.alias_in_helper_or_consutructor);
			
			cs.add_constraint(condition);
			Field v = field;
			Field old_v = cs.this_old_status.search_internal_id(v.internal_id);
			
			Expr v_class_object_expr = v.class_object.get_full_Expr(new ArrayList<IntExpr>(indexs), cs);

			Expr old_assign_field_expr = null;
			Expr assign_field_expr = null;
			if(v instanceof Variable){
				assign_field_expr = v.get_Expr(cs);
			}else{
				old_assign_field_expr = cs.ctx.mkSelect(old_v.get_Expr(cs.this_old_status), v_class_object_expr);
				assign_field_expr = cs.ctx.mkSelect(v.get_Expr(cs), v_class_object_expr);
			}
			
			//���\�b�h�̍ŏ��ł�⿌^���������Ă��邱�Ƃ����肵�Ă���
			//�t�B�[���h����
			if(in_helper && !(v instanceof Variable)){
				old_v.add_refinement_constraint(cs.this_old_status, v_class_object_expr, new ArrayList<IntExpr>(indexs.subList(0, v.class_object_dims_sum())), true);
			}
			
			v.assert_refinement(cs, v_class_object_expr, new ArrayList<IntExpr>(indexs.subList(0, v.class_object_dims_sum())));
			
			cs.solver.pop();
		}
	}

	
}

