package system.parsers;

import com.microsoft.z3.BoolExpr;

import system.Check_status;
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
				BoolExpr expr = cs.ctx.mkEq(cs.get_variable(this.variable_decls.ident).get_Expr_assign(cs), this.implies_expr.check(cs));
				cs.add_constraint(expr);
				cs.get_variable(this.variable_decls.ident).temp_num++;
				
				v.length = cs.right_side_status.length;
			}
			
			if(v.refinement_type_clause!=null){
				if(v.refinement_type_clause.refinement_type!=null){
					v.refinement_type_clause.refinement_type.assert_refinement(cs, v, v.get_Expr(cs));
				}else if(v.refinement_type_clause.ident!=null){
					refinement_type rt = cs.search_refinement_type(v.class_object.type, v.refinement_type_clause.ident);
					if(rt!=null){
						rt.assert_refinement(cs, v, v.get_Expr(cs));
					}else{
						throw new Exception("cant find refinement type " + v.refinement_type_clause.ident);
					}
				}
			}



			
			return v;
			
		}else{
			//System.out.println("this name is used");
			throw new Exception("this name is used");
		}
	}
	
}

