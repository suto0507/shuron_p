package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class modifiers implements Parser<String>{
	boolean is_private;
	public boolean is_final;
	boolean is_spec_public;
	boolean is_pure;
	boolean is_helper;
	boolean is_model;
	boolean is_no_refinement_type;
	
	modifiers(){
		this.is_final = false;
		this.is_private = false;
		this.is_spec_public = false;
		this.is_pure = false;
		this.is_helper = false;
		this.is_model = false;
		this.is_no_refinement_type = false;
	}
	
	public String parse(Source s,Parser_status ps)throws Exception{
		String ret = "";
		Source s_backup = s.clone();
		try {
			while(true){
				s_backup = s.clone();
				String st = new modifier().parse(s,ps);
				if(st.equals("private")){
					this.is_private = true;
				}else if(st.equals("final")){
					this.is_final = true;
				}else if(st.equals("spec_public")){
					this.is_spec_public = true;
				}else if(st.equals("pure")){
					this.is_pure = true;
				}else if(st.equals("helper")){
					this.is_helper = true;
				}else if(st.equals("model")){
					this.is_model = true;
				}else if(st.equals("no_refinement_type")){
					this.is_no_refinement_type = true;
				}
				ret = ret + st + new newLines().parse(s,ps);
			}
		}catch (Exception e){
			s.revert(s_backup);
		}
		
		return ret;
		
	}

}
