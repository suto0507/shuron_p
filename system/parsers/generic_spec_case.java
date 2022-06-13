package system.parsers;

import java.util.ArrayList;
import java.util.List;

import system.Parser;
import system.Parser_status;
import system.Source;

public class generic_spec_case implements Parser<String>{
	spec_header spec_header;
	simple_spec_body simple_spec_body;
	
	public String class_type_name;
	
	generic_spec_case(){
		this.spec_header = null;
		this.simple_spec_body = null;
	}
	
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		Source s_backup = s.clone();
		try{
			spec_header sh = new spec_header();
			st =  st + sh.parse(s, ps);
			this.spec_header = sh;
			
			Source s_backup2 = s.clone();
			try{
				simple_spec_body ssb = new simple_spec_body();
				st =  st + ssb.parse(s, ps);
				this.simple_spec_body = ssb;
			}catch (Exception e){
				s.revert(s_backup2);
			}	
			
		}catch (Exception e){
			s.revert(s_backup);
			
			simple_spec_body ssb = new simple_spec_body();
			st =  st + ssb.parse(s, ps);
			this.simple_spec_body = ssb;
		}
		
		this.class_type_name = ps.class_type_name;
		
		return st;
	}
	
	List<requires_clause> get_requires(){
		if(this.spec_header!=null){
			return this.spec_header.requires;
		}else{
			return null;
		}
	}
	
	List<ensures_clause> get_ensures(){
		if(this.simple_spec_body!=null){
			return this.simple_spec_body.ensures;
		}else{
			return null;
		}
	}
	
	//—v‘f”0 -> nothing      null -> assignable‚ª‚È‚¢(‰½‚Å‚à‘ã“ü‚µ‚Ä‚¢‚¢)
	List<assignable_clause> get_assignable(){
		if(this.simple_spec_body!=null){
			if(this.simple_spec_body.assignable_nothing){
				return new ArrayList<assignable_clause>();
			}
			if(this.simple_spec_body.assignables.size()==0){//assignable‚ª‚È‚¢(‰½‚Å‚à‘ã“ü‚µ‚Ä‚¢‚¢)
				return null;
			}
			return this.simple_spec_body.assignables;
		}else{
			return null;
		}
	}

}

