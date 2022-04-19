package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class string implements Parser<String>{
	String string;
	
	string(String s){
		this.string = s;
	}
	
	public String parse(Source s,Parser_status ps) throws Exception{
		for (int i=0; i<string.length(); i++){
			new cha(string.charAt(i)).parse(s,ps);
		}
		return string;
	}
}