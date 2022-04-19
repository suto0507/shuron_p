package system.parsers;

import java.util.ArrayList;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;

import system.Check_status;
import system.Parser;
import system.Parser_status;
import system.Source;

public class logical_and_expr implements Parser<String>{
	equality_expr equality_expr;
	//óvëfêîÇ™1à»è„Ç»ÇÁå„ÇÎÇ™ä‹Ç‹ÇÍÇÈ
	ArrayList<equality_expr> equality_exprs;
	public String parse(Source s,Parser_status ps)throws Exception{
		this.equality_exprs = new ArrayList<equality_expr>();
		String st = "";
		this.equality_expr = new equality_expr();
		st = st + this.equality_expr.parse(s,ps);
		Source s_backup = s.clone();
		try{
			while(true){
				s_backup = s.clone();
				String st2 = new spaces().parse(s, ps);
				st2 = st2 + new string("&&").parse(s, ps);
				st2 = st2 + new spaces().parse(s, ps);
				equality_expr ee = new equality_expr();
				st2 = st2 + ee.parse(s, ps);
				this.equality_exprs.add(ee);
				st = st + st2;
			}
		}catch (Exception e){
			s.revert(s_backup);
		}
		
		return st;
	}
	
	public Expr check(Check_status cs) throws Exception{
		if(this.equality_exprs.size()==0){
			return this.equality_expr.check(cs);
		}else{
			BoolExpr expr = (BoolExpr)equality_expr.check(cs);
			for(equality_expr ee : equality_exprs){
				expr = cs.ctx.mkAnd(expr,(BoolExpr)ee.check(cs));
			}
			return expr;
		}
	}
	


}

