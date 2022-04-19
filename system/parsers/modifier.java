package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class modifier implements Parser<String>{
	
	public String parse(Source s,Parser_status ps)throws Exception{
		String st;
		Source s_backup = s.clone();
		try{
			st = new string("private").parse(s,ps);
		}catch (Exception e){
			s.revert(s_backup);
			try{
				st = new string("final").parse(s,ps);
			}catch (Exception e2){
				s.revert(s_backup);
				st = new jml_anotation(new jml_modifier()).parse(s,ps);
			}
		}
		
		return st;
	}

}
