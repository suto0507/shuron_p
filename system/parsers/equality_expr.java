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

public class equality_expr implements Parser<String>{
	relational_expr relational_expr1, relational_expr2;
	String op;
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		this.relational_expr1 = new relational_expr();
		st = st + this.relational_expr1.parse(s,ps);
		Source s_backup = s.clone();
		try{
			try{
				String st2 = new spaces().parse(s, ps);
				this.op = new string("==").parse(s, ps);
				st2 = st2 + this.op;
				st2 = st2 + new spaces().parse(s, ps);
				relational_expr re = new relational_expr();
				st2 = st2 + re.parse(s, ps);
				this.relational_expr2 = re;
				st = st + st2;
			}catch (Exception e){
				s.revert(s_backup);
				String st2 = new spaces().parse(s, ps);
				this.op = new string("!=").parse(s, ps);
				st2 = st2 + this.op;
				st2 = st2 + new spaces().parse(s, ps);
				relational_expr re = new relational_expr();
				st2 = st2 + re.parse(s, ps);
				this.relational_expr2 = re;
				st = st + st2;
			}
		}catch (Exception e){
			s.revert(s_backup);
		}
		
		return st;
	}
	
	public Check_return check(Check_status cs) throws Exception{
		if(this.relational_expr2==null){
			return this.relational_expr1.check(cs);
		}else{
			BoolExpr expr = null;
			if(this.op.equals("==")){
				expr = cs.ctx.mkEq(this.relational_expr1.check(cs).expr,this.relational_expr2.check(cs).expr);
			}else if(this.op.equals("!=")){
				expr = cs.ctx.mkDistinct(this.relational_expr1.check(cs).expr,this.relational_expr2.check(cs).expr);
			}
			return new Check_return(expr, null, null);
		}
	}
	
	public boolean have_index_access(Check_status cs){
 		if(this.relational_expr2!=null) return relational_expr1.have_index_access(cs) || relational_expr2.have_index_access(cs);
		return relational_expr1.have_index_access(cs);
	}
	
	public Check_return loop_assign(Pair<List<F_Assign>,BoolExpr>assigned_fields, Check_status cs) throws Exception{
		if(this.relational_expr2==null){
			return this.relational_expr1.loop_assign(assigned_fields, cs);
		}else{
			BoolExpr expr = null;
			if(this.op.equals("==")){
				expr = cs.ctx.mkEq(this.relational_expr1.loop_assign(assigned_fields, cs).expr,this.relational_expr2.loop_assign(assigned_fields, cs).expr);
			}else if(this.op.equals("!=")){
				expr = cs.ctx.mkDistinct(this.relational_expr1.loop_assign(assigned_fields, cs).expr,this.relational_expr2.loop_assign(assigned_fields, cs).expr);
			}
			return new Check_return(expr, null, null);
		}
	}

	
}

