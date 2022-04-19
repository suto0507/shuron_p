package system.parsers;

import java.util.ArrayList;
import java.util.List;

import system.Check_status;
import system.Parser;
import system.Parser_status;
import system.Source;

public class formals implements Parser<String>{
	List<param_declaration> param_declarations;
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		st = st + new string("(").parse(s, ps);
		st = st + new spaces().parse(s, ps);
		
		Source s_backup = s.clone();
		try{
			param_declaration_list pdl = new param_declaration_list();
			st = st + pdl.parse(s, ps);
			this.param_declarations = pdl.param_declarations;
		}catch (Exception e){
			s.revert(s_backup);
			//ñ≥Ç¢èÍçáÇ‡óvëf0ÇÃList
			param_declarations = new ArrayList<param_declaration>() ;
		}
		st = st + new spaces().parse(s, ps);
		st = st + new string(")").parse(s, ps);
		return st;
	}
	
	public void check(Check_status cs) throws Exception{
		for(param_declaration pd : param_declarations){
			pd.check(cs);
		}
	}

}
