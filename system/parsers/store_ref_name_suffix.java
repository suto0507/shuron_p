package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class store_ref_name_suffix implements Parser<String>{
	boolean is_field,is_index;
	String ident;
	spec_array_ref_expr spec_array_ref_expr;
	store_ref_name_suffix(){
		this.is_field = false;
		this.is_index = false;
	}
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		Source s_backup = s.clone();
		try{
			st = new string(".").parse(s, ps);
			this.ident = new ident().parse(s, ps);;
			st = st + this.ident;
			this.is_field = true;
		}catch (Exception e){
			s.revert(s_backup);
			st = new string("[").parse(s, ps);
			st = st + new spaces().parse(s, ps);
			spec_array_ref_expr sare = new spec_array_ref_expr();
			st = st + sare.parse(s, ps);
			this.spec_array_ref_expr = sare;
			this.is_index = true;
			st = st + new spaces().parse(s, ps);
			st = st + new string("]").parse(s, ps);
		}
		
		return st;
	}

}
