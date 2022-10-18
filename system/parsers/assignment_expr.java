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
			
			
			
		}
		Check_return rc = this.implies_expr.check(cs);
		Expr implies_tmp = rc.expr;
		
		
		
		if(this.postfix_expr != null){
			
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
			

			//配列の篩型が安全かどうか
			BoolExpr pathcondition;
			if(cs.pathcondition==null){
				pathcondition = cs.ctx.mkBool(true);
			}else{
				pathcondition = cs.pathcondition;
			}
			if(rc.field!=null && rc.field.dims>0 && rc.field.dims_sum()!=rc.indexs.size() && rc.field.refinement_type_clause!=null && rc.field.refinement_type_clause.have_index_access(rc.field.class_object.type, cs)){
				if(v.dims>0 && v.dims_sum()!=indexs.size() && v.refinement_type_clause!=null && v.refinement_type_clause.have_index_access(v.class_object.type, cs)){//どっちも篩型を持つ配列
					Expr rc_assign_field_expr = rc.field.get_full_Expr(new ArrayList<IntExpr>(rc.indexs.subList(0, rc.field.class_object_dims_sum())), cs);
					rc.field.refinement_type_clause.equal_predicate(rc.indexs, rc_assign_field_expr, rc.field.class_object, rc.field.class_object.get_full_Expr(rc.indexs, cs), v.refinement_type_clause, indexs, assign_field_expr, v.class_object, v_class_object_expr, cs);
				}else if(v.dims>0 && v.dims_sum()!=indexs.size() && v instanceof Variable){//ローカル変数
					if(((Variable)v).out_loop_v) throw new Exception("can not alias with refined array　in loop");//ループの中ではエイリアスできない
					
					Expr alias;
					if(((Variable) v).alias == null){
						alias = cs.ctx.mkBool(false);
					}else{
						alias = ((Variable) v).alias;
					}
					
					cs.assert_constraint(cs.ctx.mkNot(alias));
					
					Expr alias_refined;
					if(((Variable) v).alias_refined == null){
						alias_refined = cs.ctx.mkBool(false);
					}else{
						alias_refined = ((Variable) v).alias_refined;
					}
					
					cs.assert_constraint(cs.ctx.mkNot(alias_refined));
					
					if(((Variable) v).alias_refined == null){
						((Variable) v).alias_refined = pathcondition;
					}else{
						((Variable) v).alias_refined = cs.ctx.mkOr(((Variable) v).alias_refined, pathcondition);
					}
				}else{//篩型の安全を保証できないような大入
					throw new Exception("can not alias with refined array");
				}
			}else if(v.dims>0 && v.dims_sum()!=indexs.size()  && v.refinement_type_clause!=null && v.refinement_type_clause.have_index_access(v.class_object.type, cs)){
				if(rc.field!=null && rc.field.dims>0 && rc.field.dims_sum()!=rc.indexs.size() && rc.field instanceof Variable){//ローカル変数
					
					if(((Variable)rc.field).out_loop_v) throw new Exception("can not alias with refined array　in loop");//ループの中ではエイリアスできない
					
					Expr alias;
					if(((Variable) rc.field).alias == null){
						alias = cs.ctx.mkBool(false);
					}else{
						alias = ((Variable) rc.field).alias;
					}
					
					cs.assert_constraint(cs.ctx.mkNot(alias));
					
					Expr alias_refined;
					if(((Variable) rc.field).alias_refined == null){
						alias_refined = cs.ctx.mkBool(false);
					}else{
						alias_refined = ((Variable) rc.field).alias_refined;
					}
					
					cs.assert_constraint(cs.ctx.mkNot(alias_refined));
					
					if(((Variable) rc.field).alias_refined == null){
						((Variable) rc.field).alias_refined = pathcondition;
					}else{
						((Variable) rc.field).alias_refined = cs.ctx.mkOr(((Variable) rc.field).alias_refined, pathcondition);
					}
				}else{//篩型の安全を保証できないような大入
					throw new Exception("can not alias with refined array");
				}	
			}else{ 
				if(rc.field!=null && rc.field.dims>0 && rc.field.dims_sum()!=rc.indexs.size() && rc.field instanceof Variable && !v.new_array){//ローカル変数
					Expr alias_refined;
					if(((Variable) rc.field).alias_refined == null){
						alias_refined = cs.ctx.mkBool(false);
					}else{
						alias_refined = ((Variable) rc.field).alias_refined;
					}
					
					cs.assert_constraint(cs.ctx.mkNot(alias_refined));
					
					if(((Variable) rc.field).alias == null){
						((Variable) rc.field).alias = pathcondition;
					}else{
						((Variable) rc.field).alias = cs.ctx.mkOr(((Variable) rc.field).alias, pathcondition);
					}
				}
				if(v!=null && v.dims>0 && v.dims_sum()!=indexs.size() && v instanceof Variable && !(rc.field!=null && rc.field.new_array)){//ローカル変数
					Expr alias_refined;
					if(((Variable) v).alias_refined == null){
						alias_refined = cs.ctx.mkBool(false);
					}else{
						alias_refined = ((Variable) v).alias_refined;
					}
					
					cs.assert_constraint(cs.ctx.mkNot(alias_refined));
					
					
					if(((Variable) v).alias == null){
						((Variable) v).alias = pathcondition;
					}else{
						((Variable) v).alias = cs.ctx.mkOr(((Variable) v).alias, pathcondition);
					}
					
				}else if(v!=null && v.dims>0 && v.dims_sum()==indexs.size() && v instanceof Variable){//篩型を持つ配列とエイリアスしたローカル配列は、要素の変更はできない
					
					Expr alias_refined;
					if(((Variable) v).alias_refined == null){
						alias_refined = cs.ctx.mkBool(false);
					}else{
						alias_refined = ((Variable) v).alias_refined;
					}
					
					cs.assert_constraint(cs.ctx.mkNot(alias_refined));
				}
			}
			
			
			//篩型の検証
			if(v.refinement_type_clause!=null && cs.in_helper){//helperメソッドの中では、篩型の検証を後回しにする
				Pair<BoolExpr, Pair<Field, ArrayList<IntExpr>>> assigned_field = new Pair(cs.pathcondition, new Pair(v, new ArrayList<IntExpr>(v.index.subList(0, v.class_object_dims_sum()))));
				cs.helper_assigned_fields.add(assigned_field);
			}else if(v.refinement_type_clause!=null && !(cs.in_constructor&&v.class_object.equals(cs.this_field, cs))){
				if(v.refinement_type_clause.refinement_type!=null){
					v.refinement_type_clause.refinement_type.assert_refinement(cs, v, assign_field_expr, v.class_object, v_class_object_expr, new ArrayList<IntExpr>(v.index.subList(0, v.class_object_dims_sum())));
				}else if(v.refinement_type_clause.ident!=null){
					refinement_type rt = cs.search_refinement_type(v.class_object.type, v.refinement_type_clause.ident);
					if(rt!=null){
						rt.assert_refinement(cs, v, assign_field_expr, v.class_object, v_class_object_expr, new ArrayList<IntExpr>(v.index.subList(0, v.class_object_dims_sum())));
					}else{
		                throw new Exception("can't find refinement type " + v.refinement_type_clause.ident);
		            }
				}
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
		
		if(this.postfix_expr!=null){
			Check_return cr = this.postfix_expr.loop_assign(assigned_fields, cs);
			
			boolean find_field = false;
			for(Pair<Field,List<List<IntExpr>>> f_i : assigned_fields.fst){
				if(f_i.fst == cr.field){//見つかったら追加する
					find_field = true;
					f_i.snd.add(cr.indexs);
					break;
				}
			}
			//見つからなかったら新しくフィールドごと追加する
			if(!find_field){
				List<List<IntExpr>> f_indexs_snd = new ArrayList<List<IntExpr>>();
				f_indexs_snd.add(cr.indexs);
				Pair<Field,List<List<IntExpr>>> f_i = new Pair<Field,List<List<IntExpr>>>(cr.field, f_indexs_snd);
				assigned_fields.fst.add(f_i);
			}
		}
		return this.implies_expr.loop_assign(assigned_fields, cs);
	}

		
}

