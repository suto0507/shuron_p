package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class store_ref_name implements Parser<String>{
	String ident;
	boolean is_this;
	
	store_ref_name(){
		this.is_this = false;
	}
	
	public String parse(Source s,Parser_status ps)throws Exception{
		this.ident = new ident().parse(s, ps);
		if(this.ident.equals("this")) this.is_this = true;
		return this.ident;
	}

}
