package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Reserved_words;
import system.Source;

public class ident implements Parser<String>{
	String ident;
	
	ident(){
		this.ident = "";
	}
	
	public String parse(Source s,Parser_status ps)throws Exception{
		this.ident = this.ident + new letter().parse(s,ps);
		Reserved_words rs = new Reserved_words();
		Source s_backup = s.clone();
		try{
			while(true){
				s_backup = s.clone();
				this.ident = this.ident + new letter_or_digit().parse(s,ps);
			}
		}catch (Exception e){
			s.revert(s_backup);
		}
		for(String r_word : rs.reserved_words){
			if(r_word.equals(ident))throw new Exception("reserved word");
		}
		return ident;
	}
	
}	
