package system.parsers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import system.Check_status;
import system.Pair;
import system.Parser;
import system.Parser_status;
import system.Source;
import system.Summery;

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
	
	public void link_inheritance(List<Pair<String, String>> extends_pairs, Summery summery){
		try {
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
			
			
			//���ёւ�
			List<class_declaration> sorted_classes = new ArrayList<class_declaration>();
			while(classes.size()!=0){
				for(class_declaration class_decl : classes){
					if(class_decl.super_class==null || sorted_classes.contains(class_decl.super_class)){
						sorted_classes.add(class_decl);
					}
				}
				for(class_declaration class_decl : sorted_classes){
					classes.remove(class_decl);
				}
			}
			classes = sorted_classes;
			
			//���\�b�h�̌p��
			for(class_declaration class_decl :classes){
				if(class_decl.super_class != null){
					//���\�b�h
					for(method_decl super_md : class_decl.super_class.class_block.method_decls){//�X�[�p�[�N���X�̊e���\�b�h�Ɋւ��āA�I�[�o�[���C�h����Ă��邩�ɂ���ď������s��
						method_decl override_md = null;
						for(method_decl md : class_decl.class_block.method_decls){
							if(super_md.ident.equals(md.ident)){
								override_md = md;
								break;
							}
						}
						
						if(override_md == null){//�������\�b�h�ł��T�u�N���X�Ō��؂͍s��
							class_decl.class_block.method_decls.add(super_md);
						}else{//���O�����A��������Ȃǂ̌p��
							if(super_md.method_specification!=null){
								if(override_md.method_specification!=null && override_md.method_specification.spec_case_seq!=null){
									throw new Exception("need also");
								}else if(override_md.method_specification!=null && override_md.method_specification.extending_specification!=null){
									override_md.method_specification.spec_case_seq.generic_spec_cases = new ArrayList<generic_spec_case>();
									for(generic_spec_case gsc : super_md.method_specification.spec_case_seq.generic_spec_cases){//�X�[�p�[�N���X�͊���spec_case_seq�Ŋm�肵�Ă���͂�
										override_md.method_specification.spec_case_seq.generic_spec_cases.add(gsc);
									}
									for(generic_spec_case gsc : super_md.method_specification.extending_specification.spec_case_seq.generic_spec_cases){
										override_md.method_specification.spec_case_seq.generic_spec_cases.add(gsc);
									}
								}else{//�Ȃ�������ĂȂ��ꍇ
									override_md.method_specification = super_md.method_specification;
								}
							}else{//�X�[�p�[�N���X�̃��\�b�h�Ɏd�l�͂Ȃ��ꍇ
								if(override_md.method_specification!=null && override_md.method_specification.extending_specification!=null){
									throw new Exception("super method don't have specification.");
								}
							}
						}
					}
				}
			}
			
			//�s�Ϗ����̌p��
			for(class_declaration class_decl :classes){
				if(class_decl.super_class != null){
					for(invariant inv : class_decl.super_class.class_block.invariants){
						class_decl.class_block.invariants.add(inv);
					}
				}
			}
			//⿌^�̌p���Ɋւ��鏈��������
			for(class_declaration class_decl :classes){
				if(class_decl.super_class != null){
					//override-refinement type-clause
					for(override_refinement_type_clause ortc : class_decl.class_block.override_refinement_type_clauses){
						ortc.inheritance_refinement_types(class_decl, this);
					}
					//���\�b�h
					for(method_decl md : class_decl.class_block.method_decls){
						md.inheritance_refinement_types(class_decl, this);
					}
					
				}
				
			}
		}catch(Exception e){
			System.out.println(e);
			summery.inheritance_faileds.add("Inheritance failed : "  + summery.file.toString());
		}
		
	}
	
	public void check(int deep_limit, Summery summery) throws Exception{
		for(class_declaration class_decl :classes){
			try{
				Check_status cs = new Check_status(this);
				cs.refinement_deep_limmit = deep_limit;
				class_decl.check(cs, summery);
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
				refinement_type_clause rc = null;
				for(variable_definition vd : cd.class_block.variable_definitions){
					if(vd.variable_decls.ident.equals(field_name)){
						if(rc != null) vd.variable_decls.type_spec.refinement_type_clause = rc;
						return vd;
					}
				}
				class_declaration super_class = cd;
				while(super_class.super_class != null){
					//override_refinement_type
					for(override_refinement_type_clause ortc : super_class.class_block.override_refinement_type_clauses){
						if(ortc.param_override_list==null && ortc.ident.equals(field_name)){
							if(ortc.type_or_refinement_type.type != null && rc == null){
								rc = new refinement_type_clause();
								rc.ident = ortc.type_or_refinement_type.type.type;
							}else if(ortc.type_or_refinement_type.refinement_type != null && rc == null){
								rc = new refinement_type_clause();
								rc.refinement_type = ortc.type_or_refinement_type.refinement_type;
							}
						}
					}
					
					super_class = super_class.super_class;
					for(variable_definition vd : super_class.class_block.variable_definitions){
						if(vd.variable_decls.ident.equals(field_name)){
							if(rc != null) vd.variable_decls.type_spec.refinement_type_clause = rc;
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
