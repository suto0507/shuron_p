package system.parsers;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;

import system.Check_return;
import system.Check_status;
import system.Parser;
import system.Parser_status;
import system.Source;
public class additive_expr implements Parser<String>{
	mult_expr mult_expr1;
	List<mult_expr> mult_exprs;
	List<String> ops;
	public String parse(Source s,Parser_status ps)throws Exception{
		this.mult_exprs = new ArrayList<mult_expr>();
		this.ops = new ArrayList<String>();
		String st = "";
		this.mult_expr1 = new mult_expr();
		st = st + this.mult_expr1.parse(s,ps);
		Source s_backup = s.clone();
		try{
			while(true){
				s_backup = s.clone();
				String st2 = new spaces().parse(s, ps);
				String op = new additive_op().parse(s, ps);
				st2 = st2 + op;
				st2 = st2 + new spaces().parse(s, ps);
				mult_expr me = new mult_expr();
				st2 = st2 + me.parse(s, ps);
				st = st + st2;
				this.ops.add(op);
				this.mult_exprs.add(me);
			}
		}catch (Exception e){
			s.revert(s_backup);
		}
		
		return st;
	}
	
	public Check_return check(Check_status cs) throws Exception{
		if(this.mult_exprs.size() == 0) return this.mult_expr1.check(cs);
		
		Expr expr =  this.mult_expr1.check(cs).expr;
		for(int i = 0; i < this.mult_exprs.size(); i++){
			mult_expr me = this.mult_exprs.get(i);
			String op = this.ops.get(i);
			if(op.equals("+")){
				expr = (IntExpr)cs.ctx.mkAdd((IntExpr)expr, (IntExpr)me.check(cs).expr);
			}else if(op.equals("-")){
				expr = (IntExpr)cs.ctx.mkSub((IntExpr)expr, (IntExpr)me.check(cs).expr);
			}
		}
		return new Check_return(expr, null, null);
	}

}

