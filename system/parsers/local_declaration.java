package system.parsers;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;

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

public class local_declaration implements Parser<String>{
	boolean is_final;
	variable_decls variable_decls;
	String st;
	public String parse(Source s,Parser_status ps)throws Exception{
		st = "";
		Source s_backup = s.clone();
		try{
			st = st + new string("final").parse(s, ps);
			st = st + new spaces().parse(s, ps);
			this.is_final = true;
		}catch (Exception e){
			s.revert(s_backup);
		}
		variable_decls vd = new variable_decls();
		st = st + vd.parse(s, ps);
		this.variable_decls = vd;
		return st;
	}
	
	public Variable check(Check_status cs) throws Exception{
		System.out.println("/////// " + st);
		if(cs.search_variable(this.variable_decls.ident)==false){
			modifiers m = new modifiers();
			m.is_final = this.is_final;
			Variable v = cs.add_variable(this.variable_decls.ident, this.variable_decls.type_spec.type.type, this.variable_decls.type_spec.dims, this.variable_decls.type_spec.refinement_type_clause, m, cs.ctx.mkBool(false));
			
			if(v.hava_refinement_type()){//篩型の中で使えるローカル変数
				if(v.refinement_type_clause.refinement_type!=null){
					v.refinement_type_clause.refinement_type.defined_variables.addAll(cs.variables);
				}
			}
			
			if(this.variable_decls.initializer != null){
				
				boolean pre_is_rightside = cs.is_rightside;
				cs.is_rightside = true;
				Check_return rc = this.variable_decls.initializer.check(cs);
				cs.is_rightside = pre_is_rightside;
				
				
				BoolExpr expr = cs.ctx.mkEq(v.get_Expr_assign(cs), rc.expr);
				cs.add_constraint(expr);
				cs.get_variable(this.variable_decls.ident).temp_num++;

				
				//配列の篩型が安全かどうか
				cs.check_array_alias(v, null, new ArrayList<IntExpr>(), rc.field, rc.class_expr, rc.indexs);
				
				
				//1次元以上の配列としてエイリアスした場合には、それ以降配列を代入する前に篩型の検証を行わなければならない
				if(v.hava_refinement_type() && v.have_index_access(cs) 
						&& rc.field != null && rc.field.hava_refinement_type() && rc.field.have_index_access(cs) 
						&& v.dims >= 1){
					
					if(cs.in_helper || cs.in_no_refinement_type){
						if(v instanceof Variable){
							v.alias_1d_in_helper = cs.ctx.mkOr(v.alias_1d_in_helper, cs.get_pathcondition());
						}
						if(rc.field instanceof Variable
								|| (cs.in_constructor && !(rc.field instanceof Variable) && cs.this_field.get_Expr(cs).equals(rc.class_expr) && rc.field.constructor_decl_field)){
							rc.field.alias_1d_in_helper = cs.ctx.mkOr(rc.field.alias_1d_in_helper, cs.get_pathcondition());
						}
					}else if(cs.in_constructor){
						if(!(rc.field instanceof Variable) && cs.this_field.get_Expr(cs).equals(rc.class_expr) && rc.field.constructor_decl_field){
							rc.field.alias_in_consutructor_or_2d_in_helper = cs.ctx.mkOr(rc.field.alias_in_consutructor_or_2d_in_helper, cs.get_pathcondition());
						}
					}
				}
				//2次元以上の配列としてエイリアスした場合には、それ以降篩型を満たさなければいけない
				if(v.hava_refinement_type() && v.have_index_access(cs) 
						&& rc.field != null && rc.field.hava_refinement_type() && rc.field.have_index_access(cs) 
						&& v.dims >= 2){
					
					if(cs.in_helper || cs.in_no_refinement_type){
						if(v instanceof Variable){
							v.alias_in_consutructor_or_2d_in_helper = cs.ctx.mkOr(v.alias_in_consutructor_or_2d_in_helper, cs.get_pathcondition());
						}
						if(rc.field instanceof Variable
								|| (cs.in_constructor && !(rc.field instanceof Variable) && cs.this_field.get_Expr(cs).equals(rc.class_expr) && rc.field.constructor_decl_field)){
							rc.field.alias_in_consutructor_or_2d_in_helper = cs.ctx.mkOr(rc.field.alias_in_consutructor_or_2d_in_helper, cs.get_pathcondition());
						}
					}
				}
				

				
				//篩型の検証
				if(v.hava_refinement_type()){
					if(cs.in_helper || cs.in_no_refinement_type){
						if(v.dims >= 2 && v.have_index_access(cs)){//2次元以上の配列としてエイリアスしている場合には、篩型の検証をしないといけない
							cs.solver.push();
							cs.add_constraint(v.alias_in_consutructor_or_2d_in_helper);

							v.assert_refinement(cs, cs.instance_expr);
							cs.solver.pop();
						}
						
					}else{
						v.assert_refinement(cs, cs.instance_expr);
					}
					Helper_assigned_field assigned_field = new Helper_assigned_field(cs.get_pathcondition(), v, cs.this_field.get_Expr(cs));
					cs.helper_assigned_fields.add(assigned_field);
				}
				//配列がエイリアスしたときに、右辺の配列の篩型の検証 　　初めてのエイリアスである可能性であるときだけ検証
				if(cs.in_helper || cs.in_no_refinement_type){
					if(v.hava_refinement_type() && v.have_index_access(cs) 
							&& rc.field != null && rc.field.hava_refinement_type() && rc.field.have_index_access(cs) 
							&& v.dims >= 2){
						rc.field.assert_refinement(cs, rc.class_expr);
					}
				}else if(cs.in_constructor && rc.field != null && cs.this_field.get_Expr(cs).equals(rc.class_expr) && rc.field.constructor_decl_field){
					if(v.hava_refinement_type() && v.have_index_access(cs) 
							&& rc.field != null && rc.field.hava_refinement_type() && rc.field.have_index_access(cs) 
							&& v.dims >= 1){
						rc.field.assert_refinement(cs, rc.class_expr);
					}
				}
			}
			
			return v;
			
		}else{
			//System.out.println("this name is used");
			throw new Exception("this name is used");
		}
	}

	
	public Variable loop_assign(Pair<List<F_Assign>,BoolExpr>assigned_fields, Check_status cs) throws Exception{
		if(cs.search_variable(this.variable_decls.ident)==false){
			Variable v = cs.add_variable(this.variable_decls.ident, this.variable_decls.type_spec.type.type, this.variable_decls.type_spec.dims, this.variable_decls.type_spec.refinement_type_clause, null, cs.ctx.mkBool(false));
			v.tmp_plus(cs);
			if(this.variable_decls.initializer != null){
				ArrayList<IntExpr> indexs = new ArrayList<IntExpr>();
				
				Check_return rc = this.variable_decls.initializer.loop_assign(assigned_fields, cs);
				
				cs.get_variable(this.variable_decls.ident).temp_num++;
			}
			return v;
		}else{
			//System.out.println("this name is used");
			throw new Exception("this name is used");
		}
	}
}

