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
	variable_decls variable_decls;
	String st;
	public String parse(Source s,Parser_status ps)throws Exception{
		st = "";
		variable_decls vd = new variable_decls();
		st = st + vd.parse(s, ps);
		this.variable_decls = vd;
		return st;
	}
	
	public Variable check(Check_status cs) throws Exception{
		System.out.println("/////// " + st);
		if(cs.search_variable(this.variable_decls.ident)==false){
			Variable v = cs.add_variable(this.variable_decls.ident, this.variable_decls.type_spec.type.type, this.variable_decls.type_spec.dims, this.variable_decls.type_spec.refinement_type_clause, null, cs.ctx.mkBool(false));
			if(this.variable_decls.initializer != null){
				ArrayList<IntExpr> indexs = new ArrayList<IntExpr>();
				
				Check_return rc = this.variable_decls.initializer.check(cs);
				
				BoolExpr expr = cs.ctx.mkEq(cs.get_variable(this.variable_decls.ident).get_Expr_assign(cs), rc.expr);
				cs.add_constraint(expr);
				cs.get_variable(this.variable_decls.ident).temp_num++;

				
				//配列の篩型が安全かどうか
				cs.check_array_alias(v, null, new ArrayList<IntExpr>(), rc.field, rc.class_expr, rc.indexs);
				
				
				//1次元以上の配列としてエイリアスした場合には、それ以降配列を代入する前に篩型の検証を行わなければならない
				if(v.hava_refinement_type() && v.have_index_access(cs) 
						&& rc.field != null && rc.field.hava_refinement_type() && rc.field.have_index_access(cs) 
						&& v.dims >= 1){
					
					if(cs.in_helper){
						if(v instanceof Variable){
							v.alias_1d_in_helper = cs.ctx.mkOr(v.alias_1d_in_helper, cs.get_pathcondition());
						}
						if(rc.field instanceof Variable){
							rc.field.alias_1d_in_helper = cs.ctx.mkOr(rc.field.alias_1d_in_helper, cs.get_pathcondition());
						}
					}else if(cs.in_constructor){
						if(!(rc.field instanceof Variable) && cs.this_field.get_Expr(cs).equals(rc.class_expr)){
							rc.field.alias_in_consutructor_or_2d_in_helper = cs.ctx.mkOr(rc.field.alias_in_consutructor_or_2d_in_helper, cs.get_pathcondition());
						}
					}
				}
				//2次元以上の配列としてエイリアスした場合には、それ以降篩型を満たさなければいけない
				if(v.hava_refinement_type() && v.have_index_access(cs) 
						&& rc.field != null && rc.field.hava_refinement_type() && rc.field.have_index_access(cs) 
						&& v.dims >= 2){
					
					if(cs.in_helper){
						if(v instanceof Variable){
							v.alias_in_consutructor_or_2d_in_helper = cs.ctx.mkOr(v.alias_in_consutructor_or_2d_in_helper, cs.get_pathcondition());
						}
						if(rc.field instanceof Variable){
							rc.field.alias_in_consutructor_or_2d_in_helper = cs.ctx.mkOr(rc.field.alias_in_consutructor_or_2d_in_helper, cs.get_pathcondition());
						}
					}
				}
				

				
				//篩型の検証
				if(v.hava_refinement_type()){
					if(cs.in_helper){
						if(v.dims >= 2 && v.have_index_access(cs)){//2次元以上の配列としてエイリアスしている場合には、篩型の検証をしないといけない
							cs.solver.push();
							cs.add_constraint(v.alias_in_consutructor_or_2d_in_helper);

							v.assert_refinement(cs, null);
							cs.solver.pop();
						}
						
					}else{
						v.assert_refinement(cs, null);
					}
				}
				//配列がエイリアスしたときに、右辺の配列の篩型の検証 　　初めてのエイリアスである可能性であるときだけ検証
				if(cs.in_helper){
					if(v.hava_refinement_type() && v.have_index_access(cs) 
							&& rc.field != null && rc.field.hava_refinement_type() && rc.field.have_index_access(cs) 
							&& v.dims >= 2){
						rc.field.assert_refinement(cs, rc.class_expr);
					}
				}else if(cs.in_constructor && cs.this_field.get_Expr(cs).equals(rc.class_expr)){
					if(v.hava_refinement_type() && v.have_index_access(cs) 
							&& rc.field != null && rc.field.hava_refinement_type() && rc.field.have_index_access(cs) 
							&& v.dims >= 1){
						rc.field.assert_refinement(cs, rc.class_expr);
					}
				}
			}
			if(v.hava_refinement_type() && cs.in_helper){
				Helper_assigned_field assigned_field = new Helper_assigned_field(cs.get_pathcondition(), v, cs.this_field.get_Expr(cs));
				cs.helper_assigned_fields.add(assigned_field);
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

