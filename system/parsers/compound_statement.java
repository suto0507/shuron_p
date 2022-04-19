package system.parsers;

import java.util.ArrayList;
import java.util.List;

import system.Check_status;
import system.Parser;
import system.Parser_status;
import system.Source;

public class compound_statement implements Parser<String>{
	
	List<statement> statements;
	public String parse(Source s,Parser_status ps)throws Exception{
		this.statements = new ArrayList<statement>();
		
		String st = "";
		st = st + new string("{").parse(s, ps);
		st = st + new newLines().parse(s, ps);
		st = st + new spaces().parse(s, ps);
		statement sta = new statement();
		st = st + sta.parse(s, ps);
		this.statements.add(sta);
		st = st + new newLines().parse(s, ps);
		
		Source s_backup = s.clone();
		try {
			while(true){
				String st2 = "";
				s_backup = s.clone();
				st2 = st2 + new spaces().parse(s, ps);
				sta = new statement();
				st2 = st2 + sta.parse(s, ps);
				this.statements.add(sta);
				st2 = st2 + new newLines().parse(s, ps);
				st = st + st2;
			}
		}catch (Exception e){
			s.revert(s_backup);
		}
		
		st = st + new spaces().parse(s, ps);
		st = st + new string("}").parse(s, ps);
		st = st + new newLines().parse(s, ps);
		
		return st;
	}
	
	//ÇΩÇ‘ÇÒvoidÇ≈ÇÊÇ¢ÅH
	public void check(Check_status cs) throws Exception{
		for(statement statement : statements){
			statement.check(cs);
		}
	}
	
}

