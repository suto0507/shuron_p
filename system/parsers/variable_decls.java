package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class variable_decls implements Parser<String>{
	public type_spec type_spec;
	public initializer initializer;
	String ident;
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		this.type_spec = new type_spec();
		st = st + this.type_spec.parse(s, ps);
		st = st + new spaces().parse(s, ps);
		this.ident = new ident().parse(s, ps);
		st = st + this.ident;
		Source s_backup = s.clone();
		try{
			String st2 =new spaces().parse(s, ps);
			st2 = st2 + new string("=").parse(s, ps);
			st2 = st2 + new spaces().parse(s, ps);
			initializer initializer = new initializer();
			st2 = st2 + initializer.parse(s, ps);
			st2 = st2 + new spaces().parse(s, ps);
			
			this.initializer = initializer;
			st += st2;
		}catch (Exception e){
			s.revert(s_backup);
		}
		
		return st;
	}
}
