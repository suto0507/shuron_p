package system.parsers;

import java.util.ArrayList;

import system.Parser;
import system.Parser_status;
import system.Source;

public class group_list  implements Parser<String>{
	
	ArrayList<group_name> group_names;
	
	public String parse(Source s,Parser_status ps)throws Exception{
		this.group_names = new ArrayList<group_name>();
		
		String st = "";
		group_name gn = new group_name();
		st = st + gn.parse(s, ps);
		this.group_names.add(gn);
		
		Source s_backup = s.clone();
		try {
			while(true){
				s_backup = s.clone();
				String st2 = new spaces().parse(s, ps);
				st2 = st2 + new string(",").parse(s, ps);
				st2 = st2 + new spaces().parse(s, ps);
				gn = new group_name();
				st2 = st2 + gn.parse(s, ps);
				st = st + st2;
				this.group_names.add(gn);
			}
		}catch (Exception e){
			s.revert(s_backup);
		}	
		
		return st;
	}
}
