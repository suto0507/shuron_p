package system.parsers;

import java.util.ArrayList;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.Status;

import system.Check_status;
import system.Field;
import system.Parser;
import system.Parser_status;
import system.Source;
import system.Variable;

public class new_expr implements Parser<String>{
	type type;
	new_suffix new_suffix;
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		st = st + new string("new").parse(s, ps);
		st = st + new spaces().parse(s, ps);
		this.type = new type();
		st = st + this.type.parse(s, ps);
		st = st + new spaces().parse(s, ps);
		this.new_suffix = new new_suffix();
		st = st + this.new_suffix.parse(s, ps);
		if(ps.in_jml){
			throw new Exception("cant use new in jml");
		}	
		return st;
	}
	
	public Variable check(Check_status cs) throws Exception{
		if(this.new_suffix.expression!=null){
			IntExpr length = (IntExpr) this.new_suffix.expression.check(cs);
			Variable ret = null;
			cs.right_side_status.length = length;
			
			ret = new Variable(cs.Check_status_share.get_tmp_num(), "new_" + this.type.type + "_array_tmp", this.type.type, 1, null, new modifiers(), cs.this_field);

			ret.temp_num++;
			return ret;
		}else if(this.new_suffix.expression_list!=null){
			//String ident = 
			//Field f;
			//Expr ex
			new_suffix ps = this.new_suffix;
			//IntExpr f_index
			
			
			class_declaration cd = cs.Check_status_share.compilation_unit.search_class(this.type.type);
			if(cd == null){
				throw new Exception("can't find class " + this.type.type);
			}
			method_decl md = cs.Check_status_share.compilation_unit.search_method(this.type.type, this.type.type);
			if(md == null){
				throw new Exception("can't find method " + this.type.type);
			}
			cs.in_method_call = true;
			//à¯êîÇÃèàóù
			cs.called_method_args = new ArrayList<Variable>();
			if(md.formals.param_declarations.size()!=ps.expression_list.expressions.size()){
				throw new Exception("wrong number of arguments");
			}

			
			//ï‘ÇËíl
			Variable result = new Variable(cs.Check_status_share.get_tmp_num(), "class_" + this.type.type + "_constructor_tmp", this.type.type, 0, null, md.modifiers, cs.this_field);
			result.temp_num++;
			cs.result = result;

			cs.call_expr = result.get_Expr(cs);
			cs.call_field = result;
			cs.call_field_index = null;
			
			for(int j = 0; j < md.formals.param_declarations.size(); j++){
				param_declaration pd = md.formals.param_declarations.get(j);
				modifiers m = new modifiers();
				m.is_final = pd.is_final;
				Variable v = new Variable(cs.Check_status_share.get_tmp_num(), pd.ident, pd.type_spec.type.type, pd.type_spec.dims, pd.type_spec.refinement_type_clause, m, result);
				cs.called_method_args.add(v);
				v.temp_num = 0;
				//à¯êîÇ…ílÇïRÇ√ÇØÇÈ
				cs.add_constraint(cs.ctx.mkEq(v.get_Expr(cs), ps.expression_list.expressions.get(j).check(cs)));
				//‚øå^
				if(v.refinement_type_clause!=null){
					if(v.refinement_type_clause.refinement_type!=null){
						v.refinement_type_clause.refinement_type.assert_refinement(cs, v, v.get_Expr(cs));
					}else{
						refinement_type rt = cs.Check_status_share.compilation_unit.search_refinement_type(v.refinement_type_clause.ident, v.class_object.type);
						if(rt!=null){
							rt.assert_refinement(cs, v, v.get_Expr(cs));
						}else{
							throw new Exception("cant find refinement type " + v.refinement_type_clause.ident);
						}
					}
				}
			}
			//éñëOèåè
			BoolExpr pre_invariant_expr = null;
			/*Ç±ÇÍÇÕÇ¢ÇÁÇÒ
			if(cd.class_block.invariants!=null&&cd.class_block.invariants.size()>0){
				for(invariant inv : cd.class_block.invariants){
					if(pre_invariant_expr == null){
						pre_invariant_expr = (BoolExpr) inv.check(cs);
					}else{
						pre_invariant_expr = cs.ctx.mkAnd(pre_invariant_expr, (BoolExpr)inv.check(cs));
					}
				}
				cs.assert_constraint(pre_invariant_expr);
			}
			*/
			BoolExpr require_expr = null;
			if(md.requires!=null&&md.requires.size()>0){
				for(requires_clause re : md.requires){
					if(require_expr == null){
						require_expr = (BoolExpr) re.check(cs);
					}else{
						require_expr = cs.ctx.mkAnd(require_expr, (BoolExpr)re.check(cs));
					}
				}
				cs.assert_constraint(require_expr);
			}
			
			//old data
			Check_status csc = cs.clone();
			csc.clone_list();
			cs.old_status = csc;
			
			//assign
			if(md.assignables!=null&&md.assignables.size()>0){
				for(assignable_clause assigns : md.assignables){
					for(store_ref_expression sre : assigns.store_ref_list.store_ref_expressions){
						Field f_tmp = sre.check(cs);
						
						//assignÇµÇƒÇ¢Ç¢Ç©
						if(f_tmp.is_this_field()){
							if(cs.assignables!=null){
								boolean can_assign = false;
								for(Field assign_field : cs.assignables){
									if(assign_field.equals(f_tmp, cs)){
										if(f_tmp.index!=null){
											BoolExpr ex_a = cs.ctx.mkFalse();
											for(IntExpr assign_index : assign_field.assinable_indexs){
												ex_a = cs.ctx.mkOr(ex_a, cs.ctx.mkEq(f_tmp.index, assign_index));
											}
											if(cs.pathcondition!=null){
												ex_a = cs.ctx.mkAnd(cs.pathcondition, cs.ctx.mkNot(ex_a));
											}else{
												ex_a = cs.ctx.mkNot(ex_a);
											}
											cs.solver.push();
											cs.solver.add(ex_a);
											if(cs.solver.check() != Status.SATISFIABLE) {
												can_assign = true;
											}
											cs.solver.pop();
										}else{
											can_assign = true;
										}
									}
								}
								if(can_assign == false){
									throw new Exception("Cannot be assigned to" + f_tmp.field_name);
								}
							}
						}
						
						System.out.println("assign " + f_tmp.field_name);
						//Å™nullÇæÇ¡ÇΩÇËÇ∑ÇÈÇÃÇ©ÅH
						if(f_tmp.index!=null){
							Expr tmp_Expr;
							if(f_tmp.type.equals("int")){
								tmp_Expr = cs.ctx.mkIntConst("tmpInt" + cs.Check_status_share.tmp_num);
							}else if(f_tmp.type.equals("boolean")){
								tmp_Expr = cs.ctx.mkBoolConst("tmpBool" + cs.Check_status_share.tmp_num);
							}else{
								tmp_Expr = cs.ctx.mkConst("tmpRef" + cs.Check_status_share.tmp_num,  cs.ctx.mkUninterpretedSort("Ref"));
							}
							BoolExpr expr = cs.ctx.mkEq(f_tmp.assign_Expr, cs.ctx.mkStore((ArrayExpr) f_tmp.assign_now_array_Expr, f_tmp.index, tmp_Expr));
							cs.add_constraint(expr);
							f_tmp.temp_num++;
							//èâä˙âª
							f_tmp.index = null;
						}else{
							f_tmp.temp_num++;
						}
					}
				}
			}else{
				//assignÇ™èëÇ©ÇÍÇƒÇ¢Ç»Ç¢ä÷êî
				for(Variable v_a : cs.variables){
					v_a.temp_num++;
				}
				for(Field f_a : cs.fields){
					f_a.temp_num++;
				}
			}
			
			
			
			//éñå„èåè
			BoolExpr post_invariant_expr = null;
			if(cd.class_block.invariants!=null&&cd.class_block.invariants.size()>0){
				for(invariant inv : cd.class_block.invariants){
					if(post_invariant_expr == null){
						post_invariant_expr = (BoolExpr) inv.check(cs);
					}else{
						post_invariant_expr = cs.ctx.mkAnd(pre_invariant_expr, (BoolExpr)inv.check(cs));
					}
				}
				cs.add_constraint(post_invariant_expr);
			}
			BoolExpr ensures_expr = null;
			if(md.ensures!=null&&md.ensures.size()>0){
				for(ensures_clause ec : md.ensures){
					if(ensures_expr == null){
						ensures_expr = (BoolExpr) ec.check(cs);
					}else{
						ensures_expr = cs.ctx.mkAnd(ensures_expr, (BoolExpr)ec.check(cs));
					}
				}
				cs.add_constraint(ensures_expr);
			}
			cs.in_method_call = false;
			
			return result;
		}else{
			throw new Exception("wrong new clause");
		}
	}
	

}

