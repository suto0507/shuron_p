package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class jml_anotation implements Parser<String>{
	Parser p;
	jml_anotation(Parser p){
		this.p = p;
	}
	public String parse(Source s,Parser_status ps)throws Exception{
		boolean pre_in_jml = ps.in_jml;
		ps.in_jml = true;
		try{
			String st = "";
			st = st + p.parse(s,ps);
			ps.in_jml = pre_in_jml;
			return st;
		}catch(Exception e){
			ps.in_jml = pre_in_jml;
			throw new Exception(e);
		}
	}
}
