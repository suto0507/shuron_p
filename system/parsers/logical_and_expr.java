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
	
	public Check_return check(Check_status cs) throws Exception{
		if(this.equality_exprs.size()==0){
			return this.equality_expr.check(cs);
		}else{
			BoolExpr pre_pathcondition = cs.pathcondition;
			
			BoolExpr expr = (BoolExpr)equality_expr.check(cs).expr;
			cs.add_path_condition_tmp(expr);
			
			for(equality_expr ee : equality_exprs){
				expr = cs.ctx.mkAnd(expr,(BoolExpr)ee.check(cs).expr);
				cs.add_path_condition_tmp(expr);
			}
			
			cs.pathcondition = pre_pathcondition;
			
			return new Check_return(expr, null, null);
		}
	}
	
	public boolean have_index_access(Check_status cs){
		boolean have = equality_expr.have_index_access(cs);
		for(equality_expr ee : equality_exprs){
			have = have || ee.have_index_access(cs);
		}
		return have;
	}
	
	public Check_return loop_assign(Pair<List<F_Assign>,BoolExpr>assigned_fields, Check_status cs) throws Exception{
		if(this.equality_exprs.size()==0){
			return this.equality_expr.loop_assign(assigned_fields, cs);
		}else{
			BoolExpr pre_pathcondition = cs.pathcondition;
			
			BoolExpr expr = (BoolExpr)equality_expr.loop_assign(assigned_fields, cs).expr;
			
			for(equality_expr ee : equality_exprs){
				expr = cs.ctx.mkAnd(expr,(BoolExpr)ee.loop_assign(assigned_fields, cs).expr);
			}
			
			cs.pathcondition = pre_pathcondition;
			
			return new Check_return(expr, null, null);
		}
	}

}

