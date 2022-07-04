package system.parsers;

import java.util.Random;

import system.Check_status;
import system.Field;
import system.Pair;
import system.Parser;
import system.Parser_status;
import system.Source;
import system.Variable;

public class override_refinement_type_clause implements Parser<String>{
	String st;
	String ident;
	type_or_refinement_type type_or_refinement_type;
	param_override_list param_override_list;
	
	public String parse(Source s,Parser_status ps)throws Exception{
		this.st = "";
		this.st = this.st + new string("override_refinement_type").parse(s,ps);
		this.st = this.st + new spaces().parse(s,ps);
		this.type_or_refinement_type = new type_or_refinement_type();
		this.st = this.st + type_or_refinement_type.parse(s,ps);
		this.st = this.st + new spaces().parse(s,ps);
		this.ident = new ident().parse(s, ps);
		this.st = this.st + this.ident;
		
		Source s_backup = s.clone();
		try{
			String st2 = "";
			st2 = st2 + new spaces().parse(s,ps);
			st2 = st2 + new string("(").parse(s,ps);
			st2 = st2 + new spaces().parse(s,ps);
			
			Source s_backup2 = s.clone();
			try{
				String st3 = "";
				param_override_list pol = new param_override_list();
				st3 = st3 + pol.parse(s,ps);
				this.param_override_list = pol;
				st3 = st3 + new spaces().parse(s,ps);
				
				st2 = st2 + st3;
			}catch (Exception e){
				s.revert(s_backup2);
			}
			
			st2 = st2 + new string(")").parse(s,ps);
			
			st = st + st2;
		}catch (Exception e){
			s.revert(s_backup);
		}
		st = st + new spaces().parse(s,ps);
		st = st + new string(";").parse(s,ps);
		
		return this.st;
	}
	
	public void inheritance_refinement_types(class_declaration class_decl, compilation_unit cu) throws Exception{
		System.out.println("check override : " + this.st);
		
		class_declaration super_class = class_decl.super_class;
		
		if(this.param_override_list == null){//�ϐ�
			//�ϐ���⿌^
			boolean exist_super_vd = false;
			String base_type = "";
			while(true){
				variable_definition super_vd = cu.search_field(super_class.class_name, this.ident);
				if(super_vd != null){
					exist_super_vd = true;
					base_type = super_vd.variable_decls.type_spec.type.type;
				}
				if(super_vd == null || super_vd.variable_decls.type_spec.refinement_type_clause==null){//⿌^��������܂�super class��T��
					if(super_class.super_class == null){
						if(this.type_or_refinement_type.refinement_type!=null &&
								this.type_or_refinement_type.refinement_type.type.type.equals("Super_type")){
								if(exist_super_vd == false){
									throw new Exception("super class has not variable " + this.ident);
								}
							this.type_or_refinement_type.refinement_type.type.type = base_type;
						}
						break;//�e��⿌^���Ȃ��Ȃ�ǂ�
					}else{
						super_class = super_class.super_class;
					}
				}else if(this.type_or_refinement_type.type!=null && 
					this.type_or_refinement_type.type.type.equals(super_vd.variable_decls.type_spec.type.type)){//⿌^�����e�N���X�����������ꍇ�A����⿌^�������Ă��Ȃ������ꍇ
					System.out.println("Meaningless override : " + this.st);
					if(super_vd.variable_decls.type_spec.refinement_type_clause.ident!=null){
						this.type_or_refinement_type.type.type = super_vd.variable_decls.type_spec.refinement_type_clause.ident;
					}else if(super_vd.variable_decls.type_spec.refinement_type_clause.refinement_type != null){
						this.type_or_refinement_type.refinement_type = super_vd.variable_decls.type_spec.refinement_type_clause.refinement_type;
					}
					break;
				}else{//⿌^�����e�N���X�����������ꍇ�A����⿌^�������Ă���ꍇ
					if(super_vd.variable_decls.type_spec.refinement_type_clause.ident!=null){//�e��ident
						String rt_name;
						if(this.type_or_refinement_type.type!=null){//ident�̏ꍇ�́A�e��⿌^�������Ă��Ȃ��A�܂��͐e��ident����Base�^�C�v�Ɠ���
							rt_name = this.type_or_refinement_type.type.type;
						}else{//refinement_type�������Ă���ꍇ
							rt_name = this.type_or_refinement_type.refinement_type.type.type;
							if(rt_name.equals("Super_type")){
								this.type_or_refinement_type.refinement_type.type.type = super_vd.variable_decls.type_spec.refinement_type_clause.ident;
								break;
							}
						}
						while(true){
							if(super_vd.variable_decls.type_spec.refinement_type_clause.ident.equals(rt_name)){
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
						if(this.type_or_refinement_type.refinement_type!=null){//refinement_type�������Ă���ꍇ��Super_type���K�v
							if(this.type_or_refinement_type.refinement_type.type.type.equals("Super_type")){
								def_type_clause tmp_def_type = new def_type_clause();
								//���O�͈�ӂ̂͂������A����⿌^����錾����|���l�����邩������Ȃ��̂Ő�������������
								tmp_def_type.ident = class_decl.class_name + "_" + this.ident + "_ret_val_tmp_refinement_type" + (new Random().nextInt(88889) + 11111);
								tmp_def_type.refinement_type = super_vd.variable_decls.type_spec.refinement_type_clause.refinement_type;
								class_decl.class_block.def_type_clauses.add(tmp_def_type);
								this.type_or_refinement_type.refinement_type.type.type = tmp_def_type.ident;
								break;
							}	
						}
						throw new Exception(this.ident + ": return value: Base type must be the refinement type that this method has in the super class.");
					}
				}
			}
		}else{//�֐��̏ꍇ
			//��d��⿌^��ݒ肵�Ă��Ȃ������m�F���Ĕ��f�����邾��
			//⿌^�̒��g�̏�����method_decl�ł��
			method_decl md = cu.search_method(class_decl.class_name, this.ident);
			//�Ԃ�l��⿌^
			if(this.type_or_refinement_type.refinement_type != null){
				if(md.type_spec.refinement_type_clause==null){
					md.type_spec.refinement_type_clause = new refinement_type_clause();
					md.type_spec.refinement_type_clause.refinement_type = this.type_or_refinement_type.refinement_type;
				}else{
					throw new Exception("method " + this.ident + "already have refinementtype.");
				}
			}else if(this.type_or_refinement_type.type != null){
				if(this.type_or_refinement_type.type.type.equals(md.type_spec.type.type)){//⿌^�ł͂Ȃ�
					//���ɉ������Ȃ�
				}else if(md.type_spec.refinement_type_clause==null){
					md.type_spec.refinement_type_clause = new refinement_type_clause();
					md.type_spec.refinement_type_clause.ident = this.type_or_refinement_type.type.type;
				}else{
					throw new Exception("method " + this.ident + "already have refinementtype.");
				}
			}
			
			//������⿌^
			if(md.formals.param_declarations.size() != this.param_override_list.param_overrides.size()){
				throw new Exception("invalid number of arguments.");
			}
			for(int i = 0; i < md.formals.param_declarations.size(); i++){
				param_declaration pd = md.formals.param_declarations.get(i);
				Pair<String, type_or_refinement_type> rt = this.param_override_list.param_overrides.get(i);
				
				if(rt.snd.refinement_type != null){
					if(pd.type_spec.refinement_type_clause==null){
						pd.type_spec.refinement_type_clause = new refinement_type_clause();
						pd.type_spec.refinement_type_clause.refinement_type = rt.snd.refinement_type;
					}else{
						throw new Exception("method " + this.ident + "already have refinementtype.");
					}
				}else if(rt.snd != null){
					if(rt.snd.type.type.equals(pd.type_spec.type.type)){//⿌^�ł͂Ȃ�
						//���ɉ������Ȃ�
					}else if(pd.type_spec.refinement_type_clause==null){
						pd.type_spec.refinement_type_clause = new refinement_type_clause();
						pd.type_spec.refinement_type_clause.ident = rt.snd.type.type;
					}else{
						throw new Exception("method " + this.ident + "already have refinementtype.");
					}
				}
				
			}
		}
		
		
	}

}
