package system.parsers;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;

import system.Check_status;
import system.Field;
import system.Parser;
import system.Parser_status;
import system.Source;

public class primary_expr implements Parser<String>{
	java_literal java_literal;
	expression bracket_expression;
	new_expr new_expr;
	jml_primary jml_primary;
	String ident;
	boolean is_this;
	
	primary_expr(){
		this.is_this = false;
	}
	
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		Source s_backup = s.clone();
		try{
			java_literal jl = new java_literal();
			st = st + jl.parse(s, ps);
			this.java_literal = jl;
		}catch (Exception e){
			s.revert(s_backup);
			s_backup = s.clone();
			try{
				new_expr ne = new new_expr();
				st = st + ne.parse(s, ps);
				this.new_expr = ne;
			}catch (Exception e2){
				s.revert(s_backup);
				s_backup = s.clone();
				try{
					jml_primary jp = new jml_primary();
					st = st + jp.parse(s, ps);
					this.jml_primary = jp;
				}catch (Exception e3){
					s.revert(s_backup);
					s_backup = s.clone();
					try{
						st = st + new string("(").parse(s, ps);
						st = st + new spaces().parse(s, ps);
						expression ex = new expression();
						st = st + ex.parse(s, ps);
						st = st + new spaces().parse(s, ps);
						st = st + new string(")").parse(s, ps);
						this.bracket_expression = ex;
					}catch (Exception e4){
						s.revert(s_backup);
						s_backup = s.clone();
						try{
							this.ident = new ident().parse(s, ps);
							st = st + this.ident;
						}catch (Exception e5){
							s.revert(s_backup);
							new string("this").parse(s, ps);
							this.is_this = true;
							st = st + "this";
						}
					}
				}
			}
		}
		
		return st;
	}
	
	public boolean have_index_access(Check_status cs){
		if(java_literal!=null){
			return false;
		}else if(bracket_expression!=null){
			return this.bracket_expression.assignment_expr.have_index_access(cs);
		}else if(new_expr != null){
			return false;
		}else if(jml_primary != null){
			return jml_primary.have_index_access(cs);
		}else{//ident‚©this
			return false;
		}
	}
}
