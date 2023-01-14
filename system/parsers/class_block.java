package system.parsers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.microsoft.z3.*;

import system.Check_return;
import system.Check_status;
import system.Field;
import system.Model_Field;
import system.Option;
import system.Pair;
import system.Parser;
import system.Parser_status;
import system.Source;
import system.Summery;
import system.Variable;

public class class_block implements Parser<String>{
	
	public List<invariant> invariants;
	List<def_type_clause> def_type_clauses;
	List<method_decl> method_decls;
	List<variable_definition> variable_definitions;
	List<override_refinement_type_clause> override_refinement_type_clauses;
	List<represents_clause> represents_clauses;
	
	
	
	public String parse(Source s,Parser_status ps)throws Exception{
		invariants = new ArrayList<invariant>();
		def_type_clauses = new ArrayList<def_type_clause>();
		method_decls = new ArrayList<method_decl>();
		variable_definitions = new ArrayList<variable_definition>();
		override_refinement_type_clauses = new ArrayList<override_refinement_type_clause>();
		represents_clauses = new ArrayList<represents_clause>();

		new string("{").parse(s,ps);
		new newLines().parse(s,ps);
		Source s_backup = s.clone();
		try{
			while(true){
				s_backup = s.clone();
				field f = new field();
				Parser p = f.parse(s,ps);
				if(p instanceof invariant){
					invariants.add((invariant) p);
				}else if(p instanceof def_type_clause){
					def_type_clauses.add((def_type_clause) p);
				}else if(p instanceof method_decl){
					method_decls.add((method_decl) p);
				}else if(p instanceof variable_definition){
					variable_definitions.add((variable_definition) p);
				}else if(p instanceof override_refinement_type_clause){
					override_refinement_type_clauses.add((override_refinement_type_clause)p);
				}else if(p instanceof represents_clause){
					represents_clauses.add((represents_clause)p);
				}
			}
		}catch (Exception e){
			s.revert(s_backup);
		}
		new spaces().parse(s,ps);
		new cha('}').parse(s,ps);
		
		String ret = "";
		
		ret = ret + "{\n";
		ret = ret + "//def_types are \n";
		for(def_type_clause p : def_type_clauses){
			ret = ret + "<----------\n" + p.st + "\n------->\n";
		}
		ret = ret + "//variables are \n";
		for(variable_definition p : variable_definitions){
			ret = ret + "<----------\n" + p.st + "\n------->\n";
		}
		ret = ret + "//invariants are \n";
		for(invariant p : invariants){
			ret = ret + "<----------\n" + p.st + "\n------->\n";
		}
		ret = ret + "//methods are \n";
		for(method_decl p : method_decls){
			ret = ret + "<----------\n" + p.st + "\n------->\n";
		}
		ret = ret + "//represents are \n";
		for(represents_clause p : represents_clauses){
			ret = ret + "<----------\n" + p.st + "\n------->\n";
		}
		ret = ret + "}\n";
		
		
		
		return ret;
	}
	
	public void check(Option option, compilation_unit cu, class_declaration cd, Summery summery) throws Exception{
		//invariantとフィールドについて書く
		
		for(method_decl method :method_decls){
			if(!(method.modifiers != null && method.modifiers.is_model)){//modelメソッドは検証する必要はない
				Check_status cs = new Check_status(cu);
				cs.refinement_deep_limmit = option.refinement_deep_limmit;
				cs.invariant_refinement_type_deep_limmit = option.invariant_refinement_type_deep_limmit;
				if(option.timeout >= 0){
					Global.setParameter("timeout", Integer.toString(option.timeout));
				}
				
				
				modifiers m = new modifiers();
				m.is_final = true;
				Field this_field = new Variable(cs.Check_status_share.get_tmp_num(), "this", cd.class_name, 0, null, m, null, cs.ctx.mkBool(false));
				this_field.temp_num = 0;
				//csc.fields.add(this_field);
				cs.this_field = this_field;
				//初期化
				cs.instance_expr = this_field.get_Expr(cs);
				cs.instance_class_name = cd.class_name;
				
				//コンストラクタで新しく作ったrefは被らないことを表すための制約
				ArrayExpr alloc = cs.ctx.mkArrayConst("alloc_array", cs.ctx.mkUninterpretedSort("Ref"), cs.ctx.mkIntSort());
				BoolExpr constraint = cs.ctx.mkEq(cs.ctx.mkSelect(alloc, this_field.get_Expr(cs)), cs.ctx.mkInt(0));//thisは0で固定にする
				cs.add_constraint(constraint);
				
				//初期化
				//このクラスが持っているフィールドは予め追加しておく　コンストラクタでの検証のため
				class_block cb = cd.class_block;
				
				BoolExpr alias_2d = cs.ctx.mkBool(true);
				if(method.type_spec==null) alias_2d = cs.ctx.mkBool(false);//コンストラクタ

				for(variable_definition vd : cb.variable_definitions){
					Field f = null;
					if(vd.modifiers.is_model){
						f = cs.search_model_field(vd.variable_decls.ident, cs.this_field.type, cs);
					}else{
						//データグループのリストを作る
						ArrayList<Model_Field> data_groups = new ArrayList<Model_Field>();
						for(group_name gn : vd.group_names){
							String class_type = null;
							if(gn.is_super){
								class_type = cd.super_class.class_name;
							}else{
								class_type = cd.class_name;
							}
							
							String pre_type = cs.this_field.type;
							cs.this_field.type = class_type;
							data_groups.add(cs.search_model_field(gn.ident, cs.this_field.type, cs));
							cs.this_field.type = pre_type;
						}
						
						f = new Field(cs.Check_status_share.get_tmp_num(), vd.variable_decls.ident, vd.variable_decls.type_spec.type.type
								, vd.variable_decls.type_spec.dims, vd.variable_decls.type_spec.refinement_type_clause, vd.modifiers, cs.this_field.type, alias_2d, data_groups);
						
						if(f.modifiers!=null && f.modifiers.is_final) f.final_initialized = false;
						
						//initializerが付いていた場合
						if(vd.variable_decls.initializer!=null && (method.type_spec==null || (vd.modifiers != null && vd.modifiers.is_final))){
							Check_return init_Expr = vd.variable_decls.initializer.check(cs);
							Expr field_Expr = cs.ctx.mkSelect(f.get_Expr(cs), cs.this_field.get_Expr(cs));
							cs.add_constraint(cs.ctx.mkEq(field_Expr, init_Expr.expr));
							if(method.type_spec==null && (vd.modifiers != null && vd.modifiers.is_final)){
								f.final_initialized = true;
							}
						}
						
						
						
						
						
						
						cs.fields.add(f);
					}
					f.constructor_decl_field = true;
				}
	
				method.check(cs, summery);
			}
		}
	}
	
	
	
	
	
}
