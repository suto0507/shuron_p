package system.parsers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.BoolExpr;

import system.Check_status;
import system.Field;
import system.Parser;
import system.Parser_status;
import system.Source;
import system.Variable;

public class method_decl implements Parser<String>{
	String st;
	List<requires_clause> requires;
	List<ensures_clause> ensures;
	List<assignable_clause> assignables;
	modifiers modifiers;
	type_spec type_spec;
	String ident;
	int args_num;
	formals formals;
	compound_statement compound_statement;
	
	public String class_type_name;
	
	
	public String parse(Source s,Parser_status ps)throws Exception{
		this.st = "";
		Source s_backup = s.clone();
		try{
			generic_spec_case gsc = new generic_spec_case();
			this.st = this.st + gsc.parse(s, ps);
			this.requires = gsc.get_requires();
			this.ensures = gsc.get_ensures();
			this.assignables = gsc.get_assignable();
			st = st + new spaces().parse(s, ps);
		}catch (Exception e){
			s.revert(s_backup);
		}	
		
		this.modifiers = new modifiers();
		this.st = this.st + this.modifiers.parse(s, ps);
		this.st = this.st + new spaces().parse(s, ps);
		
		s_backup = s.clone();
		try{
			String st2 = "";
			type_spec ts = new type_spec();
			st2 = st2 + ts.parse(s, ps);
			this.type_spec = ts;
			st2 = st2 + new spaces().parse(s, ps);
			
			method_head mh = new method_head();
			st2 = st2 + mh.parse(s, ps);
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
		
		this.compound_statement = new compound_statement();
		this.st = this.st + this.compound_statement.parse(s, ps);
		
		this.class_type_name = ps.class_type_name;
		
		return st;
	}
	
	public void check(Check_status cs){
		
		System.out.println("Verify method " + this.ident);
		
		
		
		//�F�X�̏�������Œǉ�
		try{//return�̏���
			
			
			
			
			if(this.type_spec!=null){
				cs.return_v = new Variable(cs.Check_status_share.get_tmp_num(), "return", this.type_spec.type.type, this.type_spec.dims, this.type_spec.refinement_type_clause, this.modifiers, cs.this_field);
			}else{
				//�R���X�g���N�^�ł̏�����
				for(Field f : cs.fields){
					if(f.type.equals("int")){
						BoolExpr ex = cs.ctx.mkEq(cs.ctx.mkSelect((ArrayExpr) f.get_Expr(cs), cs.this_field.get_Expr(cs)), cs.ctx.mkInt(0));	
						cs.add_constraint(ex);
					}else if(f.type.equals("boolean")){
						BoolExpr ex = cs.ctx.mkEq(cs.ctx.mkSelect((ArrayExpr) f.get_Expr(cs), cs.this_field.get_Expr(cs)), cs.ctx.mkFalse());
						cs.add_constraint(ex);			
					}
				}
			}
			
			//����
			this.formals.check(cs);

			if(this.type_spec!=null){
				
				//���O����
				System.out.println("precondition invariant");
				BoolExpr pre_invariant_expr = null;
				if(cs.invariants!=null&&cs.invariants.size()>0){
					for(invariant inv : cs.invariants){
						if(inv.is_private==true){//�������������̂����g���Ȃ�
							cs.ban_default_visibility = true;
						}else{
							cs.ban_private_visibility = true;
						}

						if(pre_invariant_expr == null){
							pre_invariant_expr = (BoolExpr) inv.check(cs);
						}else{
							pre_invariant_expr = cs.ctx.mkAnd(pre_invariant_expr, (BoolExpr)inv.check(cs));
						}

						cs.ban_default_visibility = false;
						cs.ban_private_visibility = false;
					}
					cs.add_constraint(pre_invariant_expr);
				}

			
			}else{//�R���X�g���N�^
				cs.in_constructor = true;
			}
			

			System.out.println("precondition requires");
			BoolExpr require_expr = null;
			if(this.requires!=null&&this.requires.size()>0){
				if(this.modifiers.is_privte==false){
					cs.ban_private_visibility = true;
				}

				for(requires_clause re : this.requires){
					if(require_expr == null){
						require_expr = (BoolExpr) re.check(cs);
					}else{
						require_expr = cs.ctx.mkAnd(require_expr, (BoolExpr)re.check(cs));
					}
				}
				cs.add_constraint(require_expr);

				cs.ban_private_visibility = false;
			}
			
			//old�p
			Check_status csc = cs.clone();
			csc.clone_list();
			cs.this_old_status = csc;
		
			
			//assignable�̏���
			if(this.assignables!=null&&this.assignables.size()>0){
				cs.assignables = new ArrayList<Field>();
				for(assignable_clause ac : this.assignables){
					for(store_ref_expression sre : ac.store_ref_list.store_ref_expressions){
						System.out.println("add assinable " + sre.st);
						cs.assignables.add(sre.check(cs));
					}
				}
			}else{
				cs.assignables = null;
			}
			
			
			String pre_class_type_name = cs.this_field.type;
			cs.this_field.type = this.class_type_name;
			//���g
			this.compound_statement.check(cs);
			
			cs.this_field.type = pre_class_type_name;
			
			//return�̏���
			if(cs.after_return==false && !(this.type_spec==null||this.type_spec.type.type.equals("void"))){
				throw new Exception("In some cases, \"return\" has not been done.");
			}
			if(!(this.type_spec==null||this.type_spec.type.type.equals("void"))){
				cs.return_expr = cs.return_exprs.get(cs.return_exprs.size()-1);
				for(int i = cs.return_exprs.size()-2; i>=0; i--){
					cs.return_expr = cs.ctx.mkITE(cs.return_pathconditions.get(i), cs.return_exprs.get(i), cs.return_expr);
				}
			}
			
			
			//�������
			System.out.println("postcondition invariant");
			BoolExpr post_invariant_expr = null;
			if(cs.invariants!=null&&cs.invariants.size()>0){
				for(invariant inv : cs.invariants){
					if(inv.is_private==true){//�������������̂����g���Ȃ�
						cs.ban_default_visibility = true;
					}else{
						cs.ban_private_visibility = true;
					}
					
					if(post_invariant_expr == null){
						post_invariant_expr = (BoolExpr) inv.check(cs);
					}else{
						post_invariant_expr = cs.ctx.mkAnd(post_invariant_expr, (BoolExpr)inv.check(cs));
					}
					
					cs.ban_default_visibility = false;
					cs.ban_private_visibility = false;
				}
				cs.assert_constraint(post_invariant_expr);
			}
			System.out.println("postcondition ensures");
			BoolExpr ensures_expr = null;
			if(this.ensures!=null&&this.ensures.size()>0){
				if(this.modifiers.is_privte==false){
					cs.ban_private_visibility = true;
				}
				
				for(ensures_clause ec : this.ensures){
					if(ensures_expr == null){
						ensures_expr = (BoolExpr) ec.check(cs);
					}else{
						ensures_expr = cs.ctx.mkAnd(ensures_expr, (BoolExpr)ec.check(cs));
					}
				}
				cs.assert_constraint(ensures_expr);
				
				cs.ban_private_visibility = false;
			}
			
			
			
			//�Ԃ�l��refinement_type
			if(this.type_spec!=null&&cs.return_v.refinement_type_clause!=null){
				if(cs.return_v.refinement_type_clause.refinement_type!=null){
					cs.return_v.refinement_type_clause.refinement_type.assert_refinement(cs, cs.return_v, cs.return_expr);
				}else if(cs.return_v.refinement_type_clause.ident!=null){
					refinement_type rt = cs.search_refinement_type(cs.return_v.class_object.type, cs.return_v.refinement_type_clause.ident);
					if(rt!=null){
						rt.assert_refinement(cs, cs.return_v, cs.return_expr);
					}else{
						throw new Exception("can't find refinement type " + cs.return_v.refinement_type_clause.ident);
					}
				}
			}
			
			
			
			
			if(cs.in_constructor){
				//�R���X�g���N�^�̍Ō�ł�⿌^�̃`�F�b�N
				cs.constructor_refinement_check();
				
				//�R���X�g���N�^���ŏ���������Ă��Ȃ�final���Ȃ���
				for(Field f : cs.fields){
					if(f.modifiers!=null && f.modifiers.is_final){
						if(f.class_object!=null && f.class_object.equals(cs.this_field) && f.final_initialized==false){
							throw new Exception("final variable " + f.field_name + " was not initialized.");
						}
					}
				}
			}
			
			
			System.out.println("method \"" + this.ident + "\" is valid\n\n");
		}catch(Exception e){
			System.out.println(e);
			System.out.println("!!!!!!!!!! method \"" + this.ident + "\" is invalid !!!!!!!!!!\n\n");
		}
		
		
		
	}

	public void inheritance_refinement_types(class_declaration class_decl, compilation_unit cu) throws Exception{
		System.out.println("check method type inheritance : " + this.ident);
		
		class_declaration super_class = class_decl.super_class;
		
		//�Ԃ�l�̌^��⿌^
		boolean exist_super_md = false;
		while(true){
			method_decl super_md = cu.search_method(super_class.class_name, this.ident);
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
				if(super_md.type_spec.refinement_type_clause.ident!=null){//�e��ident
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
		Field this_field = new Variable(cs.Check_status_share.get_tmp_num(), "this", class_decl.class_name, 0, null, m, null );
		this_field.temp_num = 0;
		cs.fields.add(this_field);
		cs.this_field = this_field;
		//������
		cs.refined_class_Expr = this_field.get_Expr(cs);
		cs.refined_class_Field = this_field;
		cs.refined_class_Field_index = null;
		
		//�e�����̃`�F�b�N
		for(int i = 0; i < this.formals.param_declarations.size(); i++){
			
			//�������������\��Variable
			modifiers modi = new modifiers();
			modi.is_final = this.formals.param_declarations.get(i).is_final;
			modi.is_privte = false;
			modi.is_spec_public = false;
			//����v��⿌^�����Ȃ��Ă�����(����͒ǉ����Ă����̂�)
			Variable v = cs.add_variable(this.formals.param_declarations.get(i).ident, this.formals.param_declarations.get(i).type_spec.type.type
					, this.formals.param_declarations.get(i).type_spec.dims, null, modi);
			
			v.temp_num=0;
			
			while(true){
				method_decl super_md = cu.search_method(super_class.class_name, this.ident);
				if(super_md == null || super_md.formals.param_declarations.get(i).type_spec.refinement_type_clause==null){//⿌^��������܂�super class��T��
					if(super_class.super_class == null){
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
					
					this_refinement_type.check_subtype(v, super_refinement_type, cs);
					break;
					
				}else{//⿌^�����e�N���X�����������ꍇ�A����⿌^�������Ă��Ȃ������ꍇ
					//�e��refinement_type�̃C���X�^���X�����̂܂܎󂯌p��
					this.formals.param_declarations.get(i).type_spec.refinement_type_clause = super_md.formals.param_declarations.get(i).type_spec.refinement_type_clause;
					break;
				}
			}

		}
	}
}

