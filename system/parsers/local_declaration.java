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
				
				
				
				//�z���⿌^�����S���ǂ���
				Expr rc_assign_field_expr = null;
				Expr rc_class_field_expr = null;
				if(rc.field!=null){
					rc_assign_field_expr = rc.field.get_full_Expr(new ArrayList<IntExpr>(rc.indexs.subList(0, rc.field.class_object_dims_sum())), cs);
					rc_class_field_expr = rc.field.class_object.get_full_Expr((ArrayList<IntExpr>) rc.indexs.clone(), cs);
				}
				cs.check_array_alias(v, v.get_Expr(cs), v.class_object.get_Expr(cs), indexs, rc.field, rc_assign_field_expr, rc_class_field_expr, rc.indexs);
				
				
				
				//1�����ȏ�̔z��Ƃ��ăG�C���A�X�����ꍇ�ɂ́A����ȍ~�z���������O��⿌^�̌��؂��s��Ȃ���΂Ȃ�Ȃ�
				if(v.hava_refinement_type() && v.have_index_access(cs) 
						&& rc.field != null && rc.field.hava_refinement_type() && rc.field.have_index_access(cs) && indexs.size()+1 <= v.dims_sum() ){
					if(cs.in_helper){
						if(v instanceof Variable)v.alias_in_helper_or_consutructor = cs.ctx.mkOr(v.alias_in_helper_or_consutructor, cs.get_pathcondition());
						if(rc.field instanceof Variable)rc.field.alias_in_helper_or_consutructor = cs.ctx.mkOr(rc.field.alias_in_helper_or_consutructor, cs.get_pathcondition());
					}else if(cs.in_constructor){
						if(!(rc.field instanceof Variable) && rc.field.class_object != null && rc.field.class_object.equals(cs.this_field, cs)){
							rc.field.alias_in_helper_or_consutructor = cs.ctx.mkOr(rc.field.alias_in_helper_or_consutructor, cs.get_pathcondition());
						}
					}
				}
				//2�����ȏ�̔z��Ƃ��ăG�C���A�X�����ꍇ�ɂ́A����ȍ~⿌^�𖞂����Ȃ���΂����Ȃ�
				if(v.hava_refinement_type() && v.have_index_access(cs) 
						&& rc.field != null && rc.field.hava_refinement_type() && rc.field.have_index_access(cs) && indexs.size()+2 <= v.dims_sum() ){
					if(cs.in_helper){
						if(v instanceof Variable)v.alias_2d_in_helper_or_consutructor = cs.ctx.mkOr(v.alias_2d_in_helper_or_consutructor, cs.get_pathcondition());
						if(rc.field instanceof Variable)rc.field.alias_2d_in_helper_or_consutructor = cs.ctx.mkOr(rc.field.alias_2d_in_helper_or_consutructor, cs.get_pathcondition());
						
						//⿌^�̌��ؒl�͑��������Ȃ̂ŁA�ǂ�������؂��Ă�����
						if(v.have_index_access(cs) && rc.field.have_index_access(cs)){
							v.assert_refinement(cs, cs.this_field.get_Expr(cs), new ArrayList<IntExpr>());
						}
					}else if(cs.in_constructor){
						if(!(rc.field instanceof Variable) && rc.field.class_object != null && rc.field.class_object.equals(cs.this_field, cs)){
							rc.field.alias_2d_in_helper_or_consutructor = cs.ctx.mkOr(rc.field.alias_2d_in_helper_or_consutructor, cs.get_pathcondition());
							
							//⿌^�̌��ؒl�͑��������Ȃ̂ŁA�ǂ�������؂��Ă�����
							if(v.have_index_access(cs) && rc.field.have_index_access(cs)){
								v.assert_refinement(cs, cs.this_field.get_Expr(cs), new ArrayList<IntExpr>());
							}
						}
					}
				}
				

				if(!cs.in_helper){
					//⿌^�̌���
					if(v.hava_refinement_type()){
						v.assert_refinement(cs, cs.this_field.get_Expr(cs), new ArrayList<IntExpr>());
					}
				}
			}
			if(v.hava_refinement_type() && cs.in_helper){
				Helper_assigned_field assigned_field = new Helper_assigned_field(cs.get_pathcondition(), v, cs.this_field.get_Expr(cs), new ArrayList<IntExpr>());
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

