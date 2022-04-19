package system.parsers;

import java.util.ArrayList;
import java.util.List;

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
		//�S�ẴN���X��extends_class�t�B�[���h�𖄂߂�
		for(Pair<String, String> extends_pair : extends_pairs){
			for(class_declaration class_decl :classes){
				String extends_class_name = extends_pair.get_snd(class_decl.class_name);
				if(extends_class_name!=null){//�p���̏���
					class_declaration extends_class = this.search_class(extends_class_name);
					if(extends_class == null){
						throw new Exception(extends_class + "don't exist");
					}
					class_decl.extends_class = extends_class;
					break;
				}
			}
		}
		
		//⿌^�̌p���Ɋւ��鏈��������
		for(class_declaration class_decl :classes){
			if(class_decl.extends_class != null){
				//���\�b�h
				for(method_decl md : class_decl.class_block.method_decls){
					//�Ԃ�l�̌^��⿌^
					if(md.type_spec.refinement_type_clause!=null){
						if(md.type_spec.refinement_type_clause.ident!=null){//ident�̏ꍇ�͐e��⿌^�������Ă��Ȃ��A�܂��͐e��ident������
							class_declaration extends_class = class_decl.extends_class;
							while(true){
								method_decl super_md = this.search_method(extends_class.class_name, md.ident);
								if(super_md == null || super_md.type_spec.refinement_type_clause==null){//⿌^��������܂�super class��T��
									if(extends_class.extends_class == null){
										break;//�e��⿌^���Ȃ��Ȃ�ǂ�
									}else{
										extends_class = extends_class.extends_class;
									}
								}else{
									if(super_md.type_spec.refinement_type_clause.ident!=null){//�e��ident
										if(super_md.type_spec.refinement_type_clause.ident.equals(md.type_spec.refinement_type_clause.ident)){
											break;//�����Ȃ�悢
										}else if(){
											
										}else{
											throw new Exception("Base type must be the refinement type that this method has in the super class.");
										}
									}
								}
							}
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
			}
		}
		return null;
	}
	
}
