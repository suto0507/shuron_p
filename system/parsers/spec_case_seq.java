package system.parsers;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.*;

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
				st2 = st2 + new newLines().parse(s, ps);
				st2 = st2 + new spaces().parse(s,ps);
				st2 = st2 + new string("also").parse(s, ps);
				st2 = st2 + new newLines().parse(s, ps);
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
			if(rcs == null || rcs.size()==0){
				expr = cs.ctx.mkBool(true);
				break;
			}else{
				BoolExpr pre_pathcondition = cs.pathcondition;
				
				BoolExpr gsc_expr = null;
				for(requires_clause rc : rcs){
					String pre_class_type_name = cs.instance_class_name;
					cs.instance_class_name = gsc.class_type_name;
					
					rc.set_expr((BoolExpr) rc.check(cs), cs);
					
					cs.instance_class_name = pre_class_type_name;
					
					BoolExpr rc_expr = rc.get_expr(cs);
					if(gsc_expr == null){
						gsc_expr = rc_expr;
					}else{
						gsc_expr = cs.ctx.mkAnd(gsc_expr, rc_expr);
					}
					cs.add_path_condition_tmp(rc_expr);
				}
				
				if(expr == null){
					expr = gsc_expr;
				}else{
					expr = cs.ctx.mkOr(expr, gsc_expr);
				}
				
				cs.pathcondition = pre_pathcondition;
			}
			
			
			
		}
		
		if(expr == null) expr = cs.ctx.mkBool(true);
		
		return expr;
	}
	
	public BoolExpr ensures_expr(Check_status cs) throws Exception{
		BoolExpr expr = null;
		for(generic_spec_case gsc : generic_spec_cases){
			
			String pre_class_type_name = cs.instance_class_name;
			cs.instance_class_name = gsc.class_type_name;
			
			List<ensures_clause> ecs = gsc.get_ensures();
			if(ecs != null && ecs.size() > 0){
				BoolExpr pre_expr = null;
				BoolExpr post_expr = null;
				
				BoolExpr pre_pathcondition = cs.pathcondition;
				
				//éñëOèåè
				List<requires_clause> rcs = gsc.get_requires();
				if(rcs == null || rcs.size()==0){
					pre_expr = cs.ctx.mkBool(true);
				}else{
					for(requires_clause rc : rcs){
						BoolExpr rc_expr = rc.get_expr(cs);
						if(pre_expr == null){
							pre_expr = rc_expr;
						}else{
							pre_expr = cs.ctx.mkAnd(pre_expr, rc_expr);
						}
						cs.add_path_condition_tmp(rc_expr);
					}
				}
				
				//éñå„èåè
				if(ecs == null || ecs.size()==0){
					post_expr = cs.ctx.mkBool(true);
				}else{
					for(ensures_clause ec : ecs){
						if(post_expr == null){
							post_expr = (BoolExpr) ec.check(cs);
						}else{
							post_expr = cs.ctx.mkAnd(post_expr, (BoolExpr) ec.check(cs));
						}
					}
				}
				
				cs.pathcondition = pre_pathcondition;
				
				if(expr == null){
					expr = cs.ctx.mkImplies(pre_expr, post_expr);
				}else{
					expr = cs.ctx.mkAnd(expr, cs.ctx.mkImplies(pre_expr, post_expr));
				}
			}
			
			
			cs.instance_class_name = pre_class_type_name;
		}
		
		if(expr == null) expr = cs.ctx.mkBool(true);
		
		return expr;
	}
	
	
	public Method_Assign assignables(Check_status cs) throws Exception{
		
		BoolExpr can_all_assing = null;
		BoolExpr cannot_all_assing = null;
		
		ArrayList<Case_Assign> case_assigns = new ArrayList<Case_Assign>();
		
		for(generic_spec_case gsc : generic_spec_cases){
			
			String pre_class_type_name = cs.instance_class_name;
			cs.instance_class_name = gsc.class_type_name;
			
			List<assignable_clause> acs = gsc.get_assignable();
			
			
			//éñëOèåè
			BoolExpr pre_expr = null;
			
			List<requires_clause> rcs = gsc.get_requires();
			if(rcs == null  || rcs.size()==0){
				pre_expr = cs.ctx.mkBool(true);
			}else{
				for(requires_clause rc : rcs){
					if(pre_expr == null){
						pre_expr = rc.get_expr(cs);
					}else{
						pre_expr = cs.ctx.mkAnd(pre_expr, rc.get_expr(cs));
					}
				}
			}
			
			
			if(acs != null){
				//assignable
				
				ArrayList<Check_return> crs = new ArrayList<Check_return>();
				
				for(assignable_clause ac : acs){
					for(store_ref_expression sre : ac.store_ref_list.store_ref_expressions){
						Check_return cr = sre.check(cs);
						
						crs.add(cr);
					}
				}
				
				if(cannot_all_assing==null){
					cannot_all_assing = pre_expr;
				}else{
					cannot_all_assing = cs.ctx.mkOr(cannot_all_assing, pre_expr);
				}
				
				case_assigns.add(new Case_Assign(pre_expr, crs));
				
			}else{//âΩÇ≈Ç‡ë„ì¸ÇµÇƒÇ¢Ç¢éñëOèåè
				if(can_all_assing==null){
					can_all_assing = pre_expr;
				}else{
					can_all_assing = cs.ctx.mkOr(can_all_assing, pre_expr);
				}
			}
			
			cs.instance_class_name = pre_class_type_name;
		}
		if(cannot_all_assing==null){
			cannot_all_assing = cs.ctx.mkFalse();
		}
		if(can_all_assing==null){
			can_all_assing = cs.ctx.mkFalse();
		}
		return new Method_Assign(cs.ctx.mkAnd(can_all_assing, cs.ctx.mkNot(cannot_all_assing)), case_assigns);
	}

	
}

