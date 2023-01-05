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
				
				//事前条件
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
				
				//事後条件
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
	
	public Pair<List<F_Assign>, BoolExpr> assignables(Check_status cs) throws Exception{
		
		List<Field> fields = new ArrayList<Field>();
		List<Pair<BoolExpr, List<Pair<Field, List<Pair<Expr, List<IntExpr>>>>>>> cnst_fields = new ArrayList<Pair<BoolExpr, List<Pair<Field, List<Pair<Expr, List<IntExpr>>>>>>>();
		List<BoolExpr> all_assign_exprs = new ArrayList<BoolExpr>(); //何でも代入していい事前条件
		
		List<F_Assign> f_assigns = new ArrayList<F_Assign>();
		
		for(generic_spec_case gsc : generic_spec_cases){
			
			String pre_class_type_name = cs.instance_class_name;
			cs.instance_class_name = gsc.class_type_name;
			
			List<assignable_clause> acs = gsc.get_assignable();
			
			
			//事前条件
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
				
				List<Pair<Field, List<Pair<Expr, List<IntExpr>>>>> assignable_clause_field_arrays = new ArrayList<Pair<Field, List<Pair<Expr, List<IntExpr>>>>>();
				
				for(assignable_clause ac : acs){
					for(store_ref_expression sre : ac.store_ref_list.store_ref_expressions){
						Check_return cr = sre.check(cs);
						
						boolean add = false;
						for(Pair<Field, List<Pair<Expr, List<IntExpr>>>> assignable_clause_field_array : assignable_clause_field_arrays){			
							if(assignable_clause_field_array.fst.equals(cr.field)){
								assignable_clause_field_array.snd.add(new Pair<Expr, List<IntExpr>>(cr.class_expr, cr.indexs));
								add = true;
								break;
							}
						}
						if(!add){
							List<Pair<Expr, List<IntExpr>>> l = new ArrayList<Pair<Expr, List<IntExpr>>>();
							l.add(new Pair<Expr, List<IntExpr>>(cr.class_expr, cr.indexs));
							assignable_clause_field_arrays.add(new Pair<Field, List<Pair<Expr, List<IntExpr>>>>(cr.field, l));
						}
						
						if(!fields.contains(cr.field)){
							fields.add(cr.field);
						}
					}
				}
				
				
				cnst_fields.add(new Pair<BoolExpr, List<Pair<Field, List<Pair<Expr, List<IntExpr>>>>>>(pre_expr, assignable_clause_field_arrays));
				
			}else{//何でも代入していい事前条件
				all_assign_exprs.add(pre_expr);
			}
			
			
			cs.instance_class_name = pre_class_type_name;
		}
		
		
		
		for(Field f : fields){
			
			List<Pair<BoolExpr,List<Pair<Expr, List<IntExpr>>>>> has_f_arrays = new ArrayList<Pair<BoolExpr,List<Pair<Expr, List<IntExpr>>>>>();
			BoolExpr has_not_f_array = null;//そのフィールドの任意の配列への代入を行わない条件
			
			for(Pair<BoolExpr, List<Pair<Field, List<Pair<Expr, List<IntExpr>>>>>> cnst_field : cnst_fields){
				//配列の要素への代入
				boolean assign_field = false;
				for(Pair<Field, List<Pair<Expr, List<IntExpr>>>> field_index: cnst_field.snd){
					if(field_index.fst.equals(f)){
						has_f_arrays.add(new Pair<BoolExpr,List<Pair<Expr, List<IntExpr>>>>(cnst_field.fst, field_index.snd));
						assign_field = true;
					}
				}
				if(!assign_field){
					if(has_not_f_array == null){
						has_not_f_array = cnst_field.fst;
					}else{
						has_not_f_array = cs.ctx.mkOr(has_not_f_array, cnst_field.fst);
					}
				}
			}
			
			if(has_not_f_array == null) has_not_f_array = cs.ctx.mkBool(false);//そのフィールドの任意の配列への代入を行わない条件
			
			for(Pair<BoolExpr,List<Pair<Expr, List<IntExpr>>>> field_index: has_f_arrays){
				field_index.fst = cs.ctx.mkAnd(field_index.fst, cs.ctx.mkNot(has_not_f_array));
			}
			
			f_assigns.add(new F_Assign(f, has_f_arrays));
		}
		
		//何でも代入していい条件を作る
		BoolExpr all_assign_expr = null;
		for(BoolExpr aae : all_assign_exprs){
			if(all_assign_expr == null){
				all_assign_expr = aae;
			}else{
			all_assign_expr = cs.ctx.mkOr(all_assign_expr, aae);
			}
		}
		if(all_assign_expr == null){
			all_assign_expr = cs.ctx.mkBool(false);
		}else{//false && hoge をしてもしょうがないので
			for(Pair<BoolExpr, List<Pair<Field, List<Pair<Expr, List<IntExpr>>>>>> cnst_field : cnst_fields){
				all_assign_expr = cs.ctx.mkAnd(all_assign_expr, cs.ctx.mkNot(cnst_field.fst));
			}
		}
		
		return new Pair<List<F_Assign>, BoolExpr>(f_assigns, all_assign_expr);
	}

	
}

