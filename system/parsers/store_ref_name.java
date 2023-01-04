package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class store_ref_name implements Parser<String>{
	String ident;
	boolean is_this;
	boolean is_super;
	
	store_ref_name(){
		this.is_this = false;
		this.is_super = false;
	}
	
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		Source s_backup = s.clone();
		try{
			this.ident = new ident().parse(s, ps);
			st = st + this.ident;
		}catch (Exception e5){
			s.revert(s_backup);
			s_backup = s.clone();
			try{
				new string("this").parse(s, ps);
				this.is_this = true;
				st = st + "this";
			}catch (Exception e6){
				s.revert(s_backup);
				new string("super").parse(s, ps);
				this.is_super = true;
				st = st + "super";
			}
		}
		return st;
	}

}
