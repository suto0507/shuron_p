package system.parsers;

import java.util.ArrayList;

import system.Parser;
import system.Parser_status;
import system.Source;

public class in_group_clause  implements Parser<String>{
	
	ArrayList<group_name> group_names;
	
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = new string("in").parse(s, ps);
		st = st + new spaces().parse(s, ps);
		group_list gl = new group_list();
		st = st + gl.parse(s, ps);
		st = st + new spaces().parse(s, ps);
		st = st + new string(";").parse(s, ps);
		this.group_names = gl.group_names;
		return st;
	}
}
