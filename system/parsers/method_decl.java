package system.parsers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;

import system.Check_status;
import system.Field;
import system.Helper_assigned_field;
import system.Pair;
import system.Parser;
import system.Parser_status;
import system.Source;
import system.Summery;
import system.Type_info;
import system.Variable;
import system.F_Assign;

public class method_decl implements Parser<String>{
	String st;
	method_specification method_specification;
	modifiers modifiers;
	type_spec type_spec;
	String ident;
	int args_num;
	formals formals;
	compound_statement compound_statement;
	
	public String class_type_name;
	
	String file_path;
	
	public String parse(Source s,Parser_status ps)throws Exception{
		this.st = "";
		
		Source s_backup = s.clone();
		try{
			method_specification ms = new method_specification();
			this.st = this.st + ms.parse(s, ps);
			st = st + new spaces().parse(s, ps);
			this.method_specification = ms;
		}catch (Exception e){
			s.revert(s_backup);
		}	
		
		this.modifiers = new modifiers();
		this.st = this.st + this.modifiers.parse(s, ps);
		this.st = this.st + new newLines().parse(s, ps);
		
		this.st = this.st + new spaces().parse(s, ps);
		s_backup = s.clone();
		try{
			String st2 = "";
			type_spec ts = new type_spec();
			st2 = st2 + ts.parse(s, ps);
			st2 = st2 + new spaces().parse(s, ps);
			
			method_head mh = new method_head();
			st2 = st2 + mh.parse(s, ps);
			this.type_spec = ts;
			this.ident = mh.ident;
			this.formals = mh.formals;
			this.st = this.st + st2;
		}catch (Exception e){
			s.revert(s_backup);
			method_head mh = new method_head();
			this.st = this.st + mh.parse(s, ps);
			this.ident = mh.ident;
			this.formals = mh.formals;
			this.type_spec = null;
		}	
		

		
		this.st = this.st + new newLines().parse(s, ps);
		
		//method_body
		s_backup = s.clone();
		try{
			this.compound_statement = new compound_statement();
			this.st = this.st + this.compound_statement.parse(s, ps);
		}catch (Exception e){
			this.st = this.st + new string(";").parse(s, ps);
		}
			
			
		this.class_type_name = ps.class_type_name;
		this.file_path = ps.file_path;
		
		return st;
	}
	
	public void check(Check_status cs, Summery summery){
		
		System.out.println("Verify method " + this.ident);
		
		//������
		cs.md = this;
		cs.return_conditions = new ArrayList<BoolExpr>();
		
		
		
		//�F�X�̏�������Œǉ�
		try{//return�̏���
			
			if(this.modifiers.is_helper){
				if(!(this.modifiers.is_pure || this.modifiers.is_private)){
					throw new Exception("helper method must be private or pure");
				}
				cs.in_helper = true;
			}
			
			if(this.modifiers.is_no_refinement_type) cs.in_no_refinement_type = true;
			
			if(this.type_spec!=null){
				cs.return_v = new Variable(cs.Check_status_share.get_tmp_num(), "return", this.type_spec.type.type, this.type_spec.dims, this.type_spec.refinement_type_clause, this.modifiers, cs.this_field.type, cs.ctx.mkBool(false));
				cs.return_v.temp_num++;
				cs.return_v.alias = cs.ctx.mkBool(true);
			}else{
				//�R���X�g���N�^�ł̏�����
				
				for(Field f : cs.fields){
					if(f.type.equals("int") && f.dims == 0){
						BoolExpr ex = cs.ctx.mkEq(cs.ctx.mkSelect((ArrayExpr) f.get_Expr(cs), cs.this_field.get_Expr(cs)), cs.ctx.mkInt(0));	
						cs.add_constraint(ex);
					}else if(f.type.equals("boolean") && f.dims == 0){
						BoolExpr ex = cs.ctx.mkEq(cs.ctx.mkSelect((ArrayExpr) f.get_Expr(cs), cs.this_field.get_Expr(cs)), cs.ctx.mkFalse());
						cs.add_constraint(ex);			
					}
				}
			}
			
			//����
			this.formals.check(cs);
			
			//new�ō����̂ƎQ�Ƃ����Ȃ�
			cs.this_field.ref_constraint(0, cs.invariant_refinement_type_deep_limmit+1, null, new ArrayList<IntExpr>(), cs);
			//���[�J���ϐ���
			for(Variable v : cs.variables){
				v.ref_constraint(0, cs.invariant_refinement_type_deep_limmit+1, null, new ArrayList<IntExpr>(), cs);
			}
			
			//old�p
			Check_status csc = cs.clone();
			csc.clone_list();
			cs.this_old_status = csc;
			csc.this_old_status = cs;

			cs.used_methods.add(this);
			
			if(this.type_spec!=null){
				
				//���O����
				System.out.println("precondition invariant");
				
				if(!cs.in_helper){
					BoolExpr pre_invariant_expr = cs.all_invariant_expr();
					cs.add_constraint(pre_invariant_expr);
				}
				

			
			}else{//�R���X�g���N�^
				cs.in_constructor = true;
			}
			

			System.out.println("precondition requires");
			BoolExpr require_expr = null;
			
			if(this.method_specification != null){
				if(this.modifiers.is_private==false){
					cs.ban_private_visibility = true;
				}
				
				require_expr = this.method_specification.requires_expr(cs);
				cs.add_constraint(require_expr);
				
				cs.ban_private_visibility = false;
			}
			
			cs.used_methods.remove(this);
		
			

			if(this.method_specification != null){
				if(this.modifiers.is_private==false){
					cs.ban_private_visibility = true;
				}
				Pair<List<F_Assign>, BoolExpr> assign_cnsts = this.method_specification.assignables(cs);
				//�e�t�B�[���h�̑���������e�t�B�[���h�Ɏ�������
				for(F_Assign fa : assign_cnsts.fst){
					fa.field.assinable_cnst_indexs.addAll(fa.cnst_array);
				}
				//�ǂ̃t�B�[���h�ɂ�����ł������
				cs.assinable_cnst_all = assign_cnsts.snd;
				
				
				cs.ban_private_visibility = false;
			}else{//���ł�������Ă���
				cs.assinable_cnst_all = cs.ctx.mkBool(true);
			}
			
			
			String pre_class_type_name = cs.this_field.type;
			cs.this_field.type = this.class_type_name;
			
			//���g
			if(this.type_spec==null){//�R���X�g���N�^
				class_declaration cd = cs.Check_status_share.compilation_unit.search_class(cs.instance_class_name);
				cd = cd.super_class;
				if(cd != null){//�T�u�N���X�̃R���X�g���N�^�[
					try{
						postfix_expr pe = this.compound_statement.statements.get(0).expression.assignment_expr.implies_expr.logical_or_expr.logical_and_expr.equality_expr.relational_expr1.additive_expr1.mult_expr1.unary_expr1.postfix_expr;
						if(pe.primary_expr.is_super && pe.primary_suffixs.size() == 1 && pe.primary_suffixs.get(0).is_method){
							pe.primary_expr.constructor_first = true;
						}else{
							throw new Exception("call super constructor");
						}
						
					}catch(Exception e){//�ŏ���statement�̓R���X�g���N�^�[�ł͂Ȃ�
						//���������ŃR���X�g���N�^���Ăяo��
						primary_suffix ps = new primary_suffix();
						ps.expression_list = new expression_list();
						ps.expression_list.expressions = new ArrayList<expression>();
						new postfix_expr().method(cs, cd.class_name, cd.class_name, cs.instance_expr, ps, true);
					}
					
				}
			}
			this.compound_statement.check(cs);
			
			cs.this_field.type = pre_class_type_name;
			
			//return���Ȃ������ꍇ�̌���
			cs.used_methods.add(this);
			if(cs.after_return==false && (this.type_spec==null||this.type_spec.type.type.equals("void"))){
				this.check_post_condition(cs);
			}
			if(cs.after_return==false && !(this.type_spec==null||this.type_spec.type.type.equals("void"))){
				throw new Exception("In some cases, \"return\" has not been done.");
			}
			cs.used_methods.remove(this);
			
			System.out.println("method \"" + this.ident + "\" is valid\n\n");
			summery.valids.add("" + this.ident + "(class : " + this.class_type_name + ")" + " " + this.file_path);
		}catch(Exception e){
			System.out.println(e);
			System.out.println("!!!!!!!!!! method \"" + this.ident + "\" is invalid !!!!!!!!!!\n\n");
			summery.invalids.add("" + this.ident + "(class : " + this.class_type_name + ")" + " " + this.file_path);
		}
	}
	
	
	//��������̌���
	public void check_post_condition(Check_status cs) throws Exception{
		//return�̏���

		
		//�������
		cs.in_postconditions = true;
		cs.used_methods.add(this);
		System.out.println("postcondition invariant");
		if(!cs.in_helper){
			BoolExpr post_invariant_expr = cs.all_invariant_expr();
			cs.assert_constraint(post_invariant_expr);
		}
		
		System.out.println("postcondition ensures");
		BoolExpr ensures_expr = null;
		if(this.method_specification!=null){
			if(this.modifiers.is_private==false){
				cs.ban_private_visibility = true;
			}
			

			ensures_expr = this.method_specification.ensures_expr(cs);

			cs.assert_constraint(ensures_expr);
			
			cs.ban_private_visibility = false;
		}
		cs.in_postconditions = false;
		
		
		//�Ԃ�l��refinement_type
		//�Ԃ�l�����f���t�B�[���h�ƌ��ѕt�����邱�Ƃ͂Ȃ��͂�
		//�Ȃ̂ŁAcs.return_expr��⿌^�����ϐ��̒l�Ƃ��ēn�����߂ɂ����͂��̂܂�
		if(this.type_spec!=null&&cs.return_v.refinement_type_clause!=null){
			if(cs.return_v.refinement_type_clause.refinement_type!=null){
				cs.return_v.refinement_type_clause.refinement_type.assert_refinement(cs, cs.return_v, cs.return_expr, cs.this_field.get_Expr(cs));
			}else if(cs.return_v.refinement_type_clause.ident!=null){
				refinement_type rt = cs.search_refinement_type(cs.this_field.type, cs.return_v.refinement_type_clause.ident);
				if(rt!=null){
					rt.assert_refinement(cs, cs.return_v, cs.return_expr, cs.this_field.get_Expr(cs));
				}else{
					throw new Exception("can't find refinement type " + cs.return_v.refinement_type_clause.ident);
				}
			}
		}

		cs.used_methods.remove(this);
		
		
		
		if(cs.in_constructor){
			//�R���X�g���N�^�̍Ō�ł�⿌^�̃`�F�b�N
			cs.constructor_refinement_check();
			
			//�R���X�g���N�^���ŏ���������Ă��Ȃ�final���Ȃ���
			for(Field f : cs.fields){
				if(f.modifiers!=null && f.modifiers.is_final){
					if(f.final_initialized==false){
						throw new Exception("final variable " + f.field_name + " was not initialized.");
					}
				}
			}
		}
		
		if(cs.in_helper || cs.in_no_refinement_type){
			
			System.out.println("refinement type check : helper method");
			
			//helper�̍Ō�ł�⿌^�̃`�F�b�N
			
			for(Helper_assigned_field assigned_fields : cs.helper_assigned_fields){
				cs.solver.push();
				
				cs.add_constraint(assigned_fields.assigned_pathcondition);
				Field v = cs.search_internal_id(assigned_fields.field.internal_id);
				if(v == null)v = assigned_fields.field;//if���A���[�v���Œ�`���ꂽ�ϐ��@������ŏI�I��⿌^�𖞂������̌��؂��s���K�v������
				Field old_v = cs.this_old_status.search_internal_id(v.internal_id);
				
				Expr v_class_object_expr = assigned_fields.class_object_expr;

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
				if(!(v instanceof Variable)){
					old_v.add_refinement_constraint(cs.this_old_status, v_class_object_expr, true);
				}
				
				v.assert_refinement(cs, v_class_object_expr);
				
				cs.solver.pop();
			}
			
			
		}
	}
	
	

	public void inheritance_refinement_types(class_declaration class_decl, compilation_unit cu) throws Exception{
		System.out.println("check method type inheritance : " + this.ident);
		
		class_declaration super_class = class_decl.super_class;
		
		
		//�R���X�g���N�^�[�͌p�����Ȃ�
		if(this.type_spec == null) return;
		
		ArrayList<Type_info> param_types = new ArrayList<Type_info>();
		for(param_declaration pd : this.formals.param_declarations){
			param_types.add(new Type_info(pd.type_spec.type.type, pd.type_spec.dims));
		}
		
		//�p�����Ă��Ȃ��Ȃ牽�����Ȃ�
		if(cu.search_method(super_class.class_name, this.ident, param_types, false, class_decl.class_name) == null) return;
		
		
		
		//�Ԃ�l�̌^��⿌^
		
		//�z��ɂ͐V����⿌^�����邱�Ƃ��ł��Ȃ�
		if(this.type_spec.dims > 0 && this.type_spec.refinement_type_clause!=null) throw new Exception("array can not override refinement type");
		
		boolean exist_super_md = false;
		while(true){
			method_decl super_md = cu.search_method(super_class.class_name, this.ident, param_types, false, class_decl.class_name);
			if(super_md != null) exist_super_md = true;
			if(super_md == null || super_md.type_spec.refinement_type_clause==null){//⿌^��������܂�super class��T��
				if(super_class.super_class == null){
					if(this.type_spec.refinement_type_clause!=null && this.type_spec.refinement_type_clause.refinement_type!=null &&
							this.type_spec.refinement_type_clause.refinement_type.type.type.equals("Super_type")){
							if(exist_super_md == false){
								throw new Exception("super class has not method " + this.ident);
							}
						this.type_spec.refinement_type_clause.refinement_type.type.type = this.type_spec.type.type;
					}
					break;//�e��⿌^���Ȃ��Ȃ�ǂ�
				}else{
					super_class = super_class.super_class;
				}
			}else if(this.type_spec.refinement_type_clause!=null){//⿌^�����e�N���X�����������ꍇ�A����⿌^�������Ă���ꍇ
				if(this.type_spec.refinement_type_clause == super_md.type_spec.refinement_type_clause){//�X�[�p�[�N���X��refinement_type_clause�Ɠ����C���X�^���X�������牽�����Ȃ�
					//�������Ȃ�
					break;
				}else if(super_md.type_spec.refinement_type_clause.ident!=null){//�e��ident
					String rt_name;
					if(this.type_spec.refinement_type_clause.ident!=null){//ident�̏ꍇ�́A�e��⿌^�������Ă��Ȃ��A�܂��͐e��ident����Base�^�C�v�Ɠ���
						rt_name = this.type_spec.refinement_type_clause.ident;
					}else{//refinement_type�������Ă���ꍇ
						rt_name = this.type_spec.refinement_type_clause.refinement_type.type.type;
						if(rt_name.equals("Super_type")){
							this.type_spec.refinement_type_clause.refinement_type.type.type = super_md.type_spec.refinement_type_clause.ident;
							break;
						}
					}
					while(true){
						if(super_md.type_spec.refinement_type_clause.ident.equals(rt_name)){
							break;//�����Ȃ�悢
						}
						refinement_type rt = cu.search_refinement_type(class_decl.class_name, rt_name);
						if(rt == null){
							throw new Exception(this.ident + ": return value: Base type must be the refinement type that this method has in the super class.");
						}
						rt_name = rt.type.type;
					}
					break;
				}else{//�e��refinement_type�������Ă���
					if(this.type_spec.refinement_type_clause.refinement_type!=null){//refinement_type�������Ă���ꍇ��Super_type���K�v
						if(this.type_spec.refinement_type_clause.refinement_type.type.type.equals("Super_type")){
							def_type_clause tmp_def_type = new def_type_clause();
							//���O�͈�ӂ̂͂������A����⿌^����錾����|���l�����邩������Ȃ��̂Ő�������������
							tmp_def_type.ident = class_decl.class_name + "_" + this.ident + "_ret_val_tmp_refinement_type" + (new Random().nextInt(88889) + 11111);
							tmp_def_type.refinement_type = super_md.type_spec.refinement_type_clause.refinement_type;
							class_decl.class_block.def_type_clauses.add(tmp_def_type);
							this.type_spec.refinement_type_clause.refinement_type.type.type = tmp_def_type.ident;
							break;
						}	
					}
					throw new Exception(this.ident + ": return value: Base type must be the refinement type that this method has in the super class.");
				}
			}else{//⿌^�����e�N���X�����������ꍇ�A����⿌^�������Ă��Ȃ������ꍇ
				//�e��refinement_type�̃C���X�^���X�����̂܂܎󂯌p��
				this.type_spec.refinement_type_clause = super_md.type_spec.refinement_type_clause;
				break;
			}
		}
		
		//�����̌^��⿌^
		
		//Check_status�̗p��
		Check_status cs = new Check_status(cu);
		modifiers m = new modifiers();
		m.is_final = true;
		Field this_field = new Variable(cs.Check_status_share.get_tmp_num(), "this", class_decl.class_name, 0, null, m, null, cs.ctx.mkBool(false));
		this_field.temp_num = 0;
		cs.this_field = this_field;
		
		Field super_this_field = new Variable(cs.Check_status_share.get_tmp_num(), "this", super_class.class_name, 0, null, m, null, cs.ctx.mkBool(false));
		super_this_field.temp_num = 0;
		
		//������
		cs.instance_expr = this_field.get_Expr(cs);
		cs.instance_class_name = this_field.type;
		
		//�e�����̃`�F�b�N
		for(int i = 0; i < this.formals.param_declarations.size(); i++){
			
			//�z��ɂ͐V����⿌^�����邱�Ƃ��ł��Ȃ�
			if(this.formals.param_declarations.get(i).type_spec.dims > 0 && this.formals.param_declarations.get(i).type_spec.refinement_type_clause!=null) throw new Exception("array can not override refinement type");
			
			//�������������\��Variable
			modifiers modi = new modifiers();
			modi.is_final = this.formals.param_declarations.get(i).is_final;
			modi.is_private = false;
			modi.is_spec_public = false;
			//����v��⿌^�����Ȃ��Ă�����(����͒ǉ����Ă����̂�)
			Variable v = cs.add_variable(this.formals.param_declarations.get(i).ident, this.formals.param_declarations.get(i).type_spec.type.type
					, this.formals.param_declarations.get(i).type_spec.dims, null, modi, cs.ctx.mkBool(true));
			
			v.temp_num=0;
			
			while(true){
				method_decl super_md = cu.search_method(super_class.class_name, this.ident, param_types, false, class_decl.class_name);
				if(super_md == null || super_md.formals.param_declarations.get(i).type_spec.refinement_type_clause==null){//⿌^��������܂�super class��T��
					if(super_class.super_class == null){
						if(this.formals.param_declarations.get(i).type_spec.refinement_type_clause!=null){
							throw new Exception(this.formals.param_declarations.get(i).ident + " in " + class_decl.super_class.class_name + "don't have refinement type");
						}
						break;//�e��⿌^���Ȃ��Ȃ�ǂ�
					}else{
						super_class = super_class.super_class;
					}
				}else if(this.formals.param_declarations.get(i).type_spec.refinement_type_clause!=null){//⿌^�����e�N���X�����������ꍇ�A����⿌^�������Ă���ꍇ
					refinement_type this_refinement_type,super_refinement_type;
					if(this.formals.param_declarations.get(i).type_spec.refinement_type_clause.refinement_type != null){
						this_refinement_type = this.formals.param_declarations.get(i).type_spec.refinement_type_clause.refinement_type;
					}else{
						this_refinement_type = cu.search_refinement_type(class_decl.class_name, this.formals.param_declarations.get(i).type_spec.refinement_type_clause.ident);
						if(this_refinement_type==null) throw new Exception("can't find refinement type " + this.formals.param_declarations.get(i).type_spec.refinement_type_clause.ident);
					}
					
					if(super_md.formals.param_declarations.get(i).type_spec.refinement_type_clause.refinement_type != null){
						super_refinement_type = super_md.formals.param_declarations.get(i).type_spec.refinement_type_clause.refinement_type;
					}else{
						super_refinement_type = cu.search_refinement_type(class_decl.class_name, super_md.formals.param_declarations.get(i).type_spec.refinement_type_clause.ident);
						if(super_refinement_type==null) throw new Exception("can't find refinement type " + super_md.formals.param_declarations.get(i).type_spec.refinement_type_clause.ident);
					}
					
					this_refinement_type.check_subtype(v, this_field.get_Expr(cs), super_refinement_type, cs);
					break;
					
				}else{//⿌^�����e�N���X�����������ꍇ�A����⿌^�������Ă��Ȃ������ꍇ
					//�e��refinement_type�̃C���X�^���X�����̂܂܎󂯌p��
					this.formals.param_declarations.get(i).type_spec.refinement_type_clause = super_md.formals.param_declarations.get(i).type_spec.refinement_type_clause;
					break;
				}
			}

		}
	}
	
	
	public void pure_modifier(){
		if(this.modifiers.is_pure && this.method_specification!=null){
			for(generic_spec_case gsc : this.method_specification.spec_case_seq.generic_spec_cases){
				if(gsc.simple_spec_body==null)gsc.simple_spec_body = new simple_spec_body();
				gsc.simple_spec_body.assignable_nothing = true;
			}
		}else if(this.modifiers.is_pure && this.method_specification==null){
			this.method_specification = new method_specification();
			this.method_specification.spec_case_seq = new spec_case_seq();
			this.method_specification.spec_case_seq.generic_spec_cases = new ArrayList<generic_spec_case>();
			generic_spec_case gsc = new generic_spec_case();
			gsc.simple_spec_body = new simple_spec_body();
			gsc.simple_spec_body.assignable_nothing = true;
			gsc.class_type_name = this.class_type_name;
			this.method_specification.spec_case_seq.generic_spec_cases.add(gsc);
		}
	}

	//���̃C���X�^���X�́A⿌^�������Ȃ�clone��Ԃ�
	public method_decl clone_no_refinemet_type(){
		method_decl clone_md = new method_decl();
		clone_md.st = this.st;
		clone_md.method_specification = this.method_specification;
		clone_md.modifiers = this.modifiers;
		clone_md.ident = this.ident;
		clone_md.args_num = this.args_num;
		clone_md.compound_statement = this.compound_statement;
		clone_md.class_type_name = this.class_type_name;
		
		clone_md.type_spec = new type_spec();
		clone_md.type_spec.type = this.type_spec.type;
		clone_md.type_spec.dims = this.type_spec.dims;
		
		clone_md.formals = new formals();
		clone_md.formals.param_declarations = new ArrayList<param_declaration>();
		for(param_declaration pd : this.formals.param_declarations){
			param_declaration clone_pd = new param_declaration();
			clone_pd.is_final = pd.is_final;
			clone_pd.ident = pd.ident;
			clone_pd.type_spec = new type_spec();
			clone_pd.type_spec.type = pd.type_spec.type;
			clone_pd.type_spec.dims = pd.type_spec.dims;
			clone_md.formals.param_declarations.add(clone_pd);
		}
		return clone_md;
	}
	
	
}

