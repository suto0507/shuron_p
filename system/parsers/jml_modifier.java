package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class jml_modifier implements Parser<String>{
	
	public String parse(Source s,Parser_status ps)throws Exception{
		String st;
		Source s_backup = s.clone();
		try{
			st = new string("spec_public").parse(s,ps);
		}catch (Exception e){
			try{
				s.revert(s_backup);
				st = new string("model").parse(s,ps);
			}catch (Exception e2){
				try{
					s.revert(s_backup);
					st = new string("pure").parse(s,ps);
				}catch (Exception e3){
					s.revert(s_backup);
					st = new string("helper").parse(s,ps);
				}
			}
		}
		return st;
	}

}
