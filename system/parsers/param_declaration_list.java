package system.parsers;

import java.util.ArrayList;
import java.util.List;

import system.Parser;
import system.Parser_status;
import system.Source;

public class param_declaration_list implements Parser<String>{
	List<param_declaration> param_declarations;
	public String parse(Source s,Parser_status ps)throws Exception{
		this.param_declarations = new ArrayList<param_declaration>();
		
		String st = "";
		param_declaration pd = new param_declaration();
		st = st + pd.parse(s, ps);
		this.param_declarations.add(pd);
		
		Source s_backup = s.clone();
		try {
			while(true){
				String st2 = "";
				s_backup = s.clone();
				st2 = st2 + new spaces().parse(s, ps);
				st2 = st2 + new string(",").parse(s, ps);
				st2 = st2 + new spaces().parse(s, ps);
				pd = new param_declaration();
				st2 = st2 + pd.parse(s, ps);
				st = st + st2;
				this.param_declarations.add(pd);
			}
		}catch (Exception e){
			s.revert(s_backup);
		}	
		
		return st;
	}
	

}
