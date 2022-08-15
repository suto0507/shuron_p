package system.parsers;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;

import system.Check_status;
import system.Field;
import system.Pair;
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
	
	public Pair<Field, List<IntExpr>> check(Check_status cs) throws Exception{
		Expr ex = null;
		Field f = null;
		List<IntExpr> indexs = new ArrayList<IntExpr>();
		if(this.store_ref_name_suffix.size() == 0){
			if(this.store_ref_name.is_this){
				throw new Exception("Cannot be assigned to \"this\"");
			}else if(this.store_ref_name.ident!=null){
				if(cs.in_method_call){//関数呼び出し
					Field searched_field = cs.search_field(this.store_ref_name.ident, cs.call_field, cs);
					if(cs.search_called_method_arg(this.store_ref_name.ident)){
						f = cs.get_called_method_arg(this.store_ref_name.ident);
						ex = f.get_Expr(cs);
						f.class_object_expr = cs.call_expr;
					}else if(searched_field != null){
						f = searched_field;
						ex = cs.ctx.mkSelect((ArrayExpr) f.get_Expr(cs),cs.call_expr);
						f.class_object_expr = cs.call_expr;
					}else{
						throw new Exception(cs.call_field.type + " don't have " + this.store_ref_name.ident);
					}
				}else{
					Field searched_field = cs.search_field(this.store_ref_name.ident, null, cs);
					if(cs.search_variable(this.store_ref_name.ident)){
						f = cs.get_variable(this.store_ref_name.ident);
						ex = f.get_Expr(cs);
						f.class_object_expr = cs.this_field.get_Expr(cs);
					}else if(searched_field != null){
						f = searched_field;
						ex = cs.ctx.mkSelect((ArrayExpr) f.get_Expr(cs), cs.this_field.get_Expr(cs));
						f.class_object_expr = cs.this_field.get_Expr(cs);
					}else{
						throw new Exception(cs.this_field.type + " don't have " + this.store_ref_name.ident);
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
					Field searched_field = cs.search_field(store_ref_name.ident, cs.call_field, cs);
					if(cs.search_called_method_arg(store_ref_name.ident)){//これいらない？
						f = cs.get_called_method_arg(store_ref_name.ident);
						ex = f.get_Expr(cs);
					}else if(searched_field != null){
						f = searched_field;
						ex = cs.ctx.mkSelect((ArrayExpr) f.get_Expr(cs),cs.call_expr);
					}else{
						throw new Exception(cs.this_field.type + " don't have " + this.store_ref_name.ident);
					}
				}else{
					Field searched_field = cs.search_field(store_ref_name.ident, cs.this_field, cs);
					if(cs.search_variable(store_ref_name.ident)){
						f = cs.get_variable(store_ref_name.ident);
						ex = f.get_Expr(cs);
					}else if(searched_field != null){
						f = searched_field;
						ex = cs.ctx.mkSelect((ArrayExpr) f.get_Expr(cs),cs.this_field.get_Expr(cs));
					}else{
						throw new Exception(cs.this_field.type + " don't have " + this.store_ref_name.ident);
					}
				}
				
			}
			//suffixについて
			for(int i = 0; i < this.store_ref_name_suffix.size(); i++){
				store_ref_name_suffix ps = this.store_ref_name_suffix.get(i);
				if(ps.is_field){
					if(ps.ident!=null){
						Field searched_field = cs.search_field(ps.ident, f, cs);
						if(searched_field != null){
							f = searched_field;
							f.class_object_expr = ex;
							ex = cs.ctx.mkSelect((ArrayExpr)f.get_Expr(cs), ex);
						}else{
							throw new Exception(ps.ident + " dont exist");
						}
					}
					
				}else if(ps.is_index){
					IntExpr index = (IntExpr) ps.spec_array_ref_expr.spec_expression.check(cs).expr;
					ex = cs.ctx.mkSelect((ArrayExpr) ex, index);
					indexs.add(index);
				}
			}
			

		}
		
		return new Pair<Field, List<IntExpr>>(f, indexs);
	}

}

