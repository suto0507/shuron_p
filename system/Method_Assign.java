package system;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.Sort;

//メソッドでの代入に関して
//書き途中
public class Method_Assign {
	
	public BoolExpr all_assign_condition;//他の事前条件は除かれている状態
	public ArrayList<Case_Assign> case_assigns;
	public ArrayList<Field> fields;//代入されるフィールド　配列の要素への代入は含まない
	public ArrayList<Check_return> arrays;//配列のフィールドへの代入
	
	public Method_Assign(BoolExpr all_assign_condition){
		this.all_assign_condition = all_assign_condition;
		case_assigns = new ArrayList<Case_Assign>();
		fields = new ArrayList<Field>();
		arrays = new ArrayList<Check_return>();
	}
	
	public Method_Assign(BoolExpr all_assign_condition, ArrayList<Case_Assign> case_assigns){
		this.all_assign_condition = all_assign_condition;
		this.case_assigns = case_assigns;
		
		fields = new ArrayList<Field>();
		arrays = new ArrayList<Check_return>();
		for(Case_Assign ca : case_assigns){
			for(Check_return cr : ca.field_assigns){
				if(!fields.contains(cr.field) && (cr.indexs==null || cr.indexs.size() == 0)){
					fields.add(cr.field);
				}
				if(!arrays.contains(cr.field) && (cr.field.dims > 0)){
					arrays.add(cr);
				}
			}
		}
	}
	
	//配列に関して
	public BoolExpr assign_condition(Expr array_ref, IntExpr index, Check_status cs) throws Exception{
		BoolExpr cannot_assign = cs.ctx.mkFalse();
		for(Case_Assign ca : case_assigns){
			cannot_assign = cs.ctx.mkOr(cannot_assign, ca.cannot_assign_condition(array_ref, index, cs));
		}
		
		
		BoolExpr can_assign = cs.ctx.mkOr(cs.ctx.mkNot(cannot_assign), all_assign_condition);
		return can_assign;
	}
	
	//フィールドに関して
	public BoolExpr assign_condition(Field f, Expr class_expr, Check_status cs){
		BoolExpr cannot_assign = cs.ctx.mkFalse();
		for(Case_Assign ca : case_assigns){
			cannot_assign = cs.ctx.mkOr(cannot_assign, ca.cannot_assign_condition(f, class_expr, cs));
		}
		
		
		BoolExpr can_assign = cs.ctx.mkOr(cs.ctx.mkNot(cannot_assign), all_assign_condition);
		return can_assign;
	}
	
	
	//このメソッドによる代入処理を行う
	public void assign_all(Check_status cs) throws Exception{
		//それぞれの代入に関して、代入していい条件を作る
		for(Case_Assign ca : case_assigns){
			for(Check_return cr : ca.field_assigns){
				if(cr.indexs!=null && cr.indexs.size()>0){//配列の代入
					Expr cr_array_ref = cr.field.get_Expr_with_indexs(cr.class_expr, new ArrayList<IntExpr>(cr.indexs.subList(0, cr.indexs.size()-1)), cs);
					IntExpr cr_index = cr.indexs.get(cr.indexs.size()-1);
					BoolExpr can_assign = assign_condition(cr_array_ref, cr_index, cs);
					
					Array array = null;
					if(cr.indexs.size()<cr.field.dims){
						array = cs.array_arrayref;
					}else{
						if(cr.field.type.equals("int")){
							array = cs.array_int;
						}else if(cr.field.type.equals("boolean")){
							array = cs.array_boolean;
						}else{
							array = cs.array_ref;
						}
					}
					Expr pre_value = array.index_access_array(cr_array_ref, cr_index, cs);
					Expr fresh_value = cs.ctx.mkITE(can_assign, cs.ctx.mkConst("freshValue_" + cs.Check_status_share.get_tmp_num(), array.elements_Sort), pre_value);
					array.update_array(cr_array_ref, cr_index, fresh_value, cs);
					
					//配列への代入でも、modelフィールドは変わる
					for(Model_Field mf : cr.field.model_fields){
						mf.tmp_plus_with_data_group(can_assign, cs);
					}
				}else{//フィールドへの代入
					BoolExpr can_assign = assign_condition(cr.field, cr.class_expr, cs);
					
					Expr expr;
					if(cr.field instanceof Variable){
						Expr pre_expr = cr.field.get_Expr(cs);
						expr = cs.ctx.mkITE(can_assign, cr.field.get_fresh_value(cs), pre_expr);
					}else{
						Expr pre_expr = cs.ctx.mkSelect(cr.field.get_Expr(cs), cr.class_expr);
						expr = cs.ctx.mkStore(cr.field.get_Expr(cs), cr.class_expr, cs.ctx.mkITE(can_assign, cr.field.get_fresh_value(cs), pre_expr));
					}
					cs.add_constraint(cs.ctx.mkEq(cr.field.get_Expr_assign(cs), expr));
					cr.field.tmp_plus_with_data_group(cr.class_expr, cs);
				}
			}
		}
	}

	//配列への代入の列挙 helperメソッドで使う
	public ArrayList<Pair<Check_return, BoolExpr>> array_assign(Check_status cs) throws Exception{
		ArrayList<Pair<Check_return, BoolExpr>> array_assigns = new ArrayList<Pair<Check_return, BoolExpr>>();
		
		for(Check_return cr : arrays){
			if(cr.field.dims > 0 && (cr.indexs == null || cr.field.dims != cr.indexs.size())){
				BoolExpr can_assign;
				if(cr.indexs.size()>0){//配列への代入
					Expr cr_array_ref = cr.field.get_Expr_with_indexs(cr.class_expr, new ArrayList<IntExpr>(cr.indexs.subList(0, cr.indexs.size()-1)), cs);
					IntExpr cr_index = cr.indexs.get(cr.indexs.size()-1);
					can_assign = assign_condition(cr_array_ref, cr_index, cs);
				}else{
					can_assign = assign_condition(cr.field, cr.class_expr, cs);
				}
				array_assigns.add(new Pair<Check_return, BoolExpr>(cr, can_assign));
			}
		}
		
		return array_assigns;
	}
	
	//ループの前処理で、このメソッドが何に代入をするのかを処理する
	public void loop_assign(Pair<List<F_Assign>,BoolExpr>assigned_fields, Check_status cs) throws Exception{
		//それぞれの代入に関して、代入していい条件を作る
		for(Case_Assign ca : case_assigns){
			for(Check_return cr : ca.field_assigns){
				BoolExpr can_assign;
				if(cr.indexs!=null && cr.indexs.size()>0){//配列の代入
					Expr cr_array_ref = cr.field.get_Expr_with_indexs(cr.class_expr, new ArrayList<IntExpr>(cr.indexs.subList(0, cr.indexs.size()-1)), cs);
					IntExpr cr_index = cr.indexs.get(cr.indexs.size()-1);
					can_assign = assign_condition(cr_array_ref, cr_index, cs);
				}else{//フィールドへの代入
					can_assign = assign_condition(cr.field, cr.class_expr, cs);
				}
				
				boolean find_field = false;
				for(F_Assign f_i : assigned_fields.fst){
					if(f_i.field.equals(cr.field, cs) ){//見つかったら追加する
						find_field = true;
						f_i.cnst_array.add(new Pair<BoolExpr,Pair<Expr, List<IntExpr>>>(cs.ctx.mkAnd(cs.get_pathcondition(), can_assign), new Pair<Expr, List<IntExpr>>(cr.class_expr, cr.indexs)));
						break;
					}
				}
				//見つからなかったら新しくフィールドごと追加する
				if(!find_field){
					List<Pair<BoolExpr,Pair<Expr, List<IntExpr>>>> b_is = new ArrayList<Pair<BoolExpr,Pair<Expr, List<IntExpr>>>>();
					b_is.add(new Pair<BoolExpr,Pair<Expr, List<IntExpr>>>(cs.ctx.mkAnd(cs.get_pathcondition(), can_assign), new Pair<Expr, List<IntExpr>>(cr.class_expr, cr.indexs)));
					assigned_fields.fst.add(new F_Assign(cr.field, b_is));
				}
			}
		}
		
		//なんでも代入できる場合
		assigned_fields.snd = cs.ctx.mkOr(assigned_fields.snd, cs.ctx.mkAnd(cs.get_pathcondition(), all_assign_condition));
	}
}


