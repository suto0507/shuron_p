package system.parsers;

import system.Check_status;
import system.Parser;
import system.Parser_status;
import system.Source;

public class jml_primary implements Parser<String>{
	boolean is_result;
	old_expression old_expression;
	spec_quantified_expr spec_quantified_expr;
	jml_primary(){
		is_result = false;
	}
	public String parse(Source s,Parser_status ps)throws Exception{
		Source s_backup = s.clone();
		String st;
		try{
			st = new result_expression().parse(s,ps);
			this.is_result = true;
			if(ps.in_ensures!=true || ps.in_jml!=true){
				throw new Exception("result should be used in ensures");
			}
		}catch (Exception e){
			s.revert(s_backup);
			s_backup = s.clone();
			try{
				old_expression oe = new old_expression();
				st = oe.parse(s,ps);
				this.old_expression = oe;
				if(ps.in_ensures!=true || ps.in_jml!=true){
					throw new Exception("old should be used in ensures");
				}
			}catch (Exception e2){
				s.revert(s_backup);
				spec_quantified_expr sqe = new spec_quantified_expr();
				st = sqe.parse(s, ps);
				this.spec_quantified_expr = sqe;
			}
		}
		
		
		return st;
	}
	
	public boolean have_index_access(Check_status cs){
		if(is_result){
			return false;
		}else if(old_expression!=null){
			return old_expression.spec_expression.expression.have_index_access(cs);
		}else if(spec_quantified_expr!=null){
			return spec_quantified_expr.guard.have_index_access(cs) || spec_quantified_expr.body.have_index_access(cs);
		}
		
		return false;
		
	}
	
}
