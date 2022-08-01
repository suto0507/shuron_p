package system.parsers;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;

import system.Check_status;
import system.Parser;
import system.Parser_status;
import system.Source;

public class implies_expr implements Parser<String>{
	logical_or_expr logical_or_expr;
	implies_expr implies_expr;
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		this.logical_or_expr = new logical_or_expr();
		st = st + this.logical_or_expr.parse(s,ps);
		Source s_backup = s.clone();
		try{
			String st2 = new spaces().parse(s, ps);
			st2 = st2 + new string("==>").parse(s, ps);
			if(ps.in_jml == false){
				throw new Exception("not in jml");
			}
			st2 = st2 + new spaces().parse(s, ps);
			implies_expr ie = new implies_expr();
			st2 = st2 + ie.parse(s, ps);
			this.implies_expr = ie;
			st = st + st2;
		}catch (Exception e){
			s.revert(s_backup);
		}
		
		return st;
	}
	
	public Expr check(Check_status cs) throws Exception{
		if(this.implies_expr==null){
			return this.logical_or_expr.check(cs);
		}else{
			BoolExpr pre_pathcondition = cs.pathcondition;
			
			BoolExpr guard = (BoolExpr)this.logical_or_expr.check(cs);
			cs.add_path_condition(guard);
			
			Expr expr = cs.ctx.mkImplies(guard,(BoolExpr)this.implies_expr.check(cs));
			
			cs.pathcondition = pre_pathcondition;
			
			return expr;
		}
	}
	

}
