package system.parsers;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.Status;

import system.Check_return;
import system.Check_status;
import system.F_Assign;
import system.Field;
import system.Helper_assigned_field;
import system.Pair;
import system.Parser;
import system.Parser_status;
import system.Source;
import system.Variable;

public class assignment_expr implements Parser<String>{
	implies_expr implies_expr;
	postfix_expr postfix_expr;
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		Source s_backup = s.clone();
		try{
			String st2 = "";
			postfix_expr pe = new postfix_expr();
			st2 = st2 + pe.parse(s, ps);
			st2 = st2 + new spaces().parse(s, ps);
			st2 = st2 + new assignment_op().parse(s, ps);
			st2 = st2 + new spaces().parse(s, ps);
			implies_expr ie = new implies_expr();
			st2 = st2 + ie.parse(s, ps);
			this.postfix_expr = pe;
			this.implies_expr = ie;
			st = st + st2;
		}catch (Exception e){
			s.revert(s_backup);
			this.implies_expr = new implies_expr();
			st = st + this.implies_expr.parse(s, ps);
		}
		
		
		return st;
	}
	
	public Check_return check(Check_status cs) throws Exception{
		Check_return assign_cr = null;
		
		if(this.postfix_expr != null){
			assign_cr = this.postfix_expr.check_assign(cs);
			//代入していいかどうかの検証
			if(!(assign_cr.field instanceof Variable)){//フィールドかどうか？
				//assignしていいか
				
				BoolExpr ex;
				
				
				ex = assign_cr.field.assign_index_expr(assign_cr.class_expr, assign_cr.indexs, cs);
				
				
				//何でも代入していい
				ex = cs.ctx.mkOr(ex, cs.assinable_cnst_all);
				
				//コンストラクタ
				if(!(cs.in_constructor&&cs.this_field.equals(assign_cr.class_expr))){
					System.out.println("check assign");
					cs.assert_constraint(ex);
				}
				
				//finalかどうか
				if(assign_cr.field.modifiers!=null && assign_cr.field.modifiers.is_final){
					if(cs.in_constructor&&cs.this_field.equals(assign_cr.class_expr)&&assign_cr.field.final_initialized==false){
						assign_cr.field.final_initialized = true;
					}else{
						throw new Exception("Cannot be assigned to " + assign_cr.field.field_name);
					}
				}
			}
			
			//左辺のExpr
			
			if(cs.in_helper){
				//helperメソッド、コンストラクタでは、配列を代入前に検証が必要な場合がある
				if(assign_cr.indexs.size() < assign_cr.field.dims){
					assign_cr.field.assert_all_array_assign_in_helper(0, 1, assign_cr.class_expr, cs.ctx.mkBool(true), new ArrayList<IntExpr>(), cs);
				}
			}
			
		}
		Check_return rc = this.implies_expr.check(cs);
		
		
		
		if(this.postfix_expr != null){
			//1次元以上の配列としてエイリアスした場合には、それ以降配列を代入する前に篩型の検証を行わなければならない
			if(assign_cr.field.hava_refinement_type() && assign_cr.field.have_index_access(cs) 
					&& rc.field != null && rc.field.hava_refinement_type() && rc.field.have_index_access(cs) 
					&& assign_cr.field.dims - assign_cr.indexs.size() >= 1){
				
				if(cs.in_helper){
					if(assign_cr.field instanceof Variable){
						assign_cr.field.alias_1d_in_helper = cs.ctx.mkOr(assign_cr.field.alias_1d_in_helper, cs.get_pathcondition());
					}
					if(rc.field instanceof Variable){
						rc.field.alias_1d_in_helper = cs.ctx.mkOr(rc.field.alias_1d_in_helper, cs.get_pathcondition());
					}
				}else if(cs.in_constructor){
					if(!(assign_cr.field instanceof Variable) && cs.this_field.get_Expr(cs).equals(assign_cr.class_expr)){
						assign_cr.field.alias_in_consutructor_or_2d_in_helper = cs.ctx.mkOr(assign_cr.field.alias_in_consutructor_or_2d_in_helper, cs.get_pathcondition());
					}
					if(!(rc.field instanceof Variable) && cs.this_field.get_Expr(cs).equals(rc.class_expr)){
						rc.field.alias_in_consutructor_or_2d_in_helper = cs.ctx.mkOr(rc.field.alias_in_consutructor_or_2d_in_helper, cs.get_pathcondition());
					}
				}
			}
			//2次元以上の配列としてエイリアスした場合には、それ以降篩型を満たさなければいけない
			if(assign_cr.field.hava_refinement_type() && assign_cr.field.have_index_access(cs) 
					&& rc.field != null && rc.field.hava_refinement_type() && rc.field.have_index_access(cs) 
					&& assign_cr.field.dims - assign_cr.indexs.size() >= 2){
				
				if(cs.in_helper){
					if(assign_cr.field instanceof Variable){
						assign_cr.field.alias_in_consutructor_or_2d_in_helper = cs.ctx.mkOr(assign_cr.field.alias_in_consutructor_or_2d_in_helper, cs.get_pathcondition());
					}
					if(rc.field instanceof Variable){
						rc.field.alias_in_consutructor_or_2d_in_helper = cs.ctx.mkOr(rc.field.alias_in_consutructor_or_2d_in_helper, cs.get_pathcondition());
					}
				}
			}
			
			
			//値を代入する
			assign_cr.field.assign_value(assign_cr.class_expr, assign_cr.indexs, rc.expr, cs);
			
			
			//refinement_type
			//篩型を持つ配列とエイリアスしたローカル配列は、要素の変更はできない
			if(assign_cr.field.dims>0 && !(assign_cr.field.hava_refinement_type() && assign_cr.field.have_index_access(cs))
					 && assign_cr.field.dims==assign_cr.indexs.size() && assign_cr.field instanceof Variable){
				Expr alias_refined;
				if(((Variable) assign_cr.field).alias_refined == null){
					alias_refined = cs.ctx.mkBool(false);
				}else{
					alias_refined = ((Variable) assign_cr.field).alias_refined;
				}
				cs.assert_constraint(cs.ctx.mkNot(alias_refined));
			}
			
			//配列の篩型が安全かどうか
			cs.check_array_alias(assign_cr.field, assign_cr.class_expr, assign_cr.indexs, rc.field, rc.class_expr, rc.indexs);
			

	
			//篩型の検証
			System.out.println("check refinement type");
			if(assign_cr.field.hava_refinement_type()){
				if(cs.in_helper){
					if(assign_cr.field.dims >= 2 && assign_cr.field.have_index_access(cs)){//2次元以上の配列としてエイリアスしている場合には、篩型の検証をしないといけない
						cs.solver.push();
						cs.add_constraint(assign_cr.field.alias_in_consutructor_or_2d_in_helper);

						assign_cr.field.assert_refinement(cs, assign_cr.class_expr);
						cs.solver.pop();
					}
					
					Helper_assigned_field assigned_field = new Helper_assigned_field(cs.get_pathcondition(), assign_cr.field, assign_cr.class_expr);
					cs.helper_assigned_fields.add(assigned_field);
				}else if(cs.in_constructor && cs.this_field.get_Expr(cs).equals(assign_cr.class_expr)){
					BoolExpr condition = cs.this_alias;//thisをどこかに渡した後には、篩型の検証をする必要がある
					if(assign_cr.field.dims >= 1 && assign_cr.field.have_index_access(cs)){//配列としてエイリアスしている場合には、篩型の検証をしないといけない
						condition = cs.ctx.mkOr(condition, assign_cr.field.alias_in_consutructor_or_2d_in_helper);
					}
					
					cs.solver.push();
					cs.add_constraint(assign_cr.field.alias_in_consutructor_or_2d_in_helper);

					assign_cr.field.assert_refinement(cs, assign_cr.class_expr);
					cs.solver.pop();
				}else{
					assign_cr.field.assert_refinement(cs, assign_cr.class_expr);
				}
			}
			//配列がエイリアスしたときに、右辺の配列の篩型の検証 　　初めてのエイリアスである可能性であるときだけ検証
			if(cs.in_helper){
				if(assign_cr.field.hava_refinement_type() && assign_cr.field.have_index_access(cs) 
						&& rc.field != null && rc.field.hava_refinement_type() && rc.field.have_index_access(cs) 
						&& assign_cr.field.dims - assign_cr.indexs.size() >= 2){
					rc.field.assert_refinement(cs, rc.class_expr);
				}
			}else if(cs.in_constructor && cs.this_field.get_Expr(cs).equals(rc.class_expr)){
				if(assign_cr.field.hava_refinement_type() && assign_cr.field.have_index_access(cs) 
						&& rc.field != null && rc.field.hava_refinement_type() && rc.field.have_index_access(cs) 
						&& assign_cr.field.dims - assign_cr.indexs.size() >= 1){
					rc.field.assert_refinement(cs, rc.class_expr);
				}
			}
			
			
			return new Check_return(assign_cr.field.get_Expr_with_indexs(assign_cr.class_expr, assign_cr.indexs, cs), assign_cr.field, assign_cr.indexs, assign_cr.class_expr);
		}else{
			return rc;
		}
	}
	
	public boolean have_index_access(Check_status cs){
 		if(this.postfix_expr!=null) return implies_expr.have_index_access(cs) || postfix_expr.have_index_access(cs);
		return implies_expr.have_index_access(cs);
	}
	
	public Check_return loop_assign(Pair<List<F_Assign>,BoolExpr>assigned_fields, Check_status cs) throws Exception{
		Check_return cr_l = null;
		if(this.postfix_expr!=null){
			cr_l = this.postfix_expr.loop_assign(assigned_fields, cs);
			
			boolean find_field = false;
			for(F_Assign f_i : assigned_fields.fst){
				if(f_i.field.equals(cr_l.field, cs) ){//見つかったら追加する
					find_field = true;
					ArrayList<Pair<Expr,List<IntExpr>>> indexs =  new ArrayList<Pair<Expr,List<IntExpr>>>();
					indexs.add(new Pair(cr_l.class_expr, cr_l.indexs));
					f_i.cnst_array.add(new Pair<BoolExpr,List<Pair<Expr,List<IntExpr>>>>(cs.get_pathcondition(), indexs));
					break;
				}
			}
			//見つからなかったら新しくフィールドごと追加する
			if(!find_field){
				List<Pair<BoolExpr,List<Pair<Expr,List<IntExpr>>>>> b_is = new ArrayList<Pair<BoolExpr,List<Pair<Expr,List<IntExpr>>>>>();
				ArrayList<Pair<Expr,List<IntExpr>>> indexs = new ArrayList<Pair<Expr,List<IntExpr>>>();
				indexs.add(new Pair(cr_l.class_expr, cr_l.indexs));
				b_is.add(new Pair(cs.get_pathcondition(), indexs));
				assigned_fields.fst.add(new F_Assign(cr_l.field, b_is));
			}
			
		}
		

		
		Check_return cr_r =  this.implies_expr.loop_assign(assigned_fields, cs);
		if(this.postfix_expr!=null){
			//1次元以上の配列としてエイリアスした場合には、それ以降配列を代入する前に篩型の検証を行わなければならない
			if(cr_l.field.hava_refinement_type() && cr_l.field.have_index_access(cs) 
					&& cr_r.field != null && cr_r.field.hava_refinement_type() && cr_r.field.have_index_access(cs) 
					&& cr_l.field.dims - cr_l.indexs.size() >= 1){
				
				if(cs.in_helper){
					if(cr_l.field instanceof Variable){
						cr_l.field.alias_1d_in_helper = cs.ctx.mkOr(cr_l.field.alias_1d_in_helper, cs.get_pathcondition());
					}
					if(cr_r.field instanceof Variable){
						cr_r.field.alias_1d_in_helper = cs.ctx.mkOr(cr_r.field.alias_1d_in_helper, cs.get_pathcondition());
					}
				}else if(cs.in_constructor){
					if(!(cr_l.field instanceof Variable) && cs.this_field.get_Expr(cs).equals(cr_l.class_expr)){
						cr_l.field.alias_in_consutructor_or_2d_in_helper = cs.ctx.mkOr(cr_l.field.alias_in_consutructor_or_2d_in_helper, cs.get_pathcondition());
					}
					if(!(cr_r.field instanceof Variable) && cs.this_field.get_Expr(cs).equals(cr_r.class_expr)){
						cr_r.field.alias_in_consutructor_or_2d_in_helper = cs.ctx.mkOr(cr_r.field.alias_in_consutructor_or_2d_in_helper, cs.get_pathcondition());
					}
				}
			}
			//2次元以上の配列としてエイリアスした場合には、それ以降篩型を満たさなければいけない
			if(cr_l.field.hava_refinement_type() && cr_l.field.have_index_access(cs) 
					&& cr_r.field != null && cr_r.field.hava_refinement_type() && cr_r.field.have_index_access(cs) 
					&& cr_l.field.dims - cr_l.indexs.size() >= 2){
				
				if(cs.in_helper){
					if(cr_l.field instanceof Variable){
						cr_l.field.alias_in_consutructor_or_2d_in_helper = cs.ctx.mkOr(cr_l.field.alias_in_consutructor_or_2d_in_helper, cs.get_pathcondition());
					}
					if(cr_r.field instanceof Variable){
						cr_r.field.alias_in_consutructor_or_2d_in_helper = cs.ctx.mkOr(cr_r.field.alias_in_consutructor_or_2d_in_helper, cs.get_pathcondition());
					}
				}
			}
			return cr_l;
		}
		return cr_r;
	}

		
}

