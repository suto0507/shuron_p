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
import system.parsers.spec_case_seq.F_Assign;

//compund_statementで空白、改行をやっている
	public class statement implements Parser<String>{
		boolean is_expression, is_return, is_if;
		compound_statement compound_statement;
		local_declaration local_declaration;
		expression expression;
		statement true_statement, false_statement;
		assert_statement assert_statement;
		possibly_annotated_loop possibly_annotated_loop;
		def_type_clause def_type_clause;
		
		statement(){
			this.is_expression = false;
			this.is_if = false;
			this.is_return = false;
		}
		
		public String parse(Source s,Parser_status ps)throws Exception{
			String st = ""; 
			Source s_backup = s.clone();
			try{
				//compound_statement
				compound_statement cs = new compound_statement();
				st = st + cs.parse(s, ps);
				this.compound_statement = cs;
			}catch (Exception e){
				s.revert(s_backup);
				s_backup = s.clone();
				try{
					//local_declaration
					local_declaration ld = new local_declaration();
					st = st + ld.parse(s, ps);
					st = st + new spaces().parse(s, ps);
					st = st + new string(";").parse(s, ps);
					this.local_declaration = ld;
				}catch (Exception e2){
					s.revert(s_backup);
					s_backup = s.clone();
					try{
						//expression
						expression ex = new expression();
						st = st + ex.parse(s, ps);
						st = st + new spaces().parse(s, ps);
						st = st + new string(";").parse(s, ps);
						this.expression = ex;
						this.is_expression = true;
					}catch (Exception e3){
						s.revert(s_backup);
						s_backup = s.clone();
						try{
							//if
							st = st + new string("if").parse(s, ps);
							st = st + new spaces().parse(s, ps);
							st = st + new string("(").parse(s, ps);
							st = st + new spaces().parse(s, ps);
							expression ex = new expression();
							st = st + ex.parse(s, ps);
							st = st + new spaces().parse(s, ps);
							st = st + new string(")").parse(s, ps);
							st = st + new spaces().parse(s, ps);
							statement state = new statement();
							st = st + state.parse(s, ps);
							this.expression = ex;
							this.true_statement = state;
							this.is_if = true;
							s_backup = s.clone();
							try{
								String st2 = "";
								st2 = st2 + new spaces().parse(s, ps);
								st2 = st2 + new string("else").parse(s, ps);
								st2 = st2 + new spaces().parse(s, ps);
								statement state2 = new statement();
								st2 = st2 + state2.parse(s, ps);
								this.false_statement = state2;
								st = st + st2;
							}catch (Exception e3_1){
								s.revert(s_backup);
							}	
						}catch (Exception e4){
							s.revert(s_backup);
							s_backup = s.clone();
							try{
								//return
								st = st + new string("return").parse(s, ps);
								s_backup = s.clone();
								this.is_return = true;
								try{
									String st2;
									st2 = new spaces().parse(s, ps);
									expression ex = new expression();
									st2 = st2 + ex.parse(s, ps);
									st = st + st2;
									this.expression = ex;
								}catch (Exception e4_1){
									s.revert(s_backup);
								}
								st = st + new spaces().parse(s, ps);
								st = st + new string(";").parse(s, ps);
							}catch (Exception e5){
								s.revert(s_backup);
								s_backup = s.clone();
								try{
									//assert
									assert_statement as = new assert_statement();
									st = new jml_anotation(as).parse(s,ps);;
									this.assert_statement = as;
								}catch (Exception e6){
									s.revert(s_backup);
									s_backup = s.clone();
									try{
										//loop
										possibly_annotated_loop pal = new possibly_annotated_loop();
										st = st + pal.parse(s, ps);
										this.possibly_annotated_loop = pal;
									}catch (Exception e7){
										s.revert(s_backup);
										//def_type
										def_type_clause dtc = new def_type_clause();
										st = st + new my_jml_anotation(dtc).parse(s,ps);
										this.def_type_clause = dtc;
									}
								}
							}
						}
					}
				}
			}
			return st;
		}
		
		//たぶんvoidでよい？
		public void check(Check_status cs) throws Exception{
			if(cs.after_return){
				throw new Exception("after you've already done \"return\"");
			}
			if(this.is_expression){
				this.expression.check(cs);
			}else if(this.local_declaration!=null){
				this.local_declaration.check(cs);
			}else if(this.assert_statement!=null){
				this.assert_statement.check(cs);
			}else if(this.is_if){
				BoolExpr pc = (BoolExpr)this.expression.check(cs).expr;

				Check_status cs_then = cs.clone();
				this.refresh_list(cs_then);
				Check_status cs_else = null;
				if(this.false_statement!=null){
					cs_else = cs.clone();
					this.refresh_list(cs_else);
				}
				cs_then.add_path_condition(pc);
				this.true_statement.check(cs_then);
				
				if(cs_then.return_v!=null)cs.return_v = cs_then.return_v;
				if(cs_then.return_expr!=null)cs.return_expr = cs_then.return_expr;
				//cs.return_pathcondition = cs_then.return_pathcondition;
				
				if(this.false_statement!=null){
					cs_else.add_path_condition(cs.ctx.mkNot(pc));
					//return
					//cs_else.return_pathcondition = cs_then.return_pathcondition;
					cs_else.return_expr = cs_then.return_expr;
					this.false_statement.check(cs_else);
					
					if(cs_else.return_v!=null)cs.return_v = cs_else.return_v;
					if(cs_else.return_expr!=null)cs.return_expr = cs_else.return_expr;
					//cs.return_pathcondition = cs_else.return_pathcondition;
				}
				//変更されていたものは統合する
				for(Variable v : cs.variables){
					if(cs_else==null){
						Variable v_then = cs_then.get_variable(v.field_name);
						if(v.temp_num!=v_then.temp_num){
							Expr e1 = v.get_Expr(cs);
							Expr e2 = v_then.get_Expr(cs_then);
							Expr e3 = v.get_Expr_assign(cs);
							cs.add_constraint(cs.ctx.mkEq(e3, cs.ctx.mkITE(pc, e2, e1)));
							v.temp_num++;
						}
						
						v.loop_alias = v_then.loop_alias;
						
					}else{
						Variable v_then = cs_then.get_variable(v.field_name);
						Variable v_else = cs_else.get_variable(v.field_name);
						if(v.temp_num!=v_then.temp_num || v.temp_num!=v_else.temp_num){
							Expr e1 = v_else.get_Expr(cs);
							Expr e2 = v_then.get_Expr(cs_then);
							Expr e3 = v.get_Expr_assign(cs);
							cs.add_constraint(cs.ctx.mkEq(e3, cs.ctx.mkITE(pc, e2, e1)));
							v.temp_num++;
						}
						
						v.loop_alias = v_then.loop_alias || v_else.loop_alias;
					}
				}
				
				//cs.fieldsに含まれないものがあれば追加する
				for(Field f : cs_then.fields){
					cs.search_field(f.field_name, f.class_object, cs);
				}
				if(cs_else != null){
					for(Field f : cs_else.fields){
						cs.search_field(f.field_name, f.class_object, cs);
					}
				}
				
				
				for(Field f : cs.fields){
					if(cs_else==null){
						Field f_then = cs_then.search_field(f.field_name, f.class_object, cs);
						if(f.temp_num!=f_then.temp_num){
							Expr e1 = f.get_Expr(cs);
							Expr e2 = f_then.get_Expr(cs_then);
							Expr e3 = f.get_Expr_assign(cs);
							cs.add_constraint(cs.ctx.mkEq(e3, cs.ctx.mkITE(pc, e2, e1)));
							f.temp_num++;
							//コンストラクタでのfinalの初期化
							if(cs.in_constructor && f.final_initialized==false && f_then.final_initialized==true){
								throw new Exception("shuold initialize in both of then and else.");
							}
						}
					}else{
						System.out.println(f.class_object);
						Field f_then = cs_then.search_field(f.field_name, f.class_object, cs);
						Field f_else = cs_else.search_field(f.field_name, f.class_object, cs);
						if(f.temp_num!=f_then.temp_num || f.temp_num!=f_else.temp_num){
							Expr e1 = f_else.get_Expr(cs);
							Expr e2 = f_then.get_Expr(cs_then);
							Expr e3 = f.get_Expr_assign(cs);
							cs.add_constraint(cs.ctx.mkEq(e3, cs.ctx.mkITE(pc, e2, e1)));
							f.temp_num++;
							
							//コンストラクタでのfinalの初期化
							if(cs.in_constructor && f.final_initialized==false){
								if(f_then.final_initialized==true && f_else.final_initialized==true){
									f.final_initialized=true;
								}else{
									throw new Exception("shuold initialize in both of then and else.");
								}
							}
						}
					}
				}
				
				//returnのやつ
				if(cs_else!=null&& cs_then.after_return&&cs_else.after_return){
					cs.after_return = true;
				}
				
			}else if(this.compound_statement!=null){
				this.compound_statement.check(cs);
			}else if(this.def_type_clause!=null){
				cs.local_refinements.add(new Pair<String,refinement_type>(this.def_type_clause.ident,this.def_type_clause.refinement_type));
			}else if(this.is_return){
				
				//配列の篩型が安全かどうか
				BoolExpr pathcondition;
				if(cs.pathcondition==null){
					pathcondition = cs.ctx.mkBool(true);
				}else{
					pathcondition = cs.pathcondition;
				}
				Check_return rc = this.expression.check(cs);
				if(rc.field!=null && rc.field.dims>0 && rc.field.dims_sum()!=rc.indexs.size() && rc.field.refinement_type_clause!=null && rc.field.refinement_type_clause.have_index_access(rc.field.class_object.type, cs)){
					if(cs.return_v.dims>0 && cs.return_v.refinement_type_clause!=null && cs.return_v.refinement_type_clause.have_index_access(cs.return_v.class_object.type, cs)){//どっちも篩型を持つ配列
						Expr rc_assign_field_expr = rc.field.get_full_Expr(new ArrayList<IntExpr>(rc.indexs.subList(0, rc.field.class_object_dims_sum())), cs);
						//rc.exprをcomparative_assign_field_exprとして突っ込んでしまってもいいはず
						rc.field.refinement_type_clause.equal_predicate(rc.indexs, rc_assign_field_expr, rc.field.class_object, rc.field.class_object.get_full_Expr(rc.indexs, cs), cs.return_v.refinement_type_clause, new ArrayList<IntExpr>(), rc.expr, cs.return_v.class_object, cs.this_field.get_Expr(cs), cs);
					}else{//篩型の安全を保証できないような大入
						throw new Exception("can not alias with refined array");
					}
				}else if(cs.return_v.dims>0 && cs.return_v.refinement_type_clause!=null && cs.return_v.refinement_type_clause.have_index_access(cs.return_v.class_object.type, cs)){
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
						
						/*
						if(((Variable) rc.field).alias_refined == null){
							((Variable) rc.field).alias_refined = pathcondition;
						}else{
							((Variable) rc.field).alias_refined = cs.ctx.mkOr(((Variable) rc.field).alias_refined, pathcondition);
						}*/
					}else{//篩型の安全を保証できないような大入
						throw new Exception("can not alias with refined array");
					}	
				}else if(rc.field!=null && rc.field.dims>0 && rc.field.dims_sum()!=rc.indexs.size() && rc.field instanceof Variable){//ローカル変数
					Expr alias_refined;
					if(((Variable) rc.field).alias_refined == null){
						alias_refined = cs.ctx.mkBool(false);
					}else{
						alias_refined = ((Variable) rc.field).alias_refined;
					}
					
					cs.assert_constraint(cs.ctx.mkNot(alias_refined));
					
					/*
					if(((Variable) rc.field).alias == null){
						((Variable) rc.field).alias = pathcondition;
					}else{
						((Variable) rc.field).alias = cs.ctx.mkOr(((Variable) rc.field).alias, pathcondition);
					}*/
				}
				
				//返す値
				cs.return_exprs.add(rc.expr);
				cs.return_pathconditions.add(cs.pathcondition);
				
				cs.after_return = true;
			}else if(this.possibly_annotated_loop!=null){
				
				System.out.println("loop verification");
				
				/////////////どのフィールドが変更されるかの検証
				//インスタンスの生成
				Check_status cs_loop_assign_check = cs.clone();
				this.refresh_list(cs_loop_assign_check);
				
				for(Variable v : cs_loop_assign_check.variables){
					v.tmp_plus(cs_loop_assign_check);
				}
				for(Field f : cs_loop_assign_check.fields){
					f.tmp_plus(cs_loop_assign_check);
				}
				
				
				cs_loop_assign_check.in_loop = true;
				
				//ループ内での代入
				Pair<List<Pair<Field,List<List<IntExpr>>>>,Boolean> assigned_fields = new Pair<List<Pair<Field,List<List<IntExpr>>>>,Boolean>(new ArrayList<Pair<Field,List<List<IntExpr>>>>(), false);

				
				//local_declarationの処理
				if(this.possibly_annotated_loop.loop_stmt.local_declaration!=null){
					this.possibly_annotated_loop.loop_stmt.local_declaration.loop_assign(assigned_fields,cs_loop_assign_check);
				}
				
				//中身
				
				this.possibly_annotated_loop.loop_stmt.statement.loop_assign(assigned_fields, cs_loop_assign_check);
				if(this.possibly_annotated_loop.loop_stmt.expression_list!=null){
					for(expression ex : this.possibly_annotated_loop.loop_stmt.expression_list.expressions){
						ex.loop_assign(assigned_fields, cs_loop_assign_check);
					}
				}
				
				//assigned_fieldsに含まれないものが同じ程度のことは保証する
				if(!assigned_fields.snd){
					for(Variable v : cs_loop_assign_check.variables){
						boolean assigned = false;
						for(Pair<Field,List<List<IntExpr>>> assigned_field: assigned_fields.fst){
							assigned_field.fst.equals(v, cs);
							assigned = true;
							break;
						}
						if(!assigned){
							v.temp_num--;
							Expr pre_expr = v.get_Expr(cs_loop_assign_check);
							v.temp_num++;
							cs.add_constraint(cs.ctx.mkEq(pre_expr, v.get_Expr(cs_loop_assign_check)));
						}
					}
					
					for(Field f : cs_loop_assign_check.fields){
						boolean assigned = false;
						for(Pair<Field,List<List<IntExpr>>> assigned_field: assigned_fields.fst){
							assigned_field.fst.equals(f, cs);
							assigned = true;
							break;
						}
						if(!assigned){
							f.temp_num--;
							Expr pre_expr = f.get_Expr(cs_loop_assign_check);
							f.temp_num++;
							cs.add_constraint(cs.ctx.mkEq(pre_expr, f.get_Expr(cs_loop_assign_check)));
						}
					}
				}
				
				////////////ここからが本番の検証
				//インスタンスの生成
				Check_status cs_loop = cs.clone();
				this.refresh_list(cs_loop);
				cs_loop.in_loop = true;

				//local_declarationの処理
				Variable v_local=null;
				if(this.possibly_annotated_loop.loop_stmt.local_declaration!=null){
					v_local = this.possibly_annotated_loop.loop_stmt.local_declaration.check(cs_loop);
				}
				
				BoolExpr enter_loop_condition = null;
				if(this.possibly_annotated_loop.loop_stmt.expression!=null){
					enter_loop_condition = (BoolExpr) this.possibly_annotated_loop.loop_stmt.expression.check(cs_loop).expr;
				}else{
					enter_loop_condition = cs.ctx.mkBool(true);
				}
				
				
				
				//PCにループに入る条件を加える
				System.out.println("loop init condition");
				cs_loop.add_path_condition_tmp((BoolExpr) this.possibly_annotated_loop.loop_stmt.expression.check(cs_loop).expr);//途中でUnreachbleかどうかは関係ないはず
				
				for(loop_invariant li : this.possibly_annotated_loop.loop_invariants){
					BoolExpr ex = li.predicate.check(cs_loop);
					cs_loop.assert_constraint(ex);
				}
				//中身用に変数を一新
				if(assigned_fields.snd){//何でも代入できるメソッドを呼び出したとき
					for(Variable v : cs_loop.variables){
						v.tmp_plus(cs_loop);
					}
					for(Field f : cs_loop.fields){
						f.tmp_plus(cs_loop);
					}
				}else{//代入されるフィールドのそれぞれのインデックスにフレッシュな値を代入する
					for(Variable v_loop : cs_loop.variables){//ローカル変数
						System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
						Pair<Field,List<List<IntExpr>>> v_assign_indexs = null;
						for(Pair<Field,List<List<IntExpr>>> variable_assign_indexs : assigned_fields.fst){
							if(v_loop.equals(variable_assign_indexs.fst, cs)){
								v_assign_indexs = variable_assign_indexs;
								break;
							}
						}
						
						if(v_assign_indexs!=null){
							System.out.println("cccccccccccccccccccccccccccccc " + v_loop.field_name + " " + v_loop.id + " " + v_loop.temp_num  + " " +  v_assign_indexs.snd.size());
							List<Pair<BoolExpr,List<List<IntExpr>>>> b_is = new ArrayList<Pair<BoolExpr,List<List<IntExpr>>>>();
							b_is.add(new Pair(cs.ctx.mkBool(true), v_assign_indexs.snd));
							F_Assign fa = new spec_case_seq().new F_Assign(v_loop, b_is);
							fa.assign_fresh_value(cs_loop);
							System.out.println("ddddddddddddddddddddddddddddd " + v_loop.field_name + " " + v_loop.id + " " + v_loop.temp_num);
						}else{
							System.out.println("gggggggggggggggggggggggggggggggg " + v_loop.field_name + " " + v_loop.id + " " + v_loop.temp_num);
							v_loop.tmp_plus(cs_loop);
						}
						
					}
					for(Field f_loop : cs_loop.fields){//フィールド
						Pair<Field,List<List<IntExpr>>> f_assign_indexs = null;
						for(Pair<Field,List<List<IntExpr>>> field_assign_indexs : assigned_fields.fst){
							if(f_loop.equals(field_assign_indexs.fst, cs)){
								f_assign_indexs = field_assign_indexs;
								break;
							}
						}
						
						if(f_assign_indexs!=null){
							List<Pair<BoolExpr,List<List<IntExpr>>>> b_is = new ArrayList();
							b_is.add(new Pair(cs.ctx.mkBool(true), f_assign_indexs.snd));
							F_Assign fa = new spec_case_seq().new F_Assign(f_loop, b_is);
							fa.assign_fresh_value(cs_loop);
						}
					}
				}

				
				//中身の初期条件
				System.out.println("a loop pre condition");
				cs_loop.add_path_condition((BoolExpr) this.possibly_annotated_loop.loop_stmt.expression.check(cs_loop).expr);
				for(loop_invariant li : this.possibly_annotated_loop.loop_invariants){
					BoolExpr ex = li.predicate.check(cs_loop);
					cs_loop.add_constraint(ex);
				}
				
				//中身
				this.possibly_annotated_loop.loop_stmt.statement.check(cs_loop);
				if(this.possibly_annotated_loop.loop_stmt.expression_list!=null)this.possibly_annotated_loop.loop_stmt.expression_list.check(cs_loop);
				
				
				//中身の事後条件
				System.out.println("a loop post condition");
				for(loop_invariant li : this.possibly_annotated_loop.loop_invariants){
					BoolExpr ex = li.predicate.check(cs_loop);
					cs_loop.assert_constraint(ex);
				}
				
				//全体の事後条件
				System.out.println("loop post condition");
				//変更されていたものは統合する
				//cs.fieldsに含まれないものがあれば追加する
				for(Field f : cs_loop.fields){
					cs.search_field(f.field_name, f.class_object, cs);
				}
				for(Field f : cs.fields){//フィールドに関して値を更新
					Field f_loop = cs_loop.search_field(f.field_name,f.class_object, cs);
					if(f.temp_num<f_loop.temp_num){
						if(cs.in_constructor && f.modifiers!=null && f.modifiers.is_final){//コンストラクタのfinalの初期化はloopのなかではできない
							if(f.final_initialized==false&&f_loop.final_initialized==true){
								throw new Exception("can not initialized final variable in loop.");
							}
						}
						Expr e1 = f.get_Expr(cs);
						Expr e2 = f_loop.get_Expr(cs_loop);
						Expr e3 = f.get_Expr_assign(cs);
						cs.add_constraint(cs.ctx.mkEq(e3, cs.ctx.mkITE(enter_loop_condition, e2, e1)));
						f.temp_num++;
					}
				}
				for(Variable v :cs.variables){//ローカル変数に関して値を更新
					Variable v_loop = cs_loop.get_variable(v.field_name);
					if(v.temp_num<v_loop.temp_num){
						Expr e1 = v.get_Expr(cs);
						Expr e2 = v_loop.get_Expr(cs_loop);
						Expr e3 = v.get_Expr_assign(cs);
						cs.add_constraint(cs.ctx.mkEq(e3, cs.ctx.mkITE(enter_loop_condition, e2, e1)));
						v.temp_num++;
					}
					
					//配列のエイリアス
					if(v_loop.loop_alias){
						if(cs.in_loop){
							v.loop_alias = true;
						}else{
							if(((Variable) v).alias == null){
								((Variable) v).alias = enter_loop_condition;
							}else{
								((Variable) v).alias = cs.ctx.mkOr(((Variable) v).alias, enter_loop_condition);
							}
						}
					}
				}
				
				//ループ出た後の条件
				BoolExpr post_loop = cs.ctx.mkNot((BoolExpr) this.possibly_annotated_loop.loop_stmt.expression.check(cs_loop).expr);
				
				BoolExpr pre_pathcondition = cs.pathcondition;
				
				for(loop_invariant li : this.possibly_annotated_loop.loop_invariants){
					BoolExpr li_expr = li.predicate.check(cs_loop);
					post_loop = cs.ctx.mkAnd(post_loop, li_expr);
					cs.add_path_condition_tmp(li_expr);
				}
				
				cs.pathcondition = pre_pathcondition;
				
				cs.add_constraint(cs.ctx.mkImplies(enter_loop_condition, post_loop));
				
				//v_localは中身での変数なので消す
				if(v_local!=null)cs.variables.remove(v_local);
				
				
				
			}else{
				System.out.println("statementのまだ書いてないところ");
			}
			

		}
		
		public void refresh_list(Check_status cs) throws Exception{
			if(cs.variables!=null){
				List<Variable> v_tmp = cs.variables;
				cs.variables = new ArrayList<Variable>();
				for(Variable v : v_tmp){
					Variable new_v = v.clone_e();
					new_v.id = cs.Check_status_share.get_tmp_num();
					cs.add_constraint(cs.ctx.mkEq(v.get_Expr(cs), new_v.get_Expr(cs)));
					cs.variables.add(new_v);
					
					//配列のループ内でのエイリアスしたかどうかは引き継ぐ
					new_v.loop_alias = v.loop_alias;
					
				}
			}
			if(cs.fields!=null){
				List<Field> f_tmp = cs.fields;
				cs.fields = new ArrayList<Field>();
				for(Field f : f_tmp){
					Field new_f = f.clone_e();
					new_f.id = cs.Check_status_share.get_tmp_num();
					cs.add_constraint(cs.ctx.mkEq(f.get_Expr(cs), new_f.get_Expr(cs)));
					cs.fields.add(new_f);
				}
			}
		}
		
		//前と値が同じではない
		public void new_id_list(Check_status cs) throws Exception{
			if(cs.variables!=null){
				List<Variable> v_tmp = cs.variables;
				cs.variables = new ArrayList<Variable>();
				for(Variable v : v_tmp){
					Variable new_v = v.clone_e();
					new_v.id = cs.Check_status_share.get_tmp_num();
					cs.variables.add(new_v);
				}
			}
			if(cs.fields!=null){
				List<Field> f_tmp = cs.fields;
				cs.fields = new ArrayList<Field>();
				for(Field f : f_tmp){
					Field new_f = f.clone_e();
					new_f.id = cs.Check_status_share.get_tmp_num();
					cs.fields.add(new_f);
				}
			}
		}
		
		public void loop_assign(Pair<List<Pair<Field,List<List<IntExpr>>>>,Boolean>assigned_fields, Check_status cs) throws Exception{
			if(this.is_expression){
				this.expression.loop_assign(assigned_fields,cs);
			}else if(this.local_declaration!=null){
				this.local_declaration.loop_assign(assigned_fields,cs);
			}else if(this.assert_statement!=null){
				//なにもしない
			}else if(this.is_if){
				this.true_statement.loop_assign(assigned_fields,cs);
				this.false_statement.loop_assign(assigned_fields,cs);
			}else if(this.compound_statement!=null){
				for(statement statement : this.compound_statement.statements){
					statement.loop_assign(assigned_fields,cs);
				}
			}else if(this.def_type_clause!=null){
				//なにもしない
			}else if(this.is_return){
				//なにもしない
			}else if(this.possibly_annotated_loop!=null){
				this.possibly_annotated_loop.loop_stmt.statement.loop_assign(assigned_fields, cs);
				if(this.possibly_annotated_loop.loop_stmt.expression_list!=null){
					for(expression expression : this.possibly_annotated_loop.loop_stmt.expression_list.expressions){
						expression.loop_assign(assigned_fields, cs);
					}
				}
			}
		}
	
	}
	
