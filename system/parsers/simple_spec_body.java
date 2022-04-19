package system.parsers;

import java.util.ArrayList;
import java.util.List;

import system.Parser;
import system.Parser_status;
import system.Source;
public class simple_spec_body implements Parser<String>{
	List<ensures_clause> ensures;
	List<assignable_clause> assignables;
	boolean assignable_nothing;
	
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		this.assignable_nothing = false;
		
		this.ensures = new ArrayList<ensures_clause>();
		this.assignables = new ArrayList<assignable_clause>();
		
		simple_spec_body_clause ssbc = new simple_spec_body_clause();
		st = st + ssbc.parse(s, ps);
		Parser p = ssbc.get_p();
		if(p instanceof ensures_clause){
			ensures.add((ensures_clause) p);
		}else if(p instanceof assignable_clause){
			assignables.add((assignable_clause) p);
		}
		
		Source s_backup = s.clone();
		try {
			while(true){
				s_backup = s.clone();
				
				ssbc = new simple_spec_body_clause();
				st = st + ssbc.parse(s, ps);
				p = ssbc.get_p();
				if(p instanceof ensures_clause){
					ensures.add((ensures_clause) p);
				}else if(p instanceof assignable_clause){
					assignables.add((assignable_clause) p);
					if(((assignable_clause)p).store_ref_list.is_nothing){
						this.assignable_nothing = true;
					}
				}
			}
		}catch (Exception e){
			s.revert(s_backup);
		}
		
		return st;
	}

}

