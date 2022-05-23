package system.parsers;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.BoolExpr;

import system.*;

public class spec_case_seq implements Parser<String>  {
	List<generic_spec_case> generic_spec_cases;
	public String parse(Source s,Parser_status ps)throws Exception{
		this.generic_spec_cases = new ArrayList<generic_spec_case>();
		
		String st = "";
		generic_spec_case gsc = new generic_spec_case();
		st = st + gsc.parse(s, ps);
		this.generic_spec_cases.add(gsc);
		
		Source s_backup = s.clone();
		try {
			while(true){
				String st2 = "";
				s_backup = s.clone();
				st2 = st2 + new spaces().parse(s, ps);
				st2 = st2 + new string("also").parse(s, ps);
				st2 = st2 + new spaces().parse(s, ps);
				gsc = new generic_spec_case();
				st2 = st2 + gsc.parse(s, ps);
				st = st + st2;
				this.generic_spec_cases.add(gsc);
			}
		}catch (Exception e){
			s.revert(s_backup);
		}	
		
		return st;
	}
	
	public BoolExpr requires_expr(Check_status cs) throws Exception{
		BoolExpr expr = null;
		for(generic_spec_case gsc : generic_spec_cases){
			List<requires_clause> rcs = gsc.get_requires();
			if(rcs == null){
				expr = cs.ctx.mkBool(true);
				break;
			}else{
				BoolExpr gsc_expr = null;
				for(requires_clause rc : rcs){
					if(gsc_expr == null){
						gsc_expr = (BoolExpr) rc.check(cs);
					}else{
						gsc_expr = cs.ctx.mkAnd(gsc_expr, (BoolExpr) rc.check(cs));
					}
				}
				
				if(expr == null){
					expr = gsc_expr;
				}else{
					expr = cs.ctx.mkOr(expr, gsc_expr);
				}
			}
		}
		
		if(expr == null) expr = cs.ctx.mkBool(true);
		
		return expr;
	}
	
	public BoolExpr ensures_expr(Check_status cs) throws Exception{
		BoolExpr expr = null;
		for(generic_spec_case gsc : generic_spec_cases){
			List<ensures_clause> ecs = gsc.get_ensures();
			if(ecs != null){
				BoolExpr pre_expr = null;
				BoolExpr post_expr = null;
				
				//éñëOèåè
				List<requires_clause> rcs = gsc.get_requires();
				if(rcs == null){
					pre_expr = cs.ctx.mkBool(true);
				}else{
					for(requires_clause rc : rcs){
						if(pre_expr == null){
							pre_expr = (BoolExpr) rc.check(cs);
						}else{
							pre_expr = cs.ctx.mkAnd(pre_expr, (BoolExpr) rc.check(cs));
						}
					}
				}
				
				//éñå„èåè
				for(ensures_clause ec : ecs){
					if(post_expr == null){
						post_expr = (BoolExpr) ec.check(cs);
					}else{
						post_expr = cs.ctx.mkAnd(post_expr, (BoolExpr) ec.check(cs));
					}
				}
				
				if(expr == null){
					expr = cs.ctx.mkImplies(pre_expr, post_expr);
				}else{
					expr = cs.ctx.mkAnd(expr, cs.ctx.mkImplies(pre_expr, post_expr));
				}
			}
		}
		
		if(expr == null) expr = cs.ctx.mkBool(true);
		
		return expr;
	}
	
}

