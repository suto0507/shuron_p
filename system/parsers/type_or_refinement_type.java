package system.parsers;

import system.Pair;
import system.Parser_status;
import system.Source;
import system.Type_info;

public class type_or_refinement_type {
	String st;
	type type;
	int dims;
	refinement_type refinement_type;
	
	public String parse(Source s,Parser_status ps)throws Exception{
		st = "";
		Source s_backup = s.clone();
		try{
			type t = new type();
			st = st + t.parse(s, ps);
			this.type = t;
			Source s_backup2 = s.clone();
			try{
				dims d = new dims();
				st = st + d.parse(s, ps);
				dims = d.dims;
			}catch (Exception e2){
				s.revert(s_backup2);
			}
		}catch (Exception e){
			s.revert(s_backup);
			refinement_type rt = new refinement_type();
			st = st + rt.parse(s, ps);
			this.refinement_type = rt;
		}
		
		return this.st;
	}
	
	public Type_info type_info(class_declaration class_decl, compilation_unit cu) throws Exception{
		if(this.refinement_type!=null){
			Pair<String, Integer> base_type = refinement_type.base_type(cu);
			return new Type_info(base_type.fst, base_type.snd);
		}else{
			refinement_type rt = cu.search_refinement_type(class_decl.class_name, type.type);
			if(rt != null){
				Pair<String, Integer> base_type = rt.base_type(cu);
				return new Type_info(base_type.fst, base_type.snd);
			}else{
				return new Type_info(type.type, dims);
			}
		}
	}
}
