package system.parsers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import system.Check_status;
import system.Pair;
import system.Parser;
import system.Parser_status;
import system.Source;

public class compilation_unit implements Parser<String>{
	List<class_declaration> classes;
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		classes = new ArrayList<class_declaration>();
		Source s_backup = s.clone();
		try {
			while(true){
				s_backup = s.clone();
				class_declaration cd = new class_declaration();
				st = st + cd.parse(s,ps);
				classes.add(cd);
			}
		}catch (Exception e){
			s.where();
			s.deepestwhere();
			s.revert(s_backup);
		}
		return st;
	}
	
	public void link_inheritance(List<Pair<String, String>> extends_pairs) throws Exception{
		//�S�ẴN���X��super_class�t�B�[���h�𖄂߂�
		for(Pair<String, String> extends_pair : extends_pairs){
			for(class_declaration class_decl :classes){
				String extends_class_name = extends_pair.get_snd(class_decl.class_name);
				if(extends_class_name!=null){//�p���̏���
					class_declaration extends_class = this.search_class(extends_class_name);
					if(extends_class == null){
						throw new Exception(extends_class + "don't exist");
					}
					class_decl.super_class = extends_class;
					break;
				}
			}
		}
		
		//⿌^�̌p���Ɋւ��鏈��������
		for(class_declaration class_decl :classes){
			if(class_decl.super_class != null){
				//���\�b�h
				for(method_decl md : class_decl.class_block.method_decls){
					//�Ԃ�l�̌^��⿌^
						
					class_declaration super_class = class_decl.super_class;
					while(true){
						method_decl super_md = this.search_method(super_class.class_name, md.ident);
						if(super_md == null || super_md.type_spec.refinement_type_clause==null){//⿌^��������܂�super class��T��
							if(super_class.super_class == null){
								break;//�e��⿌^���Ȃ��Ȃ�ǂ�
							}else{
								super_class = super_class.super_class;
							}
						}else if(md.type_spec.refinement_type_clause!=null){//⿌^�����e�N���X�����������ꍇ�A����⿌^�������Ă���ꍇ
							if(super_md.type_spec.refinement_type_clause.ident!=null){//�e��ident
								String rt_name;
								if(md.type_spec.refinement_type_clause.ident!=null){//ident�̏ꍇ�́A�e��⿌^�������Ă��Ȃ��A�܂��͐e��ident����Base�^�C�v�Ɠ���
									rt_name = md.type_spec.refinement_type_clause.ident;
								}else{//refinement_type�������Ă���ꍇ
									rt_name = md.type_spec.refinement_type_clause.refinement_type.type.type;
									if(rt_name.equals("Super_type")){
										md.type_spec.refinement_type_clause.refinement_type.type.type = super_md.type_spec.refinement_type_clause.ident;
										break;
									}
								}
								while(true){
									if(super_md.type_spec.refinement_type_clause.ident.equals(rt_name)){
										break;//�����Ȃ�悢
									}
									refinement_type rt = this.search_refinement_type(class_decl.class_name, rt_name);
									if(rt == null){
										throw new Exception(md.ident + ": return value: Base type must be the refinement type that this method has in the super class.");
									}
									rt_name = rt.ident;
								}
								break;
							}else{//�e��refinement_type�������Ă���
								if(md.type_spec.refinement_type_clause.refinement_type!=null){//refinement_type�������Ă���ꍇ��Super_type���K�v
									if(md.type_spec.refinement_type_clause.refinement_type.type.type.equals("Super_type")){
										def_type_clause tmp_def_type = new def_type_clause();
										//���O�͈�ӂ̂͂������A����⿌^����錾����|���l�����邩������Ȃ��̂Ő�������������
										tmp_def_type.ident = class_decl.class_name + "_" + md.ident + "_ret_val_tmp_refinement_type" + (new Random().nextInt(88889) + 11111);
										tmp_def_type.refinement_type = super_md.type_spec.refinement_type_clause.refinement_type;
										class_decl.class_block.def_type_clauses.add(tmp_def_type);
										md.type_spec.refinement_type_clause.refinement_type.type.type = tmp_def_type.ident;
										break;
									}	
								}
								throw new Exception(md.ident + ": return value: Base type must be the refinement type that this method has in the super class.");
							}
						}else{//⿌^�����e�N���X�����������ꍇ�A����⿌^�������Ă��Ȃ������ꍇ
							//�e��refinement_type�̃C���X�^���X�����̂܂܎󂯌p��
							md.type_spec.refinement_type_clause = super_md.type_spec.refinement_type_clause;
							break;
						}
					}
				}
			}
			
		}
	}
	
	public void check(int deep_limit) throws Exception{
		for(class_declaration class_decl :classes){
			try{
				Check_status cs = new Check_status(this);
				cs.refinement_deep_limmit = deep_limit;
				class_decl.check(cs);
			}catch(Exception e){
				System.out.println(e);
				System.out.println("class " + class_decl.class_name + " is wrong");
			}
		}
	}
	
	public class_declaration search_class(String class_name){
		for(class_declaration cd : classes){
			if(cd.class_name.equals(class_name)){
				return cd;
			}
		}
		return null;
	}
	
	public method_decl search_method(String class_name, String method_name){
		for(class_declaration cd : classes){
			if(cd.class_name.equals(class_name)){
				for(method_decl md : cd.class_block.method_decls){
					if(md.ident.equals(method_name)){
						return md;
					}
				}
				class_declaration super_class = cd;
				while(super_class.super_class != null){
					super_class = super_class.super_class;
					for(method_decl md : super_class.class_block.method_decls){
						if(md.ident.equals(method_name)){
							return md;
						}
					}
				}
			}
		}
		return null;
	}

	
	public variable_definition search_field(String class_name, String field_name){
		for(class_declaration cd : classes){
			if(cd.class_name.equals(class_name)){
				for(variable_definition vd : cd.class_block.variable_definitions){
					if(vd.variable_decls.ident.equals(field_name)){
						return vd;
					}
				}
				class_declaration super_class = cd;
				while(super_class.super_class != null){
					super_class = super_class.super_class;
					for(variable_definition vd : super_class.class_block.variable_definitions){
						if(vd.variable_decls.ident.equals(field_name)){
							return vd;
						}
					}
				}
			}
		}
		return null;
	}
	
	
	public refinement_type search_refinement_type(String class_name, String type_name){
		for(class_declaration cd : classes){
			if(cd.class_name.equals(class_name)){
				for(def_type_clause dtc : cd.class_block.def_type_clauses){
					if(dtc.ident.equals(type_name)){
						return dtc.refinement_type;
					}
				}
				class_declaration super_class = cd;
				while(super_class.super_class != null){
					super_class = super_class.super_class;
					for(def_type_clause dtc : super_class.class_block.def_type_clauses){
						if(dtc.ident.equals(type_name)){
							return dtc.refinement_type;
						}
					}
				}
			}
		}
		return null;
	}
	
}
