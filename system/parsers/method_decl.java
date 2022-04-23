package system.parsers;

import java.util.ArrayList;
import java.util.List;

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
			
			
			
			//���g
			this.compound_statement.check(cs);

			
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
					refinement_type rt = cs.search_refinement_type(cs.return_v.refinement_type_clause.ident, cs.return_v.class_object.type);
					if(rt!=null){
						rt.assert_refinement(cs, cs.return_v, cs.return_expr);
					}else{
						throw new Exception("cant find refinement type " + cs.return_v.refinement_type_clause.ident);
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

}

