package system.parsers;

import java.util.ArrayList;

import system.Parser;
import system.Parser_status;
import system.Source;

public class jml_data_group_clause  implements Parser<String>{
	
	ArrayList<group_name> group_names;
	
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		in_group_clause igc = new in_group_clause();
		st = st + igc.parse(s, ps);
		this.group_names = igc.group_names;
		return st;
	}

}
