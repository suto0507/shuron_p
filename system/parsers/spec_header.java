package system.parsers;

import java.util.ArrayList;
import java.util.List;

import system.Parser;
import system.Parser_status;
import system.Source;

public class spec_header implements Parser<String>{
	List<requires_clause> requires;
	
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		requires = new ArrayList<requires_clause>();
		
		st = st + new spaces().parse(s, ps);
		requires_clause rc = new requires_clause();
		st = st + new jml_anotation_newLine(rc).parse(s, ps);
		this.requires.add(rc);
		
		Source s_backup = s.clone();
		try {
			while(true){
				s_backup = s.clone();
				rc = new requires_clause();
				st = st + new spaces().parse(s, ps) + new jml_anotation_newLine(rc).parse(s, ps);
				this.requires.add(rc);
			}
		}catch (Exception e){
			s.revert(s_backup);
		}	
		
		return st;
	}

}
