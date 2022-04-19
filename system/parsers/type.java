package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class type implements Parser<String>{
	Parser p;
	//int dim;
	public String type;
	//type(){
	//	dim = 0;
	//}
	public String parse(Source s,Parser_status ps)throws Exception{
		this.type = "";
		Source s_backup = s.clone();
		try{
			p = new reference_type();
			this.type = ((reference_type)p).parse(s,ps);
		}catch (Exception e){
			s.revert(s_backup);
			p = new build_in_type();
			this.type = ((build_in_type)p).parse(s,ps);
		}
		return this.type;
	}
}