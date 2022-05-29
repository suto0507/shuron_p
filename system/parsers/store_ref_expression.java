package system.parsers;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;

import system.Check_status;
import system.Field;
import system.Parser;
import system.Parser_status;
import system.Source;

public class store_ref_expression implements Parser<String>{
	store_ref_name store_ref_name;
	List<store_ref_name_suffix> store_ref_name_suffix;
	String st;
	public String parse(Source s,Parser_status ps)throws Exception{
		st = "";
		this.store_ref_name = new store_ref_name();
		st = st + this.store_ref_name.parse(s, ps);
		Source s_backup = s.clone();
		this.store_ref_name_suffix = new ArrayList<store_ref_name_suffix>();
		try {
			while(true){
				s_backup = s.clone();
				store_ref_name_suffix srns = new store_ref_name_suffix();
				st = st + srns.parse(s, ps);
				this.store_ref_name_suffix.add(srns);				}
		}catch (Exception e){
			s.revert(s_backup);
		}
		
		return st;
	}
	
	public Field check(Check_status cs) throws Exception{//exいらない？きれいにできそう
		Expr ex = null;
		Field f = null;
		String ident = null;
		if(this.store_ref_name_suffix.size() == 0){
			if(this.store_ref_name.is_this){
				throw new Exception("Cannot be assigned to \"this\"");
			}else if(this.store_ref_name.ident!=null){
				/*
				if(cs.in_refinement_predicate==true){
					if(this.ident.equals(cs.refinement_type_value)){
						return cs.refined_Expr;
					}
				}
				*/
				if(cs.in_method_call){//関数呼び出し
					Field searched_field = cs.search_field(this.store_ref_name.ident, cs.call_field, cs.call_field_index, cs);
					if(cs.search_called_method_arg(this.store_ref_name.ident)){//これいらんくないか？
						f = cs.get_called_method_arg(this.store_ref_name.ident);
						ex = f.get_Expr(cs);
					}else if(searched_field != null){
						f = searched_field;
						ex = cs.ctx.mkSelect((ArrayExpr) f.get_Expr(cs),cs.call_expr);
					}
				}else{
					Field searched_field = cs.search_field(this.store_ref_name.ident, cs.this_field, null, cs);
					if(cs.search_variable(this.store_ref_name.ident)){
						f = cs.get_variable(this.store_ref_name.ident);
						ex = f.get_Expr(cs);
					}else if(searched_field != null){
						f = searched_field;
						ex = cs.ctx.mkSelect((ArrayExpr) f.get_Expr(cs), cs.this_field.get_Expr(cs));
					}
				}
			}
		}else{
			if(this.store_ref_name.is_this){
				f = cs.this_field;
				ex = f.get_Expr(cs);
				//関数呼び出し
				if(cs.in_method_call){
					f = cs.call_field;
					ex = cs.call_expr;
				}
			}else if(this.store_ref_name.ident!=null){
				
				if(cs.in_method_call){//関数呼び出し
					Field searched_field = cs.search_field(store_ref_name.ident, cs.call_field, cs.call_field_index, cs);
					if(cs.search_called_method_arg(store_ref_name.ident)){//これいらない？
						f = cs.get_called_method_arg(store_ref_name.ident);
						ex = f.get_Expr(cs);
					}else if(searched_field != null){
						f = searched_field;
						ex = cs.ctx.mkSelect((ArrayExpr) f.get_Expr(cs),cs.call_expr);
						if(1 == this.store_ref_name_suffix.size() && this.store_ref_name_suffix.get(0).is_index){//次が最後かつ配列のとき
							f.assign_Expr = cs.ctx.mkSelect((ArrayExpr) f.get_Expr_assign(cs), cs.call_expr);
							f.assign_now_array_Expr = ex;
						}
					}else{//この中で初っ端関数は有り得ない？
						ident = this.store_ref_name.ident;
						f = cs.call_field;//嘘
						ex = f.get_Expr(cs);
					}
				}else{
					Field searched_field = cs.search_field(store_ref_name.ident, cs.this_field, null, cs);
					if(cs.search_variable(store_ref_name.ident)){
						f = cs.get_variable(store_ref_name.ident);
						ex = f.get_Expr(cs);
					}else if(searched_field != null){
						f = searched_field;
						ex = cs.ctx.mkSelect((ArrayExpr) f.get_Expr(cs),cs.this_field.get_Expr(cs));
					}else{
						ident = this.store_ref_name.ident;
						f = cs.this_field;
						ex = f.get_Expr(cs);
					}
				}
				if(cs.in_refinement_predicate==true){
					if(this.store_ref_name.ident.equals(cs.refinement_type_value)){
						f = cs.refined_Field;
						ex = cs.refined_Expr;
					}
				}
				
				
				
			}
			//suffixについて
			IntExpr f_index = null;
			for(int i = 0; i < this.store_ref_name_suffix.size(); i++){
				store_ref_name_suffix ps = this.store_ref_name_suffix.get(i);
				if(ps.is_field){
					if(ident!=null){
						Field searched_field = cs.search_field(ident, f, f_index, cs);
						if(searched_field != null){
							f = searched_field;
							ex = cs.ctx.mkSelect((ArrayExpr)f.get_Expr(cs), ex);
						}else{
							throw new Exception(ident + " dont exist");
						}
					}
					ident = ps.ident;
					
					//最後がフィールドの時、または次が配列のとき
					if(i == this.store_ref_name_suffix.size()-1 || this.store_ref_name_suffix.get(i+1).is_index){
						Expr pre_ex = ex;
						
						Field searched_field = cs.search_field(ident, f, f_index, cs);
						if(searched_field != null){
							f = searched_field;
							ex = cs.ctx.mkSelect((ArrayExpr)f.get_Expr(cs), ex);
						}else{
							throw new Exception(ident + " dont exist");
						}
						
						if(i == this.store_ref_name_suffix.size()-2 && this.store_ref_name_suffix.get(i+1).is_index){//次が最後かつ配列のとき
							f.assign_Expr = cs.ctx.mkSelect((ArrayExpr) f.get_Expr_assign(cs), pre_ex);
							f.assign_now_array_Expr = ex;
						}
					}
					
					f_index = null;
					
				}else if(ps.is_index){
					f_index = (IntExpr) ps.spec_array_ref_expr.spec_expression.check(cs);
					ex = cs.ctx.mkSelect((ArrayExpr) ex, f_index);
					if(i == this.store_ref_name_suffix.size()-1){
						if(cs.in_method_call){
							f.index = f_index;
						}else{
							f.assinable_indexs.add(f_index);
						}
					}else{
					}
					ident = null;
				}
			}
			

		}
		
		return f;
	}

}

