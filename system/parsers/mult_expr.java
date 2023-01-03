package system.parsers;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;

import system.Check_return;
import system.Check_status;
import system.F_Assign;
import system.Field;
import system.Pair;
import system.Parser;
import system.Parser_status;
import system.Source;

public class mult_expr implements Parser<String>{
	unary_expr unary_expr1;
	List<unary_expr> unary_exprs;
	List<String> ops;
	public String parse(Source s,Parser_status ps)throws Exception{
		this.unary_exprs = new ArrayList<unary_expr>();
		this.ops = new ArrayList<String>();
		String st = "";
		this.unary_expr1 = new unary_expr();
		st = st + this.unary_expr1.parse(s,ps);
		Source s_backup = s.clone();
		try{
			while(true){
				s_backup = s.clone();
				String st2 = new spaces().parse(s, ps);
				String op = new mult_op().parse(s, ps);
				st2 = st2 + op;
				st2 = st2 + new spaces().parse(s, ps);
				unary_expr ue = new unary_expr();
				st2 = st2 + ue.parse(s, ps);
				st = st + st2;
				this.ops.add(op);
				this.unary_exprs.add(ue);
			}
		}catch (Exception e){
			s.revert(s_backup);
		}
		
		return st;
	}
	
	public Check_return check(Check_status cs) throws Exception{
		if(this.unary_exprs.size() == 0) return this.unary_expr1.check(cs);
			
		Expr expr = this.unary_expr1.check(cs).expr;
		for(int i = 0; i<this.unary_exprs.size(); i++){
			unary_expr ue = unary_exprs.get(i);
			String op = this.ops.get(i);
			if(op.equals("*")){
				expr = (IntExpr)cs.ctx.mkMul((IntExpr)expr,(IntExpr)ue.check(cs).expr);

			}else if(op.equals("/")){
				expr = (IntExpr)cs.ctx.mkDiv((IntExpr)expr,(IntExpr)ue.check(cs).expr);
			}else if(op.equals("%")){
				expr = (IntExpr)cs.ctx.mkITE(cs.ctx.mkGt((IntExpr)expr, cs.ctx.mkInt(0))
						, cs.ctx.mkSub(
								(IntExpr)expr
								,cs.ctx.mkMul((IntExpr)ue.check(cs).expr,cs.ctx.mkDiv((IntExpr)expr, (IntExpr)ue.check(cs).expr)))
						, cs.ctx.mkSub(
								(IntExpr)expr
								,cs.ctx.mkMul((IntExpr)ue.check(cs).expr,cs.ctx.mkDiv(cs.ctx.mkUnaryMinus((IntExpr)expr), cs.ctx.mkUnaryMinus((IntExpr)ue.check(cs).expr))))
						);
			}
		}
		return new Check_return(expr, null, null, null, "int", 0);

	}
	
	public boolean have_index_access(Check_status cs){
		boolean have = unary_expr1.have_index_access(cs);
		for(unary_expr ue : unary_exprs){
			have = have || ue.have_index_access(cs);
		}
		return have;
	}
	
	public Check_return loop_assign(Pair<List<F_Assign>,BoolExpr>assigned_fields, Check_status cs) throws Exception{
		if(this.unary_exprs.size() == 0) return this.unary_expr1.loop_assign(assigned_fields, cs);
			
		Expr expr = this.unary_expr1.loop_assign(assigned_fields, cs).expr;
		for(int i = 0; i<this.unary_exprs.size(); i++){
			unary_expr ue = unary_exprs.get(i);
			String op = this.ops.get(i);
			if(op.equals("*")){
				expr = (IntExpr)cs.ctx.mkMul((IntExpr)expr,(IntExpr)ue.loop_assign(assigned_fields, cs).expr);

			}else if(op.equals("/")){
				expr = (IntExpr)cs.ctx.mkDiv((IntExpr)expr,(IntExpr)ue.loop_assign(assigned_fields, cs).expr);
			}else if(op.equals("%")){
				expr = (IntExpr)cs.ctx.mkITE(cs.ctx.mkGt((IntExpr)expr, cs.ctx.mkInt(0))
						, cs.ctx.mkSub(
								(IntExpr)expr
								,cs.ctx.mkMul((IntExpr)ue.loop_assign(assigned_fields, cs).expr,cs.ctx.mkDiv((IntExpr)expr, (IntExpr)ue.loop_assign(assigned_fields, cs).expr)))
						, cs.ctx.mkSub(
								(IntExpr)expr
								,cs.ctx.mkMul((IntExpr)ue.loop_assign(assigned_fields, cs).expr,cs.ctx.mkDiv(cs.ctx.mkUnaryMinus((IntExpr)expr), cs.ctx.mkUnaryMinus((IntExpr)ue.loop_assign(assigned_fields, cs).expr))))
						);
			}
		}
		return new Check_return(expr, null, null, null, "int", 0);

	}
	
}

