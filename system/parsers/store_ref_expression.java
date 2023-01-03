package system.parsers;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;

import system.Array;
import system.Check_return;
import system.Check_status;
import system.Dummy_Field;
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
	
	public Check_return check(Check_status cs) throws Exception{
		Expr ex = null;
		Field f = null;
		Expr class_expr = cs.instance_expr;
		List<IntExpr> indexs = new ArrayList<IntExpr>();
		

		if(this.store_ref_name.is_this){
			if(this.store_ref_name_suffix.size() == 0){
				throw new Exception("Cannot be assigned to \"this\"");
			}
			if(cs.this_field.get_Expr(cs).equals(cs.instance_expr)){
				f = cs.this_field;
			}else{
				f = new Dummy_Field(cs.instance_class_name, cs.instance_expr);
			}
			
			ex = cs.instance_expr;
		}else if(this.store_ref_name.ident!=null){
			//ÉçÅ[ÉJÉãïœêî
			if(cs.in_method_call){//ä÷êîåƒÇ—èoÇµ
				if(cs.search_called_method_arg(this.store_ref_name.ident)){
					f = cs.get_called_method_arg(this.store_ref_name.ident);
					ex = f.get_Expr(cs);
					class_expr = cs.instance_expr;
				}
			}else{
				if(cs.search_variable(this.store_ref_name.ident)){
					f = cs.get_variable(this.store_ref_name.ident);
					ex = f.get_Expr(cs);
					class_expr = cs.this_field.get_Expr(cs);
				}
			}
			
			if(f==null){
				Field searched_field = cs.search_field(this.store_ref_name.ident, cs.instance_class_name, cs);
				if(searched_field != null){
					f = searched_field;
					ex = cs.ctx.mkSelect((ArrayExpr) f.get_Expr(cs), cs.this_field.get_Expr(cs));
					class_expr = cs.this_field.get_Expr(cs);
				}else{
					throw new Exception(cs.this_field.type + " don't have " + this.store_ref_name.ident);
				}
			}
			
		}
		//suffixÇ…Ç¬Ç¢Çƒ
		for(int i = 0; i < this.store_ref_name_suffix.size(); i++){
			store_ref_name_suffix ps = this.store_ref_name_suffix.get(i);
			if(ps.is_field){
				if(ps.ident!=null){
					Field searched_field = cs.search_field(ps.ident, f.type, cs);
					if(searched_field != null){
						f = searched_field;
						class_expr = ex;
						ex = cs.ctx.mkSelect((ArrayExpr)f.get_Expr(cs), ex);
					}else{
						throw new Exception(ps.ident + " don't exist");
					}
				}
				
				indexs = new ArrayList<IntExpr>();
				
			}else if(ps.is_index){
				IntExpr index = (IntExpr) ps.spec_array_ref_expr.spec_expression.check(cs).expr;
				indexs.add(index);
				
				Array array;
				if(indexs.size()<f.dims){
				    array = cs.array_arrayref;
				}else{
				    if(f.type.equals("int")){
				        array = cs.array_int;
				    }else if(f.type.equals("boolean")){
				        array = cs.array_boolean;
				    }else{
				        array = cs.array_ref;
				    }
				}
				ex = array.index_access_array(ex, index, cs);
			}
		}
		return new Check_return(ex, f, (ArrayList<IntExpr>) indexs, class_expr, f.type, f.dims - indexs.size());
	}

}

