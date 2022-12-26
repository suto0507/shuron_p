package system.parsers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import system.Check_status;
import system.Option;
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
	
	public void preprocessing(List<Pair<String, String>> extends_pairs, Summery summery){
		pure_modifier();
		link_inheritance(extends_pairs, summery);
	}
	
	public void link_inheritance(List<Pair<String, String>> extends_pairs, Summery summery){
		try {
			//全てのクラスのsuper_classフィールドを埋める
			for(Pair<String, String> extends_pair : extends_pairs){
				for(class_declaration class_decl :classes){
					String extends_class_name = extends_pair.get_snd(class_decl.class_name);
					if(extends_class_name!=null){//継承の処理
						class_declaration extends_class = this.search_class(extends_class_name);
						if(extends_class == null){
							throw new Exception(extends_class + "don't exist");
						}
						class_decl.super_class = extends_class;
						break;
					}
				}
			}
			
			
			//並び替え
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
			
			//メソッドの継承
			for(class_declaration class_decl :classes){
				if(class_decl.super_class != null){
					//メソッド
					for(method_decl super_md : class_decl.super_class.class_block.method_decls){//スーパークラスの各メソッドに関して、オーバーライドされているかによって処理を行う
						method_decl override_md = null;
						for(method_decl md : class_decl.class_block.method_decls){
							if(super_md.ident.equals(md.ident)){
								override_md = md;
								break;
							}
						}
						
						if(override_md == null){//同じメソッドでもサブクラスで検証は行う
							class_decl.class_block.method_decls.add(super_md.clone_no_refinemet_type());
						}else{//事前条件、事後条件などの継承
							if(super_md.method_specification!=null){
								if(override_md.method_specification!=null && override_md.method_specification.spec_case_seq!=null){
									throw new Exception("need also");
								}else if(override_md.method_specification!=null && override_md.method_specification.extending_specification!=null){
									override_md.method_specification.spec_case_seq = new spec_case_seq();
									override_md.method_specification.spec_case_seq.generic_spec_cases = new ArrayList<generic_spec_case>();
									for(generic_spec_case gsc : super_md.method_specification.spec_case_seq.generic_spec_cases){//スーパークラスは既にspec_case_seqで確定しているはず
										override_md.method_specification.spec_case_seq.generic_spec_cases.add(gsc);
									}
									for(generic_spec_case gsc : override_md.method_specification.extending_specification.spec_case_seq.generic_spec_cases){//このクラスのもともとあったもの
										override_md.method_specification.spec_case_seq.generic_spec_cases.add(gsc);
									}
								}else{//なんも書いてない場合
									override_md.method_specification = super_md.method_specification;
								}
							}else{//スーパークラスのメソッドに仕様はない場合
								if(override_md.method_specification!=null && override_md.method_specification.extending_specification!=null){
									throw new Exception("super method don't have specification.");
								}
							}
						}
					}
				}
			}
			
			
			
			//不変条件の継承
			for(class_declaration class_decl :classes){
				if(class_decl.super_class != null){
					for(invariant inv : class_decl.super_class.class_block.invariants){
						class_decl.class_block.invariants.add(inv);
					}
				}
			}
			//篩型の継承に関する処理をする
			for(class_declaration class_decl :classes){
				if(class_decl.super_class != null){
					//override-refinement type-clause
					for(override_refinement_type_clause ortc : class_decl.class_block.override_refinement_type_clauses){
						ortc.inheritance_refinement_types(class_decl, this);
					}
					//メソッド
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
	

	public void pure_modifier(){
		for(class_declaration class_decl : classes){
			for(method_decl md : class_decl.class_block.method_decls){
				md.pure_modifier();
			}
			
		}
	}
	
	public void add_constructor(){
		for(class_declaration class_decl :classes){
			//書く
		}
	}
	
	public void check(Option option, Summery summery) throws Exception{
		for(class_declaration class_decl :classes){
			try{
				class_decl.check(option, this, summery);
			}catch(Exception e){
				System.out.println(e);
				System.out.println("class " + class_decl.class_name + " is wrong");
				summery.invalid_classes.add("(class : " + class_decl.class_name + ")" + " " + summery.file.toString());
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
	
	//オーバーロード対応版
	public method_decl search_method(String class_name, String method_name , ArrayList<String> arg_types){
		for(class_declaration cd : classes){
			if(cd.class_name.equals(class_name)){
				for(method_decl md : cd.class_block.method_decls){
					if(md.ident.equals(method_name) && md.formals.param_declarations.size() == arg_types.size()){
						boolean euqal_types = true;
						for(int i = 0; i < md.formals.param_declarations.size(); i++){
							if(!md.formals.param_declarations.get(i).type_spec.type.type.equals(arg_types.get(i))){
								euqal_types = false;
								break;
							}
						}
						
						if(euqal_types) return md;
					}
				}
				class_declaration super_class = cd;
				while(super_class.super_class != null){
					super_class = super_class.super_class;
					for(method_decl md : super_class.class_block.method_decls){
						if(md.ident.equals(method_name)){
							boolean euqal_types = true;
							for(int i = 0; i < md.formals.param_declarations.size(); i++){
								if(!md.formals.param_declarations.get(i).type_spec.type.type.equals(arg_types.get(i))){
									euqal_types = false;
									break;
								}
							}
							
							if(euqal_types) return md;
						}
					}
				}
			}
		}
		return null;
	}
	
	public represents_clause search_represents_clause(String class_name, String ident, String this_class_name){
		for(class_declaration cd : classes){
			if(cd.class_name.equals(class_name)){
				for(represents_clause rc : cd.class_block.represents_clauses){
					if(rc.ident.equals(ident) && !(rc.is_private && !class_name.equals(this_class_name))){
						return rc;
					}
				}
				class_declaration super_class = cd;
				while(super_class.super_class != null){
					super_class = super_class.super_class;
					for(represents_clause rc : super_class.class_block.represents_clauses){
						if(rc.ident.equals(ident) && !(rc.is_private && super_class.class_name.equals(this_class_name))){
							return rc;
						}
					}
				}
			}
		}
		return null;
	}

	
	public variable_definition search_field(String class_name, String field_name, boolean is_model){
		for(class_declaration cd : classes){
			if(cd.class_name.equals(class_name)){
				for(variable_definition vd : cd.class_block.variable_definitions){
					if(vd.variable_decls.ident.equals(field_name) && vd.modifiers.is_model==is_model){
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
