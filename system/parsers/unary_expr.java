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

public class unary_expr  implements Parser<String>{
	String op;
	unary_expr unary_expr;
	postfix_expr postfix_expr;
	
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		Source s_backup = s.clone();
		try{
			String op_st = new string("-").parse(s, ps);
			String st2 = op_st;
			st2 += new spaces().parse(s, ps);
			unary_expr ue = new unary_expr();
			st2 += ue.parse(s, ps);
			
			st = st + st2;
			op = op_st;
			unary_expr = ue;
		}catch (Exception e){
			s.revert(s_backup);
			
			postfix_expr pe = new postfix_expr();
			st += pe.parse(s, ps);
			postfix_expr = pe;
		}
		return st;
	}
	
	public Check_return check(Check_status cs) throws Exception{
		if(unary_expr != null && op == "-"){
			return new Check_return(cs.ctx.mkSub((IntExpr)cs.ctx.mkInt(0), (IntExpr)(unary_expr.check(cs).expr)), null, null);
		}else{
			return postfix_expr.check(cs);
		}
	}
	
	public boolean have_index_access(Check_status cs){
 		if(this.unary_expr!=null) return unary_expr.have_index_access(cs);
		return postfix_expr.have_index_access(cs);
	}
	
	public Check_return loop_assign(Pair<List<F_Assign>,BoolExpr>assigned_fields, Check_status cs) throws Exception{
		if(unary_expr != null && op == "-"){
			return new Check_return(cs.ctx.mkSub((IntExpr)cs.ctx.mkInt(0), (IntExpr)(unary_expr.loop_assign(assigned_fields, cs).expr)), null, null);
		}else{
			return postfix_expr.loop_assign(assigned_fields, cs);
		}
	}
	
}
