package system.parsers;

import java.util.ArrayList;
import java.util.List;

import system.Parser;
import system.Parser_status;
import system.Source;

public class store_ref_list implements Parser<String>{
	List<store_ref_expression> store_ref_expressions;
	boolean is_nothing;
	public String parse(Source s,Parser_status ps)throws Exception{
		this.store_ref_expressions = new ArrayList<store_ref_expression>();
		this.is_nothing = false;
		
		String st = "";
		
		Source s_backup = s.clone();
		try{
			st = new string("\\nothing").parse(s, ps);
			this.is_nothing = true;
		}catch (Exception e){
			s.revert(s_backup);
			store_ref_expression sre = new store_ref_expression();
			st = sre.parse(s, ps);
			this.store_ref_expressions.add(sre);
			
			s_backup = s.clone();
			try {
				while(true){
					s_backup = s.clone();
					st = st + new spaces().parse(s, ps);
					st = st + new string(",").parse(s, ps);
					st = st + new spaces().parse(s, ps);
					sre = new store_ref_expression();
					st = st + sre.parse(s, ps);
					this.store_ref_expressions.add(sre);
				}
			}catch (Exception e2){
				s.revert(s_backup);
			}	
		}

		return st;
	}

}