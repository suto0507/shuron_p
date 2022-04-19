package system.parsers;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;

import system.Check_status;
import system.Parser;
import system.Parser_status;
import system.Source;

public class mult_expr implements Parser<String>{
	postfix_expr postfix_expr1;
	List<postfix_expr> postfix_exprs;
	List<String> ops;
	public String parse(Source s,Parser_status ps)throws Exception{
		this.postfix_exprs = new ArrayList<postfix_expr>();
		this.ops = new ArrayList<String>();
		String st = "";
		this.postfix_expr1 = new postfix_expr();
		st = st + this.postfix_expr1.parse(s,ps);
		Source s_backup = s.clone();
		try{
			while(true){
				s_backup = s.clone();
				String st2 = new spaces().parse(s, ps);
				String op = new mult_op().parse(s, ps);
				st2 = st2 + op;
				st2 = st2 + new spaces().parse(s, ps);
				postfix_expr pe = new postfix_expr();
				st2 = st2 + pe.parse(s, ps);
				st = st + st2;
				this.ops.add(op);
				this.postfix_exprs.add(pe);
			}
		}catch (Exception e){
			s.revert(s_backup);
		}
		
		return st;
	}
	
	public Expr check(Check_status cs) throws Exception{
		Expr expr = this.postfix_expr1.check(cs);
		for(int i = 0; i<this.postfix_exprs.size(); i++){
			postfix_expr pe = postfix_exprs.get(i);
			String op = this.ops.get(i);
			if(op.equals("*")){
				expr = (IntExpr)cs.ctx.mkMul((IntExpr)expr,(IntExpr)pe.check(cs));

			}else if(op.equals("/")){
				expr = (IntExpr)cs.ctx.mkDiv((IntExpr)expr,(IntExpr)pe.check(cs));
			}else if(op.equals("%")){
				expr = (IntExpr)cs.ctx.mkITE(cs.ctx.mkGt((IntExpr)expr, cs.ctx.mkInt(0))
						, cs.ctx.mkSub(
								(IntExpr)expr
								,cs.ctx.mkMul((IntExpr)pe.check(cs),cs.ctx.mkDiv((IntExpr)expr, (IntExpr)pe.check(cs))))
						, cs.ctx.mkSub(
								(IntExpr)expr
								,cs.ctx.mkMul((IntExpr)pe.check(cs),cs.ctx.mkDiv(cs.ctx.mkUnaryMinus((IntExpr)expr), cs.ctx.mkUnaryMinus((IntExpr)pe.check(cs)))))
						);
			}
		}
		return expr;

	}
	
	
	
}

