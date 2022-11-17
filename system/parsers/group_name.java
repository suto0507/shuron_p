package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class group_name implements Parser<String>{
	boolean is_super;
	String ident;
	
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		Source s_backup = s.clone();
		try{
			st = new group_name_prefix().parse(s,ps);
			if(st.equals("super")){
				this.is_super = true;
			}
			st += ".";
		}catch (Exception e){
			s.revert(s_backup);
		}
		this.ident = new ident().parse(s, ps);
		st = st + this.ident;
		
		return st;
		
	}
}
