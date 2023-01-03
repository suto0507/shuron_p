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

public class relational_expr implements Parser<String>{
	additive_expr additive_expr1, additive_expr2;
	String op;
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		this.additive_expr1 = new additive_expr();
		st = st + this.additive_expr1.parse(s,ps);
		Source s_backup = s.clone();
		try{
			Source s_backup2 = s.clone();
			try{
				String st2 = new spaces().parse(s, ps);
				String op = new string("<=").parse(s, ps);
				st2 = st2 + op;
				st2 = st2 + new spaces().parse(s, ps);
				additive_expr ae = new additive_expr();
				st2 = st2 + ae.parse(s, ps);
				this.additive_expr2 = ae;
				this.op = op;
				st = st + st2;
			}catch (Exception e){
				s.revert(s_backup2);
				s_backup2 = s.clone();
				try{
					String st2 = new spaces().parse(s, ps);
					String op = new string(">=").parse(s, ps);
					st2 = st2 + op;
					st2 = st2 + new spaces().parse(s, ps);
					additive_expr ae = new additive_expr();
					st2 = st2 + ae.parse(s, ps);
					this.additive_expr2 = ae;
					this.op = op;
					st = st + st2;
				}catch (Exception e2){
					s.revert(s_backup2);
					s_backup2 = s.clone();
					try{
						String st2 = new spaces().parse(s, ps);
						String op = new string("<").parse(s, ps);
						st2 = st2 + op;
						st2 = st2 + new spaces().parse(s, ps);
						additive_expr ae = new additive_expr();
						st2 = st2 + ae.parse(s, ps);
						this.additive_expr2 = ae;
						this.op = op;
						st = st + st2;
					}catch (Exception e3){
						s.revert(s_backup2);
						s_backup2 = s.clone();
						String st2 = new spaces().parse(s, ps);
						String op = new string(">").parse(s, ps);
						st2 = st2 + op;
						st2 = st2 + new spaces().parse(s, ps);
						additive_expr ae = new additive_expr();
						st2 = st2 + ae.parse(s, ps);
						this.additive_expr2 = ae;
						this.op = op;
						st = st + st2;
					}
				}
			}
			
			
		}catch (Exception e){
			s.revert(s_backup);
		}
		
		return st;
	}
	
	public Check_return check(Check_status cs) throws Exception{
		if(this.additive_expr2==null){
			return this.additive_expr1.check(cs);
		}else{
			BoolExpr expr = null;
			if(this.op.equals("<=")){
				expr = cs.ctx.mkLe((IntExpr)this.additive_expr1.check(cs).expr,(IntExpr)this.additive_expr2.check(cs).expr);
			}else if(this.op.equals(">=")){
				expr = cs.ctx.mkGe((IntExpr)this.additive_expr1.check(cs).expr,(IntExpr)this.additive_expr2.check(cs).expr);
			}else if(this.op.equals("<")){
				expr = cs.ctx.mkLt((IntExpr)this.additive_expr1.check(cs).expr,(IntExpr)this.additive_expr2.check(cs).expr);
			}else if(this.op.equals(">")){
				expr = cs.ctx.mkGt((IntExpr)this.additive_expr1.check(cs).expr,(IntExpr)this.additive_expr2.check(cs).expr);
			}
			return new Check_return(expr, null, null, null, "boolean", 0);
		}
	}
	
	public boolean have_index_access(Check_status cs){
 		if(this.additive_expr2!=null) return additive_expr1.have_index_access(cs) || additive_expr2.have_index_access(cs);
		return additive_expr1.have_index_access(cs);
	}
	
	public Check_return loop_assign(Pair<List<F_Assign>,BoolExpr>assigned_fields, Check_status cs) throws Exception{
		if(this.additive_expr2==null){
			return this.additive_expr1.loop_assign(assigned_fields, cs);
		}else{
			BoolExpr expr = null;
			if(this.op.equals("<=")){
				expr = cs.ctx.mkLe((IntExpr)this.additive_expr1.loop_assign(assigned_fields, cs).expr,(IntExpr)this.additive_expr2.loop_assign(assigned_fields, cs).expr);
			}else if(this.op.equals(">=")){
				expr = cs.ctx.mkGe((IntExpr)this.additive_expr1.loop_assign(assigned_fields, cs).expr,(IntExpr)this.additive_expr2.loop_assign(assigned_fields, cs).expr);
			}else if(this.op.equals("<")){
				expr = cs.ctx.mkLt((IntExpr)this.additive_expr1.loop_assign(assigned_fields, cs).expr,(IntExpr)this.additive_expr2.loop_assign(assigned_fields, cs).expr);
			}else if(this.op.equals(">")){
				expr = cs.ctx.mkGt((IntExpr)this.additive_expr1.loop_assign(assigned_fields, cs).expr,(IntExpr)this.additive_expr2.loop_assign(assigned_fields, cs).expr);
			}
			return new Check_return(expr, null, null, null, "boolean", 0);
		}
	}
	
}

