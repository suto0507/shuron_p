package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class build_in_type implements Parser<String>{
	String type;
	public String parse(Source s,Parser_status ps)throws Exception{
		Source s_backup = s.clone();
		try{
			this.type = new string("void").parse(s,ps);
		}catch (Exception e){
			s.revert(s_backup);
			try{
				this.type = new string("boolean").parse(s,ps);
			}catch (Exception e2){
				s.revert(s_backup);
				this.type = new string("int").parse(s,ps);
			}
		}
		
		return this.type;
	}
}