package system.parsers;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;

import system.Check_return;
import system.Check_status;
import system.Field;
import system.Pair;
import system.Parser;
import system.Parser_status;
import system.Source;

public class logical_or_expr implements Parser<String>{
	logical_and_expr logical_and_expr; 
	//óvëfêîÇ™1à»è„Ç»ÇÁå„ÇÎÇ™ä‹Ç‹ÇÍÇÈ
	List<logical_and_expr> logical_and_exprs;
	public String parse(Source s,Parser_status ps)throws Exception{
		this.logical_and_exprs = new ArrayList<logical_and_expr>();
		String st = "";
		this.logical_and_expr = new logical_and_expr();
		st = st + this.logical_and_expr.parse(s,ps);
		Source s_backup = s.clone();
		try{
			while(true){
				s_backup = s.clone();
				String st2 = new spaces().parse(s, ps);
				st2 = st2 + new string("||").parse(s, ps);
				st2 = st2 + new spaces().parse(s, ps);
				logical_and_expr lae = new logical_and_expr();
				st2 = st2 + lae.parse(s, ps);
				this.logical_and_exprs.add(lae);
				st = st + st2;
			}
		}catch (Exception e){
			s.revert(s_backup);
		}
		
		return st;
	}
	
	public Check_return check(Check_status cs) throws Exception{
		if(this.logical_and_exprs.size()==0){
			return this.logical_and_expr.check(cs);
		}else{
			BoolExpr expr = (BoolExpr)logical_and_expr.check(cs).expr;
			for(logical_and_expr lae : logical_and_exprs){
				expr = cs.ctx.mkOr(expr,(BoolExpr)lae.check(cs).expr);
			}
			return new Check_return(expr, null, null);
		}
	}
	
	public boolean have_index_access(Check_status cs){
		boolean have = logical_and_expr.have_index_access(cs);
		for(logical_and_expr lae : logical_and_exprs){
			have = have || lae.have_index_access(cs);
		}
		return have;
	}
	
	public Check_return loop_assign(Pair<List<Pair<Field,List<List<IntExpr>>>>,Boolean>assigned_fields, Check_status cs) throws Exception{
		if(this.logical_and_exprs.size()==0){
			return this.logical_and_expr.loop_assign(assigned_fields, cs);
		}else{
			BoolExpr expr = (BoolExpr)logical_and_expr.loop_assign(assigned_fields, cs).expr;
			for(logical_and_expr lae : logical_and_exprs){
				expr = cs.ctx.mkOr(expr,(BoolExpr)lae.loop_assign(assigned_fields, cs).expr);
			}
			return new Check_return(expr, null, null);
		}
	}
	
}

