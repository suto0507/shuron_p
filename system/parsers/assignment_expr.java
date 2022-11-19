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
		Field v = null;
		Expr assign_expr = null;//左辺のExpr
		Expr assign_field_expr = null;//篩型をもっていた時のために使う this.a.b.x[1]のthis.a.b.xの部分
		Expr assign_expr_full = null;//左辺のExprのfullバージョン　 返り値として返す
		ArrayList<IntExpr> indexs = null;//左辺のindexs
		ArrayList<IntExpr> v_indexs = null;//左辺のindexs this.a[1][2].b.x[3][4]の3,4の部分
		Expr v_class_object_expr = null;
		if(this.postfix_expr != null){
			v = this.postfix_expr.check_assign(cs);
			//代入していいかどうかの検証
			if(v.is_this_field()){//フィールドかどうか？
				//assignしていいか
				
				BoolExpr ex;
				
				
				ex = v.assign_index_expr(v.index, cs);
				
				
				//何でも代入していい
				ex = cs.ctx.mkOr(ex, cs.assinable_cnst_all);
				
				//コンストラクタ
				if(!(cs.in_constructor&&v.class_object.equals(cs.this_field, cs))){
					System.out.println("check assign");
					cs.assert_constraint(ex);
				}
				
				//finalかどうか
				if(v.modifiers!=null && v.modifiers.is_final){
					if(cs.in_constructor&&v.class_object.equals(cs.this_field, cs)&&v.final_initialized==false){
						v.final_initialized = true;
					}else{
						throw new Exception("Cannot be assigned to " + v.field_name);
					}
				}
			}
			
			//左辺のExpr
			indexs = v.index;
			
			v_indexs = new ArrayList<IntExpr>(v.index.subList(v.class_object_dims_sum(), v.index.size()));
			
			v_class_object_expr = v.class_object_expr;
			
			
			if(cs.in_helper || (cs.in_constructor && !(v instanceof Variable) && v.class_object != null && v.class_object.equals(cs.this_field, cs))){
				//helperメソッド、コンストラクタでは、配列を代入前に検証が必要な場合がある
				if(v.hava_refinement_type() && v.have_index_access(cs) && indexs.size() < v.dims_sum()){
					cs.solver.push();
					
					//エイリアスしているときだけでいい
					cs.add_constraint(v.alias_in_helper_or_consutructor);
					
					Field old_v = cs.this_old_status.search_internal_id(v.internal_id);

					Expr old_expr = null;
					Expr expr = null;
					if(v instanceof Variable){
						expr = v.get_Expr(cs);
					}else{
						old_expr = cs.ctx.mkSelect(old_v.get_Expr(cs), v_class_object_expr);
						expr = cs.ctx.mkSelect(v.get_Expr(cs), v_class_object_expr);
					}
					
					//メソッドの最初では篩型が満たしていることを仮定していい
					//フィールドだけ
					if(cs.in_helper && !(v instanceof Variable)){
						old_v.add_refinement_constraint(cs, v_class_object_expr, new ArrayList<IntExpr>(indexs.subList(0, v.class_object_dims_sum())), true);
					}
					
					v.assert_refinement(cs, v_class_object_expr, new ArrayList<IntExpr>(indexs.subList(0, v.class_object_dims_sum())));
					
					cs.solver.pop();
				}
				
				
			}
			
		}
		Check_return rc = this.implies_expr.check(cs);
		Expr implies_tmp = rc.expr;
		
		
		
		if(this.postfix_expr != null){
			//1次元以上の配列としてエイリアスした場合には、それ以降配列を代入する前に篩型の検証を行わなければならない
			if(v.hava_refinement_type() && rc.field != null && rc.field.hava_refinement_type() && indexs.size()+1 <= v.dims_sum() ){
				if(cs.in_helper){
					if(v instanceof Variable)v.alias_in_helper_or_consutructor = cs.ctx.mkOr(v.alias_in_helper_or_consutructor, cs.get_pathcondition());
					if(rc.field instanceof Variable)rc.field.alias_in_helper_or_consutructor = cs.ctx.mkOr(rc.field.alias_in_helper_or_consutructor, cs.get_pathcondition());
				}else if(cs.in_constructor){
					if(!(v instanceof Variable) && v.class_object != null && v.class_object.equals(cs.this_field, cs)){
						v.alias_in_helper_or_consutructor = cs.ctx.mkOr(v.alias_in_helper_or_consutructor, cs.get_pathcondition());
					}
					if(!(rc.field instanceof Variable) && rc.field.class_object != null && rc.field.class_object.equals(cs.this_field, cs)){
						rc.field.alias_in_helper_or_consutructor = cs.ctx.mkOr(rc.field.alias_in_helper_or_consutructor, cs.get_pathcondition());
					}
				}
			}
			//2次元以上の配列としてエイリアスした場合には、それ以降篩型を満たさなければいけない
			if(v.hava_refinement_type() && rc.field != null && rc.field.hava_refinement_type() && indexs.size()+2 <= v.dims_sum() ){
				if(cs.in_helper){
					if(v instanceof Variable)v.alias_2d_in_helper_or_consutructor = cs.ctx.mkOr(v.alias_2d_in_helper_or_consutructor, cs.get_pathcondition());
					if(rc.field instanceof Variable)rc.field.alias_2d_in_helper_or_consutructor = cs.ctx.mkOr(rc.field.alias_2d_in_helper_or_consutructor, cs.get_pathcondition());
				}else if(cs.in_constructor){
					if(!(v instanceof Variable) && v.class_object != null && v.class_object.equals(cs.this_field, cs)){
						v.alias_2d_in_helper_or_consutructor = cs.ctx.mkOr(v.alias_2d_in_helper_or_consutructor, cs.get_pathcondition());
					}
					if(!(rc.field instanceof Variable) && rc.field.class_object != null && rc.field.class_object.equals(cs.this_field, cs)){
						rc.field.alias_2d_in_helper_or_consutructor = cs.ctx.mkOr(rc.field.alias_2d_in_helper_or_consutructor, cs.get_pathcondition());
					}
				}
			}
			
			assign_expr = v.get_Expr_assign(cs);
			
			if(v instanceof Variable){
				assign_field_expr = v.get_Expr_assign(cs);
			}else{
				assign_field_expr = cs.ctx.mkSelect(v.get_Expr_assign(cs), v_class_object_expr);
			}
			
			assign_expr_full = assign_field_expr;
			for(IntExpr index : v_indexs){
				cs.ctx.mkSelect(assign_expr_full, index);
			}
			
			
			BoolExpr expr = cs.ctx.mkEq(assign_expr, v.assign_value(indexs, implies_tmp, cs));
			cs.add_constraint(expr);
			
			
			
			v.temp_num++;
			
			
			//refinement_type
			//篩型を持つ配列とエイリアスしたローカル配列は、要素の変更はできない
			if(v.dims>0 && !(v.hava_refinement_type() && v.have_index_access(cs))
					 && v.dims_sum()==indexs.size() && v instanceof Variable){
				Expr alias_refined;
				if(((Variable) v).alias_refined == null){
					alias_refined = cs.ctx.mkBool(false);
				}else{
					alias_refined = ((Variable) v).alias_refined;
				}
				cs.assert_constraint(cs.ctx.mkNot(alias_refined));
			}
			
			//配列の篩型が安全かどうか
			Expr rc_assign_field_expr = null;
			Expr rc_class_field_expr = null;
			if(rc.field!=null){
				rc_assign_field_expr = rc.field.get_full_Expr(new ArrayList<IntExpr>(rc.indexs.subList(0, rc.field.class_object_dims_sum())), cs);
				rc_class_field_expr = rc.field.class_object.get_full_Expr((ArrayList<IntExpr>) rc.indexs.clone(), cs);
			}
			cs.check_array_alias(v, assign_field_expr, v_class_object_expr, indexs, rc.field, rc_assign_field_expr, rc_class_field_expr, rc.indexs);
			
			
			
			
			

			BoolExpr pathcondition;
			if(cs.pathcondition==null){
				pathcondition = cs.ctx.mkBool(true);
			}else{
				pathcondition = cs.pathcondition;
			}
			
			
			
			//篩型の検証
			if((v.hava_refinement_type() && cs.in_helper)
					|| (v.hava_refinement_type() && (cs.in_constructor && !(v instanceof Variable) && v.class_object.equals(cs.this_field, cs)))){//helperメソッド、コンストラクタの中では、篩型の検証を後回しにする
				if(v.dims >= 2 && v.have_index_access(cs)){//2次元以上の配列としてエイリアスしている場合には、篩型の検証をしないといけない
					cs.solver.push();
					cs.add_constraint(v.alias_2d_in_helper_or_consutructor);

					v.assert_refinement(cs, v_class_object_expr, new ArrayList<IntExpr>(v.index.subList(0, v.class_object_dims_sum())));
					cs.solver.pop();
				}
				if(cs.in_helper){
					Helper_assigned_field assigned_field = new Helper_assigned_field(pathcondition, v, v_class_object_expr, new ArrayList<IntExpr>(v.index.subList(0, v.class_object_dims_sum())));
					cs.helper_assigned_fields.add(assigned_field);
				}
			}else if(v.hava_refinement_type()){
				v.assert_refinement(cs, v_class_object_expr, new ArrayList<IntExpr>(v.index.subList(0, v.class_object_dims_sum())));
			}
			
			return new Check_return(assign_expr_full, v, indexs);
		}else{
			return rc;
		}
	}
	
	public boolean have_index_access(Check_status cs){
 		if(this.postfix_expr!=null) return implies_expr.have_index_access(cs) || postfix_expr.have_index_access(cs);
		return implies_expr.have_index_access(cs);
	}
	
	public Check_return loop_assign(Pair<List<Pair<Field,List<List<IntExpr>>>>,Boolean>assigned_fields, Check_status cs) throws Exception{
		Check_return cr_l = null;
		if(this.postfix_expr!=null){
			cr_l = this.postfix_expr.loop_assign(assigned_fields, cs);
			
			boolean find_field = false;
			for(Pair<Field,List<List<IntExpr>>> f_i : assigned_fields.fst){
				if(f_i.fst == cr_l.field){//見つかったら追加する
					find_field = true;
					f_i.snd.add(cr_l.indexs);
					break;
				}
			}
			//見つからなかったら新しくフィールドごと追加する
			if(!find_field){
				List<List<IntExpr>> f_indexs_snd = new ArrayList<List<IntExpr>>();
				f_indexs_snd.add(cr_l.indexs);
				Pair<Field,List<List<IntExpr>>> f_i = new Pair<Field,List<List<IntExpr>>>(cr_l.field, f_indexs_snd);
				assigned_fields.fst.add(f_i);
			}
			
		}
		

		
		Check_return cr_r =  this.implies_expr.loop_assign(assigned_fields, cs);
		if(this.postfix_expr!=null){
			if(cs.in_constructor || cs.in_helper){
				
				//1次元以上の配列としてエイリアスした場合には、それ以降配列を代入する前に篩型の検証を行わなければならない
				if(cr_l.field.hava_refinement_type() && cr_r.field != null && cr_r.field.hava_refinement_type() && cr_l.indexs.size()+1 <= cr_l.field.dims_sum() ){
					if(cs.in_helper){
						if(cr_l.field instanceof Variable)cr_l.field.alias_in_helper_or_consutructor = cs.ctx.mkOr(cr_l.field.alias_in_helper_or_consutructor, cs.get_pathcondition());
						if(cr_r.field instanceof Variable)cr_r.field.alias_in_helper_or_consutructor = cs.ctx.mkOr(cr_r.field.alias_in_helper_or_consutructor, cs.get_pathcondition());
					}else if(cs.in_constructor){
						if(!(cr_l.field instanceof Variable) && cr_l.field.class_object != null && cr_l.field.class_object.equals(cs.this_field, cs)){
							cr_l.field.alias_in_helper_or_consutructor = cs.ctx.mkOr(cr_l.field.alias_in_helper_or_consutructor, cs.get_pathcondition());
						}
						if(!(cr_r.field instanceof Variable) && cr_r.field.class_object != null && cr_r.field.class_object.equals(cs.this_field, cs)){
							cr_r.field.alias_in_helper_or_consutructor = cs.ctx.mkOr(cr_r.field.alias_in_helper_or_consutructor, cs.get_pathcondition());
						}
					}
				}
				//2次元以上の配列としてエイリアスした場合には、それ以降篩型を満たさなければいけない
				if(cr_l.field.hava_refinement_type() && cr_r.field != null && cr_r.field.hava_refinement_type() && cr_l.indexs.size()+2 <= cr_l.field.dims_sum() ){
					if(cs.in_helper){
						if(cr_l.field instanceof Variable)cr_l.field.alias_2d_in_helper_or_consutructor = cs.ctx.mkOr(cr_l.field.alias_2d_in_helper_or_consutructor, cs.get_pathcondition());
						if(cr_r.field instanceof Variable)cr_r.field.alias_2d_in_helper_or_consutructor = cs.ctx.mkOr(cr_r.field.alias_2d_in_helper_or_consutructor, cs.get_pathcondition());
					}else if(cs.in_constructor){
						if(!(cr_l.field instanceof Variable) && cr_l.field.class_object != null && cr_l.field.class_object.equals(cs.this_field, cs)){
							cr_l.field.alias_2d_in_helper_or_consutructor = cs.ctx.mkOr(cr_l.field.alias_2d_in_helper_or_consutructor, cs.get_pathcondition());
						}
						if(!(cr_r.field instanceof Variable) && cr_r.field.class_object != null && cr_r.field.class_object.equals(cs.this_field, cs)){
							cr_r.field.alias_2d_in_helper_or_consutructor = cs.ctx.mkOr(cr_r.field.alias_2d_in_helper_or_consutructor, cs.get_pathcondition());
						}
					}
				}
			}
			return cr_l;
		}
		return cr_r;
	}

		
}

