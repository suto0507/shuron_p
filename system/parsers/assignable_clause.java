package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class assignable_clause implements Parser<String>{
	store_ref_list store_ref_list;
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		st = st + new string("assignable").parse(s, ps);
		st = st + new spaces().parse(s, ps);
		this.store_ref_list = new store_ref_list();
		st = st + this.store_ref_list.parse(s, ps);
		st = st + new spaces().parse(s, ps);
		st = st + new string(";").parse(s, ps);
		return st;
	}
	

}
