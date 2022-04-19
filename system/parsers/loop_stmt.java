package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class loop_stmt implements Parser<String>{
	local_declaration  local_declaration;
	expression expression;
	expression_list expression_list;
	statement statement;
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		st = st + new string("for").parse(s, ps);
		st = st + new spaces().parse(s, ps);
		st = st + new string("(").parse(s, ps);
		st = st + new spaces().parse(s, ps);
		Source s_backup = s.clone();
		try{
			local_declaration ld = new local_declaration();
			st = st + ld.parse(s, ps);
			this.local_declaration = ld;
		}catch (Exception e){
			s.revert(s_backup);
		}
		st = st + new spaces().parse(s, ps);
		st = st + new string(";").parse(s, ps);
		st = st + new spaces().parse(s, ps);
		s_backup = s.clone();
		try{
			expression ex = new expression();
			st = st + ex.parse(s, ps);
			this.expression = ex;
		}catch (Exception e){
			s.revert(s_backup);
		}
		st = st + new spaces().parse(s, ps);
		st = st + new string(";").parse(s, ps);
		st = st + new spaces().parse(s, ps);
		s_backup = s.clone();
		try{
			expression_list el = new expression_list();
			st = st + el.parse(s, ps);
			this.expression_list = el;
		}catch (Exception e){
			s.revert(s_backup);
		}
		st = st + new spaces().parse(s, ps);
		st = st + new string(")").parse(s, ps);
		st = st + new spaces().parse(s, ps);
		this.statement = new statement();
		st = st + this.statement.parse(s, ps);
		
		return st;
	}
	
}

