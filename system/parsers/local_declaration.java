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
			Variable v = cs.add_variable(this.variable_decls.ident, this.variable_decls.type_spec.type.type, this.variable_decls.type_spec.dims, this.variable_decls.type_spec.refinement_type_clause, null, cs.ctx.mkBool(false));
			if(this.implies_expr != null){
				ArrayList<IntExpr> indexs = new ArrayList<IntExpr>();
				
				Check_return rc = this.implies_expr.check(cs);
				
				BoolExpr expr = cs.ctx.mkEq(cs.get_variable(this.variable_decls.ident).get_Expr_assign(cs), rc.expr);
				cs.add_constraint(expr);
				cs.get_variable(this.variable_decls.ident).temp_num++;
				
				
				
				//配列の篩型が安全かどうか
				Expr rc_assign_field_expr = null;
				Expr rc_class_field_expr = null;
				if(rc.field!=null){
					rc_assign_field_expr = rc.field.get_full_Expr(new ArrayList<IntExpr>(rc.indexs.subList(0, rc.field.class_object_dims_sum())), cs);
					rc_class_field_expr = rc.field.class_object.get_full_Expr((ArrayList<IntExpr>) rc.indexs.clone(), cs);
				}
				cs.check_array_alias(v, v.get_Expr(cs), v.class_object.get_Expr(cs), indexs, rc.field, rc_assign_field_expr, rc_class_field_expr, rc.indexs);
				
				
				//2次元以上の配列としてエイリアスした場合には、それ以降篩型を満たさなければいけない
				if(cs.in_helper && v.refinement_type_clause!=null && v.dims_sum() >= 2 && rc.field != null && !rc.field.new_array){
					if(v instanceof Variable)v.alias_2d = cs.ctx.mkOr(cs.get_pathcondition());
					if(rc.field instanceof Variable)rc.field.alias_2d = cs.ctx.mkOr(rc.field.alias_2d, cs.get_pathcondition());
					//篩型の検証
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
				}else if(!cs.in_helper){
					//篩型の検証
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
			}

			
			return v;
			
		}else{
			//System.out.println("this name is used");
			throw new Exception("this name is used");
		}
	}

	
	public Variable loop_assign(Pair<List<Pair<Field,List<List<IntExpr>>>>,Boolean>assigned_fields, Check_status cs) throws Exception{
		if(cs.search_variable(this.variable_decls.ident)==false){
			Variable v = cs.add_variable(this.variable_decls.ident, this.variable_decls.type_spec.type.type, this.variable_decls.type_spec.dims, this.variable_decls.type_spec.refinement_type_clause, null, cs.ctx.mkBool(false));
			v.tmp_plus(cs);
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

