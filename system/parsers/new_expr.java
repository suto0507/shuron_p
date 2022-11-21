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
import system.Pair;
import system.Parser;
import system.Parser_status;
import system.Source;
import system.Variable;
import system.F_Assign;

public class new_expr implements Parser<String>{
	type type;
	new_suffix new_suffix;
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		st = st + new string("new").parse(s, ps);
		st = st + new spaces().parse(s, ps);
		this.type = new type();
		st = st + this.type.parse(s, ps);
		st = st + new spaces().parse(s, ps);
		this.new_suffix = new new_suffix();
		st = st + this.new_suffix.parse(s, ps);
		if(ps.in_jml){
			throw new Exception("can't use new in jml");
		}	
		return st;
	}
	
	public Variable check(Check_status cs) throws Exception{
		if(this.new_suffix.is_index){
			Variable ret = null;
			ret = new Variable(cs.Check_status_share.get_tmp_num(), "new_" + this.type.type + "_array_tmp", this.type.type, this.new_suffix.array_decl.dims, null, new modifiers(), cs.this_field, cs.ctx.mkBool(false));
			ret.temp_num++;
			ret.new_array = true;
			

			List<IntExpr> lengths = this.new_suffix.array_decl.check(cs);
			
			IntExpr[] tmps = new IntExpr[lengths.size()-1];
			IntExpr[] tmps_full = new IntExpr[this.new_suffix.array_decl.dims];
			ArrayList<IntExpr> tmp_list = new ArrayList<IntExpr>();
			BoolExpr guard = null;
			BoolExpr length_cnst = null;
			
			Expr ex = ret.get_Expr(cs);
			
			
			for(int i = 0; i < lengths.size(); i++){
				
				//length�Ɋւ��鐧��
				int array_dim = this.new_suffix.array_decl.dims - (i);
				String array_type;
				if(ret.type.equals("int")){
					array_type = "int";
				}else if(ret.type.equals("boolean")){
					array_type = "boolean";
				}else{
					array_type = "ref";
				}
				IntExpr length = (IntExpr) cs.ctx.mkSelect(cs.ctx.mkArrayConst("length_" + array_dim + "d_" + array_type, ex.getSort(), cs.ctx.mkIntSort()), ex);
				if(length_cnst == null){
					length_cnst = cs.ctx.mkEq(length, lengths.get(i));
				}else{
					length_cnst = cs.ctx.mkAnd(length_cnst, cs.ctx.mkEq(length, lengths.get(i)));
				}
				
				
				
				IntExpr index = cs.ctx.mkIntConst("tmpIdex" + cs.Check_status_share.get_tmp_num());
				
				if(i == lengths.size() - 1){//�Ō�̃��[�v�Ő����ǉ�
					BoolExpr expr;
					if(guard == null){
						expr = length_cnst;
						cs.add_constraint(expr);
					}else{
						expr = cs.ctx.mkImplies(guard, length_cnst);
						cs.add_constraint(cs.ctx.mkForall(tmps, expr, 1, null, null, null, null));
					}
					
					
				}else{
					tmps[i] = index;
				}
				
				tmps_full[i] = index;
				tmp_list.add(index);
				//�C���f�b�N�X�͈̔͂̃K�[�h 
				if(guard == null){
					guard = cs.ctx.mkAnd(cs.ctx.mkGe(index, cs.ctx.mkInt(0)), cs.ctx.mkGt(lengths.get(i), index));
				}else{
					guard = cs.ctx.mkAnd(guard, cs.ctx.mkAnd(cs.ctx.mkGe(index, cs.ctx.mkInt(0)), cs.ctx.mkGt(lengths.get(i), index)));
				}
				
				//����ex
				ex = ret.get_full_Expr((ArrayList<IntExpr>) tmp_list.clone(), cs);
				
			}
			
			
			
			
			//�z��̏����l�ɂ��Ă̐���
			for(int i = lengths.size(); i < this.new_suffix.array_decl.dims; i++){
				IntExpr index = cs.ctx.mkIntConst("tmpIdex" + cs.Check_status_share.get_tmp_num());
				tmps_full[i] = index;
				tmp_list.add(index);
			}
			
			if(this.type.type.equals("int")){
				BoolExpr value_cnst = cs.ctx.mkEq(cs.ctx.mkInt(0), ret.get_full_Expr(tmp_list, cs));
				cs.add_constraint(cs.ctx.mkForall(tmps_full, cs.ctx.mkImplies(guard, value_cnst), 1, null, null, null, null));
			}else if(this.type.type.equals("boolean")){
				BoolExpr value_cnst = cs.ctx.mkEq(cs.ctx.mkBool(false), ret.get_full_Expr(tmp_list, cs));
				cs.add_constraint(cs.ctx.mkForall(tmps_full, cs.ctx.mkImplies(guard, value_cnst), 1, null, null, null, null));
			}
			

			return ret;
		}else if(this.new_suffix.expression_list!=null){//�R���X�g���N�^
			//String ident = 
			//Field f;
			//Expr ex
			new_suffix ps = this.new_suffix;
			//IntExpr f_index
			
			boolean pre_can_not_use_mutable = cs.can_not_use_mutable;
			if(cs.in_refinement_predicate) cs.can_not_use_mutable = true;
			boolean pre_in_refinement_predicate = cs.in_refinement_predicate;
			cs.in_refinement_predicate = false;
			
			
			class_declaration cd = cs.Check_status_share.compilation_unit.search_class(this.type.type);
			if(cd == null){
				throw new Exception("can't find class " + this.type.type);
			}
			method_decl md = cs.Check_status_share.compilation_unit.search_method(this.type.type, this.type.type);
			if(md == null){
				throw new Exception("can't find method " + this.type.type);
			}
			cs.in_method_call = true;
			//�����̏���
			if(md.formals.param_declarations.size()!=ps.expression_list.expressions.size()){
				throw new Exception("wrong number of arguments");
			}
			
			
			//helper���\�b�h�̒���helper�łȂ����\�b�h���Ă񂾏ꍇ�A�S�Ă�⿌^�����؂���
			if(cs.in_helper && !cs.in_helper){
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

			
			//�Ԃ�l
			Variable result = new Variable(cs.Check_status_share.get_tmp_num(), "class_" + this.type.type + "_constructor_tmp", this.type.type, 0, null, md.modifiers, cs.this_field, cs.ctx.mkBool(false));
			result.temp_num++;
			cs.result = result;
			
			Expr pre_instance_expr = cs.instance_expr;
			Field pre_instance_Field = cs.instance_Field;
			ArrayList<IntExpr> pre_instance_indexs = cs.instance_indexs;
			
			cs.instance_expr = result.get_Expr(cs);
			cs.instance_Field = result;
			cs.instance_indexs = new ArrayList<IntExpr>();

			
			ArrayList<Variable> assignable_args = new ArrayList<Variable>();
			
			for(int j = 0; j < md.formals.param_declarations.size(); j++){
				param_declaration pd = md.formals.param_declarations.get(j);
				modifiers m = new modifiers();
				m.is_final = pd.is_final;
				Variable v = new Variable(cs.Check_status_share.get_tmp_num(), pd.ident, pd.type_spec.type.type, pd.type_spec.dims, pd.type_spec.refinement_type_clause, m, result, cs.ctx.mkBool(true));
				cs.called_method_args.add(v);
				v.temp_num = 0;
				//�����ɒl��R�Â���
				cs.add_constraint(cs.ctx.mkEq(v.get_Expr(cs), ps.expression_list.expressions.get(j).check(cs).expr));
				if(!((v.type.equals("int") || v.type.equals("boolean")) && v.dims==0))v.arg_field =  method_arg_valuse.get(j).field;
				if(method_arg_valuse.get(j).field!=null) v.internal_id = method_arg_valuse.get(j).field.internal_id;
				if((v.type.equals("int") || v.type.equals("boolean"))) assignable_args.add(v);
				
				//�z���⿌^�����S���ǂ���
				Expr method_arg_assign_field_expr = null;
				Expr method_arg_class_field_expr = null;
				if(method_arg_valuse.get(j).field!=null){
					method_arg_assign_field_expr = method_arg_valuse.get(j).field.get_full_Expr(new ArrayList<IntExpr>(method_arg_valuse.get(j).indexs.subList(0, method_arg_valuse.get(j).field.class_object_dims_sum())), cs);
					method_arg_class_field_expr = method_arg_valuse.get(j).field.class_object.get_full_Expr((ArrayList<IntExpr>) method_arg_valuse.get(j).indexs.clone(), cs);
				}
				cs.check_array_alias(v, v.get_Expr(cs), cs.this_field.get_Expr(cs), new ArrayList<IntExpr>(), method_arg_valuse.get(j).field, method_arg_assign_field_expr, method_arg_class_field_expr, method_arg_valuse.get(j).indexs);
				
				
				//⿌^
				if(v.hava_refinement_type()){
					v.assert_refinement(cs, null, new ArrayList<IntExpr>());//class_Field�Ƃ��͖{��result�Ȃǂ����A�܂��ł��Ă��Ȃ��I�u�W�F�N�g�Ȃ̂�null�ł����͂�
				}
			}
			
			
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
			if(md.modifiers.is_pure){
				//�������Ȃ�
			}else if(md.method_specification != null){
				
				Pair<List<F_Assign>, BoolExpr> assign_cnsts = md.method_specification.assignables(cs);
				
				//helper���\�b�h�A�R���X�g���N�^�ł́A����O�Ɍ��؂��K�v�ȏꍇ������
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
					if(field instanceof Variable){//cs.fields�ɖ������� �@�@�@�@this��Variable�̃C���X�^���X
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
					cs.add_constraint(cs.ctx.mkImplies(cs.ctx.mkNot(assign_cnsts.snd), cs.ctx.mkEq(field.get_Expr_assign(cs), field.get_Expr_assign(cs))));
					field.temp_num++;
				}
				for(Variable variable : cs.called_method_args){//����
					Field field = null;
					if(variable.arg_field != null){
						field = variable.arg_field;
					}else{
						field = variable;
					}
					if(field instanceof Variable){//cs.fields�ɖ������� �@�@�@�@this��Variable�̃C���X�^���X
						cs.add_constraint(cs.ctx.mkImplies(cs.ctx.mkNot(assign_cnsts.snd), cs.ctx.mkEq(field.get_Expr_assign(cs), field.get_Expr_assign(cs))));
						field.temp_num++;
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
					if(field instanceof Variable){//cs.fields�ɖ������� �@�@�@�@this��Variable�̃C���X�^���X
						check_array_assign_in_helper_or_constructor(field, new ArrayList<IntExpr>(), cs.ctx.mkBool(true), cs.in_helper, cs.in_constructor, cs);
					}
				}
				
				
				for(Field f_a : cs.fields){
					f_a.temp_num++;
				}
			}
			
			//�������̂ɂ�,int��boolean�ł���Ζ������ɑ���ł���
			for(Variable v : assignable_args){
				v.temp_num++;
			}
			
			//helper���\�b�h��R���X�g���N�^�[�ɂ�����z��̃G�C���A�X
			update_alias_in_helper_or_constructor(9999999, cs.get_pathcondition(), cs, cs.in_helper, cs.in_constructor);
			
			
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
				cs.add_constraint(ensures_expr);
			}
			cs.in_method_call = false;
			
			cs.instance_expr = pre_instance_expr;
			cs.instance_Field = pre_instance_Field;
			cs.instance_indexs = pre_instance_indexs;
			
			cs.can_not_use_mutable = pre_can_not_use_mutable;
			cs.in_refinement_predicate = pre_in_refinement_predicate;
			
			
			return result;
		}else{
			throw new Exception("wrong new clause");
		}
	}
	public Check_return loop_assign(Pair<List<Pair<Field,List<List<IntExpr>>>>,Boolean>assigned_fields, Check_status cs) throws Exception{
		if(this.new_suffix.is_index){
			return null;
		}else if(this.new_suffix.expression_list!=null){//�R���X�g���N�^
			Field f = loop_assign_method(assigned_fields, cs, this.new_suffix);
			Expr ex = f.get_Expr(cs);
			return new Check_return(ex, f, new ArrayList<IntExpr>());
		}else{
			return null;
		}
	}
	
	public Field loop_assign_method(Pair<List<Pair<Field,List<List<IntExpr>>>>,Boolean>assigned_fields, Check_status cs, new_suffix ps)throws Exception{
		
		class_declaration cd = cs.Check_status_share.compilation_unit.search_class(this.type.type);
		if(cd == null){
			throw new Exception("can't find class " + this.type.type);
		}
		method_decl md = cs.Check_status_share.compilation_unit.search_method(this.type.type, this.type.type);
		if(md == null){
			throw new Exception("can't find method " + this.type.type);
		}
		
		
		//�����̏���
		List<Check_return> method_arg_valuse = new ArrayList<Check_return>();
		for(int j = 0; j < md.formals.param_declarations.size(); j++){
			method_arg_valuse.add(ps.expression_list.expressions.get(j).check(cs));
		}
		
		//�֐����̏���
		cs.in_method_call = true;
		
		cs.called_method_args = new ArrayList<Variable>();
		
		//�Ԃ�l
		Variable result = new Variable(cs.Check_status_share.get_tmp_num(), "class_" + this.type.type + "_constructor_tmp", this.type.type, 0, null, md.modifiers, cs.this_field, cs.ctx.mkBool(false));
		result.temp_num++;
		cs.result = result;
		
		Expr pre_instance_expr = cs.instance_expr;
		Field pre_instance_Field = cs.instance_Field;
		ArrayList<IntExpr> pre_instance_indexs = cs.instance_indexs;
		
		cs.instance_expr = result.get_Expr(cs);
		cs.instance_Field = result;
		cs.instance_indexs = new ArrayList<IntExpr>();
		
		for(int j = 0; j < md.formals.param_declarations.size(); j++){
			param_declaration pd = md.formals.param_declarations.get(j);
			modifiers m = new modifiers();
			m.is_final = pd.is_final;
			Variable v = new Variable(cs.Check_status_share.get_tmp_num(), pd.ident, pd.type_spec.type.type, pd.type_spec.dims, pd.type_spec.refinement_type_clause, m, result, cs.ctx.mkBool(true));
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

