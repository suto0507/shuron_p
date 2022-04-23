package system.parsers;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;

import system.Check_status;
import system.Field;
import system.Parser;
import system.Parser_status;
import system.Source;

public class primary_expr implements Parser<String>{
	java_literal java_literal;
	expression bracket_expression;
	new_expr new_expr;
	jml_primary jml_primary;
	String ident;
	boolean is_this;
	
	primary_expr(){
		this.is_this = false;
	}
	
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		Source s_backup = s.clone();
		try{
			java_literal jl = new java_literal();
			st = st + jl.parse(s, ps);
			this.java_literal = jl;
		}catch (Exception e){
			s.revert(s_backup);
			s_backup = s.clone();
			try{
				new_expr ne = new new_expr();
				st = st + ne.parse(s, ps);
				this.new_expr = ne;
			}catch (Exception e2){
				s.revert(s_backup);
				s_backup = s.clone();
				try{
					jml_primary jp = new jml_primary();
					st = st + jp.parse(s, ps);
					this.jml_primary = jp;
				}catch (Exception e3){
					s.revert(s_backup);
					s_backup = s.clone();
					try{
						st = st + new string("(").parse(s, ps);
						st = st + new spaces().parse(s, ps);
						expression ex = new expression();
						st = st + ex.parse(s, ps);
						st = st + new spaces().parse(s, ps);
						st = st + new string(")").parse(s, ps);
						this.bracket_expression = ex;
					}catch (Exception e4){
						s.revert(s_backup);
						s_backup = s.clone();
						try{
							this.ident = new ident().parse(s, ps);
							st = st + this.ident;
						}catch (Exception e5){
							s.revert(s_backup);
							new string("this").parse(s, ps);
							this.is_this = true;
							st = st + "this";
						}
					}
				}
			}
		}
		
		return st;
	}
	
	public Expr check(Check_status cs) throws Exception{

		Field f = null;
		Expr ex = null;
		if(this.is_this){
			if(cs.in_constructor){//コンストラクタでのthisの参照
				cs.constructor_refinement_check();
			}
			ex = cs.this_field.get_Expr(cs);
			//関数呼び出し
			if(cs.in_method_call){
				f = cs.call_field;
				ex = cs.call_expr;
			}else if(cs.in_refinement_predicate){
				f = cs.refined_class_Field;
				ex = cs.refined_class_Expr;
			}
			return ex;
		}else if(this.ident!=null){
			if(cs.in_refinement_predicate==true){
				if(this.ident.equals(cs.refinement_type_value)){
					return cs.refined_Expr;
				}
			}
			if(cs.in_method_call==true){//関数呼び出し
				if(cs.search_called_method_arg(this.ident)){
					f = cs.get_called_method_arg(this.ident);
					ex = f.get_Expr(cs);
				}else if(cs.search_field(this.ident, cs.call_field, cs.call_field_index, cs)){
					f = cs.get_field(this.ident, cs.call_field, cs.call_field_index, cs);
					ex = cs.ctx.mkSelect((ArrayExpr) f.get_Expr(cs),cs.call_expr);
				}else{
					Field f_tmp = cs.add_field(this.ident, cs.call_field, cs.call_field_index);
					if(f_tmp != null){
						f = f_tmp;
						ex = cs.ctx.mkSelect((ArrayExpr)f.get_Expr(cs), cs.call_expr);
					}else{
						throw new Exception(cs.call_field.type + " don't have " + this.ident);
					}
				}
			}else if(cs.in_refinement_predicate==true){//篩型
				if((cs.refined_class_Field==null||cs.refined_class_Field.equals(cs.this_field, cs))&&cs.search_variable(this.ident)){
					f = cs.get_variable(this.ident);
					ex = f.get_Expr(cs);
				}else if(cs.search_field(this.ident, cs.refined_class_Field, cs.refined_class_Field_index ,cs)){
					f = cs.get_field(this.ident, cs.refined_class_Field, cs.refined_class_Field_index ,cs);
					ex = cs.ctx.mkSelect((ArrayExpr) f.get_Expr(cs),cs.refined_class_Expr);
				}else{
					Field f_tmp = cs.add_field(this.ident, cs.refined_class_Field, cs.refined_class_Field_index);
					if(f_tmp != null){
						f = f_tmp;
						ex = cs.ctx.mkSelect((ArrayExpr)f.get_Expr(cs), cs.refined_class_Expr);
					}else{
						throw new Exception(cs.refined_class_Field.type + " don't have " + this.ident);
					}
				}
			}else{
				if(cs.search_variable(this.ident)){
					f = cs.get_variable(this.ident);
					ex = f.get_Expr(cs);
				}else if(cs.search_field(this.ident, cs.this_field, null, cs)){
					f = cs.get_field(this.ident, cs.this_field, null, cs);
					ex = cs.ctx.mkSelect((ArrayExpr) f.get_Expr(cs), cs.this_field.get_Expr(cs));
				}
			}
			
			if(ex == null){
				throw new Exception("cant find " + this.ident);
			}
			if(cs.in_refinement_predicate==true){//篩型の中ではfinalである必要がある
				if(f.modifiers.is_final==false){
					throw new Exception("can use only final variable in refinement type");
				}
			}
			
			if(f.refinement_type_clause!=null){//篩型
				System.out.println(f.field_name + " has refinement type");
				//バックアップ
				Field refined_Field = cs.refined_Field;
				Expr refined_Expr = cs.refined_Expr;
				String refinement_type_value = cs.refinement_type_value;
				boolean in_refinement_predicate = cs.in_refinement_predicate;
				Expr refined_class_Expr = cs.refined_class_Expr;
				Field refined_class_Field = cs.refined_class_Field;
				IntExpr refined_class_Field_index = cs.refined_class_Field_index;
				
				//篩型の処理のための事前準備
				if(cs.in_method_call){
					cs.refined_class_Expr = cs.call_expr;
					cs.refined_class_Field = cs.call_field;
					cs.refined_class_Field_index = cs.call_field_index;
				}
				cs.refinement_deep++;
				if(cs.refinement_deep <= cs.refinement_deep_limmit){
					if(f.refinement_type_clause.refinement_type!=null){
						f.refinement_type_clause.refinement_type.add_refinement_constraint(cs, f, ex);
					}else if(f.refinement_type_clause.ident!=null){
						refinement_type rt = cs.search_refinement_type(f.refinement_type_clause.ident, f.class_object.type);
						if(rt!=null){
							rt.add_refinement_constraint(cs, f, ex);
						}else{
							throw new Exception("cant find refinement type " + f.refinement_type_clause.ident);
						}
					}
				}else{
					System.out.println("The depth of refinement type's verification has reached its limit.");
				}
				
				cs.refined_Expr = refined_Expr;
				cs.refined_Field = refined_Field;
				cs.refinement_type_value = refinement_type_value;
				cs.in_refinement_predicate = in_refinement_predicate;
				
				cs.refined_class_Expr = refined_class_Expr;
				cs.refined_class_Field = refined_class_Field;
				cs.refined_class_Field_index = refined_class_Field_index;
				cs.refinement_deep--;
				
			}
			
			//JML節での使えない可視性の確認
			if(cs.ban_default_visibility){
				if(f.modifiers!=null&&f.modifiers.is_privte==false){
					throw new Exception("can not use default visibility variable");
				}
			}
			if(cs.ban_private_visibility){
				if(f.modifiers!=null&&f.modifiers.is_privte==true){
					throw new Exception("can not use private visibility variable");
				}
			}
			
			return ex;
		}else if(this.bracket_expression!=null){
			return this.bracket_expression.check(cs);
		}else if(this.java_literal!=null){
			return this.java_literal.check(cs);
		}else if(this.jml_primary!=null){
			if(cs.in_method_call){
				if(this.jml_primary.is_result){
					f = cs.result;
					ex = f.get_Expr(cs);
					return ex;
				}else{
					return this.jml_primary.old_expression.spec_expression.check(cs.old_status);
				}
			}else{
				if(this.jml_primary.is_result){
					f = cs.return_v;
					ex = cs.return_expr;
					return ex;
				}else{
					return this.jml_primary.old_expression.spec_expression.check(cs.this_old_status);
				}
			}
		}else if(this.new_expr!=null){
			return this.new_expr.check(cs).get_Expr(cs);
		}else{
			return null;
		}
	}
}
