package system.parsers;

import system.Parser_status;
import system.Source;

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
				dims = new dims().parse(s, ps);
				st = st + "[]";
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
}
