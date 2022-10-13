package system.parsers;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;

import system.Check_return;
import system.Check_status;
import system.Field;
import system.Pair;
import system.Parser;
import system.Parser_status;
import system.Source;
import system.Variable;

public class local_declaration implements Parser<String>{
	variable_decls variable_decls;
	implies_expr implies_expr;
	String st;
	public String parse(Source s,Parser_status ps)throws Exception{
		st = "";
		variable_decls vd = new variable_decls();
		st = st + vd.parse(s, ps);
		this.variable_decls = vd;
		Source s_backup = s.clone();
		try{
			String st2 = new spaces().parse(s, ps);
			st2 = st2 + new assignment_op().parse(s, ps);
			st2 = st2 + new spaces().parse(s, ps);
			implies_expr ie = new implies_expr();
			st2 = st2 + ie.parse(s, ps);
			this.implies_expr = ie;
			st = st + st2;
		}catch (Exception e){
			s.revert(s_backup);
		}
		return st;
	}
	
	public Variable check(Check_status cs) throws Exception{
		System.out.println("/////// " + st);
		if(cs.search_variable(this.variable_decls.ident)==false){
			Variable v = cs.add_variable(this.variable_decls.ident, this.variable_decls.type_spec.type.type, this.variable_decls.type_spec.dims, this.variable_decls.type_spec.refinement_type_clause, null);
			if(this.implies_expr != null){
				ArrayList<IntExpr> indexs = new ArrayList<IntExpr>();
				
				Check_return rc = this.implies_expr.check(cs);
				
				BoolExpr expr = cs.ctx.mkEq(cs.get_variable(this.variable_decls.ident).get_Expr_assign(cs), rc.expr);
				cs.add_constraint(expr);
				cs.get_variable(this.variable_decls.ident).temp_num++;
				
				
				
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
						rc.field.refinement_type_clause.equal_predicate(rc.indexs, rc_assign_field_expr, rc.field.class_object, rc.field.class_object.get_full_Expr(rc.indexs, cs), v.refinement_type_clause, indexs, v.get_Expr(cs), v.class_object, v.class_object.get_Expr(cs), cs);
					}else if(v.dims>0 && v.dims_sum()!=indexs.size() && v instanceof Variable){//ローカル変数
						
						if(cs.in_loop) throw new Exception("can not alias with refined array　in loop");//ループの中ではエイリアスできない
						
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
						
						if(cs.in_loop) throw new Exception("can not alias with refined array　in loop");//ループの中ではエイリアスできない
						
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
					}
				}
				
				
				
				//篩型
				if(v.refinement_type_clause!=null){
					if(v.refinement_type_clause.refinement_type!=null){
						v.refinement_type_clause.refinement_type.assert_refinement(cs, v, v.get_Expr(cs), cs.this_field, cs.this_field.get_Expr(cs), new ArrayList<IntExpr>());
					}else if(v.refinement_type_clause.ident!=null){
						refinement_type rt = cs.search_refinement_type(v.class_object.type, v.refinement_type_clause.ident);
						if(rt!=null){
							rt.assert_refinement(cs, v, v.get_Expr(cs), cs.this_field, cs.this_field.get_Expr(cs), new ArrayList<IntExpr>());
						}else{
							throw new Exception("can't find refinement type " + v.refinement_type_clause.ident);
						}
					}
				}
			}

			
			return v;
			
		}else{
			//System.out.println("this name is used");
			throw new Exception("this name is used");
		}
	}

	
	public Variable loop_assign(Pair<List<Pair<Field,List<List<IntExpr>>>>,Boolean>assigned_fields, Check_status cs) throws Exception{
		if(cs.search_variable(this.variable_decls.ident)==false){
			Variable v = cs.add_variable(this.variable_decls.ident, this.variable_decls.type_spec.type.type, this.variable_decls.type_spec.dims, this.variable_decls.type_spec.refinement_type_clause, null);
			if(this.implies_expr != null){
				ArrayList<IntExpr> indexs = new ArrayList<IntExpr>();
				
				Check_return rc = this.implies_expr.loop_assign(assigned_fields, cs);
				
				cs.get_variable(this.variable_decls.ident).temp_num++;
			}
			return v;
		}else{
			//System.out.println("this name is used");
			throw new Exception("this name is used");
		}
	}
}

