package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class primary_suffix implements Parser<String>{
	boolean is_field,is_index,is_method;
	String ident;
	expression_list expression_list;
	expression expression;
	primary_suffix(){
		this.is_field = false;
		this.is_index = false;
		this.is_method = false;
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
			s_backup = s.clone();
			try{

				st = new string("[").parse(s, ps);
				st = st + new spaces().parse(s, ps);
				expression ex = new expression();
				st = st + ex.parse(s, ps);
				st = st + new spaces().parse(s, ps);
				st = st + new string("]").parse(s, ps);
				this.expression = ex;
				this.is_index = true;
			}catch (Exception e2){
				s.revert(s_backup);
				st = st + new string("(").parse(s, ps);
				st = st + new spaces().parse(s, ps);
				Source s_backup2 = s.clone();
				expression_list el = null;
				try{
					el = new expression_list();
					st = st + el.parse(s,ps);
					st = st + new spaces().parse(s, ps);
				}catch (Exception e2_1){
					s.revert(s_backup2);
				}
				st = st + new string(")").parse(s, ps);
				if(ps.in_jml){
					throw new Exception("cant use method in jml");
				}
				this.expression_list = el;
				this.is_method = true;
			}
		}
		
		return st;
	}

}
