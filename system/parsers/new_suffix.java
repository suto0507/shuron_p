package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class new_suffix implements Parser<String>{
	boolean is_index,is_constructor;
	expression_list expression_list;
	array_decl array_decl;
	new_suffix(){
		this.is_index = false;
		this.is_constructor = false;
	}
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		Source s_backup = s.clone();

		try{
			array_decl array_decl = new array_decl();
			st = st + array_decl.parse(s, ps);
			this.array_decl = array_decl;
			this.is_index = true;
		}catch (Exception e2){
			s.revert(s_backup);
			st = st + new string("(").parse(s, ps);
			st = st + new spaces().parse(s, ps);
			expression_list el = new expression_list();
			Source s_backup2 = s.clone();
			try{
				st = st + el.parse(s,ps);
				st = st + new spaces().parse(s, ps);
			}catch (Exception e){
				s.revert(s_backup2);
			}
			st = st + new string(")").parse(s, ps);
			if(ps.in_jml){
				throw new Exception("can't use constructor in jml");
			}
			this.expression_list = el;
			this.is_constructor = true;
		}


		return st;
	}
	


}
