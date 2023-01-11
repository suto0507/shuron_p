package system.parsers;

import java.util.ArrayList;
import java.util.Random;

import system.Check_status;
import system.Field;
import system.Pair;
import system.Parser;
import system.Parser_status;
import system.Source;
import system.Type_info;
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
		
		
		
		st = st + new spaces().parse(s,ps);
		st = st + new string("(").parse(s,ps);
		st = st + new spaces().parse(s,ps);
		
		Source s_backup2 = s.clone();
		try{
			String st2 = "";
			param_override_list pol = new param_override_list();
			st2 = st2 + pol.parse(s,ps);
			this.param_override_list = pol;
			st2 = st2 + new spaces().parse(s,ps);
			
			st = st + st2;
		}catch (Exception e){
			s.revert(s_backup2);
		}
		
		st = st + new string(")").parse(s,ps);
		if(this.param_override_list == null){
			this.param_override_list = new param_override_list();
			this.param_override_list.param_overrides = new ArrayList<Pair<String, type_or_refinement_type>>();
		}
		
		st = st + new spaces().parse(s,ps);
		st = st + new string(";").parse(s,ps);
		
		return this.st;
	}
	
	public void inheritance_refinement_types(class_declaration class_decl, compilation_unit cu) throws Exception{
		System.out.println("check override : " + this.st);
		
		class_declaration super_class = class_decl.super_class;
		
		ArrayList<Type_info> param_types = new ArrayList<Type_info>();
		for(int i = 0; i < this.param_override_list.param_overrides.size(); i++){
			Pair<String, type_or_refinement_type> rt = this.param_override_list.param_overrides.get(i);
			param_types.add(rt.snd.type_info(class_decl, cu));
		}
		
		method_decl searched_method = cu.search_method(class_decl.class_name, this.ident, param_types, false, class_decl.class_name);
		method_decl searched_super_method = cu.search_method(super_class.class_name, this.ident, param_types, false, class_decl.class_name);
		if(searched_method != searched_super_method){
			throw new Exception("meothod " + this.ident + "defined in class " + class_decl.class_name);
		}
		
		method_decl md = searched_super_method.clone_no_refinemet_type();
		//md.class_type_name = class_decl.class_name;
		md.file_path = class_decl.file_path;
		class_decl.class_block.method_decls.add(md);
	
		//ŠÖ”‚Ìê‡
		//“ñd‚Éâ¿Œ^‚ğİ’è‚µ‚Ä‚¢‚È‚¢‚©‚ğŠm”F‚µ‚Ä”½‰f‚³‚¹‚é‚¾‚¯
		//â¿Œ^‚Ì’†g‚Ìˆ—‚Ímethod_decl‚Å‚â‚é
		//•Ô‚è’l‚Ìâ¿Œ^
		if(this.type_or_refinement_type.refinement_type != null){
			if(md.type_spec.refinement_type_clause==null){
				md.type_spec.refinement_type_clause = new refinement_type_clause();
				md.type_spec.refinement_type_clause.refinement_type = this.type_or_refinement_type.refinement_type;
			}else{
				throw new Exception("method " + this.ident + "already have refinement type.");
			}
		}else if(this.type_or_refinement_type.type != null){
			if(this.type_or_refinement_type.type.type.equals(md.type_spec.type.type)){//â¿Œ^‚Å‚Í‚È‚¢
				//“Á‚É‰½‚à‚µ‚È‚¢
			}else if(md.type_spec.refinement_type_clause==null){
				md.type_spec.refinement_type_clause = new refinement_type_clause();
				md.type_spec.refinement_type_clause.ident = this.type_or_refinement_type.type.type;
			}else{
				throw new Exception("method " + this.ident + "already have refinement type.");
			}
		}
		
		//ˆø”‚Ìâ¿Œ^
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
					throw new Exception("method " + this.ident + "already have refinement type.");
				}
			}else if(rt.snd != null){
				if(rt.snd.type.type.equals(pd.type_spec.type.type)){//â¿Œ^‚Å‚Í‚È‚¢
					//“Á‚É‰½‚à‚µ‚È‚¢
				}else if(pd.type_spec.refinement_type_clause==null){
					pd.type_spec.refinement_type_clause = new refinement_type_clause();
					pd.type_spec.refinement_type_clause.ident = rt.snd.type.type;
				}else{
					throw new Exception("method " + this.ident + "already have refinement type.");
				}
			}
			
		}
	}

}
