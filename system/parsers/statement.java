package system.parsers;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;

import system.Check_return;
import system.Check_status;
import system.Field;
import system.Helper_assigned_field;
import system.Pair;
import system.Parser;
import system.Parser_status;
import system.Source;
import system.Variable;
import system.F_Assign;

//compund_statement�ŋ󔒁A���s������Ă���
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
		
		//���Ԃ�void�ł悢�H
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
				//�ύX����Ă������͓̂�������
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
						
						//helper���\�b�h��R���X�g���N�^�[�̒��ŁA�Q�����ȏ�̔z��Ƃ��ăG�C���A�X�����ꍇ
						v.alias_2d_in_helper_or_consutructor = v_then.alias_2d_in_helper_or_consutructor;
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
						
						//helper���\�b�h��R���X�g���N�^�[�̒��ŁA�Q�����ȏ�̔z��Ƃ��ăG�C���A�X�����ꍇ
						v.alias_2d_in_helper_or_consutructor = cs.ctx.mkOr(v_then.alias_2d_in_helper_or_consutructor, v_else.alias_2d_in_helper_or_consutructor);
					}
				}
				
				//cs.fields�Ɋ܂܂�Ȃ����̂�����Βǉ�����
				for(Field f : cs_then.fields){
					Field cs_f = cs.search_field(f.field_name, f.class_object, cs);
					if(f.internal_id!=cs_f.internal_id){//���[�v���Œǉ����ꂽ���̂́A�����l�������ł���Ƃ��������������
						int pre_tmp_num = f.temp_num;
						f.temp_num = 0;
						Expr expr = f.get_Expr(cs);
						f.temp_num = pre_tmp_num;
						expr = cs.ctx.mkEq(expr, cs_f.get_Expr(cs));//���̎��_�ł�cs_f��tmp_num��0�ł���͂�
						cs.add_constraint((BoolExpr) expr);
					}
				}
				if(cs_else != null){
					for(Field f : cs_else.fields){
						Field cs_f = cs.search_field(f.field_name, f.class_object, cs);
						if(f.internal_id!=cs_f.internal_id){//���[�v���Œǉ����ꂽ���̂́A�����l�������ł���Ƃ��������������
							int pre_tmp_num = f.temp_num;
							f.temp_num = 0;
							Expr expr = f.get_Expr(cs);
							f.temp_num = pre_tmp_num;
							expr = cs.ctx.mkEq(expr, cs_f.get_Expr(cs));//���̎��_�ł�cs_f��tmp_num��0�ł���͂�
							cs.add_constraint((BoolExpr) expr);
						}
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
							//�R���X�g���N�^�ł�final�̏�����
							if(cs.in_constructor && f.final_initialized==false && f_then.final_initialized==true){
								throw new Exception("shuold initialize in both of then and else.");
							}
						}
						
						//helper���\�b�h��R���X�g���N�^�[�̒��ŁA�Q�����ȏ�̔z��Ƃ��ăG�C���A�X�����ꍇ
						f.alias_2d_in_helper_or_consutructor = f_then.alias_2d_in_helper_or_consutructor;
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
							
							//�R���X�g���N�^�ł�final�̏�����
							if(cs.in_constructor && f.final_initialized==false){
								if(f_then.final_initialized==true && f_else.final_initialized==true){
									f.final_initialized=true;
								}else{
									throw new Exception("shuold initialize in both of then and else.");
								}
							}
						}
						
						//helper���\�b�h��R���X�g���N�^�[�̒��ŁA�Q�����ȏ�̔z��Ƃ��ăG�C���A�X�����ꍇ
						f.alias_2d_in_helper_or_consutructor = cs.ctx.mkOr(f_then.alias_2d_in_helper_or_consutructor, f_else.alias_2d_in_helper_or_consutructor);
					}
				}
				
				//return�̂��
				if(cs_else!=null&& cs_then.after_return&&cs_else.after_return){
					cs.after_return = true;
				}
				
				//helper���\�b�h�̑�����ꂽ�t�B�[���h
				if(!cs_then.after_return){
					for(Helper_assigned_field assigned_field : cs_then.helper_assigned_fields){
						if(!cs.helper_assigned_fields.contains(assigned_field)){
							cs.helper_assigned_fields.add(assigned_field);
						}
					}
				}
				if(cs_else!=null&& !cs_else.after_return){
					for(Helper_assigned_field assigned_field : cs_else.helper_assigned_fields){
						if(!cs.helper_assigned_fields.contains(assigned_field)){
							cs.helper_assigned_fields.add(assigned_field);
						}
					}
				}
				
				
			}else if(this.compound_statement!=null){
				this.compound_statement.check(cs);
			}else if(this.def_type_clause!=null){
				cs.local_refinements.add(new Pair<String,refinement_type>(this.def_type_clause.ident,this.def_type_clause.refinement_type));
			}else if(this.is_return){

				Check_return rc = this.expression.check(cs);
				
				//�z���⿌^�����S���ǂ���
				Expr rc_assign_field_expr = null;
				Expr rc_class_field_expr = null;
				if(rc.field!=null){
					rc_assign_field_expr = rc.field.get_full_Expr(new ArrayList<IntExpr>(rc.indexs.subList(0, rc.field.class_object_dims_sum())), cs);
					rc_class_field_expr = rc.field.class_object.get_full_Expr((ArrayList<IntExpr>) rc.indexs.clone(), cs);
				}
				cs.check_array_alias(cs.return_v, rc.expr, cs.this_field.get_Expr(cs), new ArrayList<IntExpr>(), rc.field, rc_assign_field_expr, rc_class_field_expr, rc.indexs);
				
				
				//�Ԃ��l
				cs.return_expr = rc.expr;
				//��������̌���
				cs.md.check_post_condition(cs);
				
				cs.after_return = true;
				
				BoolExpr pathcondition;
				if(cs.pathcondition==null){
					pathcondition = cs.ctx.mkBool(true);
				}else{
					pathcondition = cs.pathcondition;
				}
				cs.return_conditions.add(pathcondition);
			}else if(this.possibly_annotated_loop!=null){
				
				System.out.println("loop verification");
				
				/////////////�ǂ̃t�B�[���h���ύX����邩�̌���
				System.out.println("check assigned fields");
				//�C���X�^���X�̐���
				Check_status cs_loop_assign_check = cs.clone();
				this.refresh_list(cs_loop_assign_check);
				
				for(Variable v : cs_loop_assign_check.variables){
					v.tmp_plus(cs_loop_assign_check);
				}
				for(Field f : cs_loop_assign_check.fields){
					f.tmp_plus(cs_loop_assign_check);
				}
				
				
				//���[�v���ł̑��
				Pair<List<Pair<Field,List<List<IntExpr>>>>,Boolean> assigned_fields = new Pair<List<Pair<Field,List<List<IntExpr>>>>,Boolean>(new ArrayList<Pair<Field,List<List<IntExpr>>>>(), false);

				
				//local_declaration�̏���
				Variable assign_check_local_v = null;
				if(this.possibly_annotated_loop.loop_stmt.local_declaration!=null){
					assign_check_local_v = this.possibly_annotated_loop.loop_stmt.local_declaration.loop_assign(assigned_fields,cs_loop_assign_check);
				}
				
				BoolExpr enter_loop_condition_assign_check = null;
				if(this.possibly_annotated_loop.loop_stmt.expression!=null){
					enter_loop_condition_assign_check = (BoolExpr) this.possibly_annotated_loop.loop_stmt.expression.check(cs_loop_assign_check).expr;
				}else{
					enter_loop_condition_assign_check = cs.ctx.mkBool(true);
				}
				cs_loop_assign_check.add_path_condition_tmp(enter_loop_condition_assign_check);
				
				//���g
				this.possibly_annotated_loop.loop_stmt.statement.loop_assign(assigned_fields, cs_loop_assign_check);
				if(this.possibly_annotated_loop.loop_stmt.expression_list!=null){
					for(expression ex : this.possibly_annotated_loop.loop_stmt.expression_list.expressions){
						ex.loop_assign(assigned_fields, cs_loop_assign_check);
					}
				}
				
				//cs.fields�Ɋ܂܂�Ȃ����̂�����Βǉ�����
				for(Field f : cs_loop_assign_check.fields){
					Field cs_f = cs.search_field(f.field_name, f.class_object, cs);
					if(f.internal_id!=cs_f.internal_id){//���[�v���Œǉ����ꂽ���̂́A�����l�������ł���Ƃ��������������
						int pre_tmp_num = f.temp_num;
						f.temp_num = 0;
						Expr expr = f.get_Expr(cs);
						f.temp_num = pre_tmp_num;
						expr = cs.ctx.mkEq(expr, cs_f.get_Expr(cs));//���̎��_�ł�cs_f��tmp_num��0�ł���͂�
						cs.add_constraint((BoolExpr) expr);
					}
				}
				//assigned_fields�Ɋ܂܂�Ȃ����̂��������x�̂��Ƃ͕ۏ؂���
				if(!assigned_fields.snd){
					System.out.println("no assign fields in loop");
					for(Variable v : cs_loop_assign_check.variables){
						boolean assigned = false;
						for(Pair<Field,List<List<IntExpr>>> assigned_field: assigned_fields.fst){
							if(assigned_field.fst.equals(v, cs)){
								assigned = true;
								break;
							}
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
							if(assigned_field.fst.equals(f, cs)){
								assigned = true;
								break;
							}
						}
						if(!assigned){
							f.temp_num--;
							Expr pre_expr = f.get_Expr(cs_loop_assign_check);
							f.temp_num++;
							cs.add_constraint(cs.ctx.mkEq(pre_expr, f.get_Expr(cs_loop_assign_check)));
						}
					}
				}
				//helper���\�b�h��R���X�g���N�^�[�̒��ŁA�Q�����ȏ�̔z��Ƃ��ăG�C���A�X�����ꍇ
				for(Variable v : cs_loop_assign_check.variables){
					Field cs_v = cs.search_internal_id(v.internal_id);
					if(cs_v!=null)cs_v.alias_2d_in_helper_or_consutructor = v.alias_2d_in_helper_or_consutructor;//���[�v���Œǉ����ꂽ���[�J���ϐ��̏ꍇ��null�ɂȂ�
				}
				for(Field f : cs_loop_assign_check.fields){
					Field cs_f = cs.search_internal_id(f.internal_id);
					cs_f.alias_2d_in_helper_or_consutructor = f.alias_2d_in_helper_or_consutructor;
				}
				
				
				////////////�������炪�{�Ԃ̌���
				System.out.println("check loop");
				//�C���X�^���X�̐���
				Check_status cs_loop = cs.clone();
				this.refresh_list(cs_loop);

				//local_declaration�̏���
				Variable v_local=null;
				if(this.possibly_annotated_loop.loop_stmt.local_declaration!=null){
					v_local = this.possibly_annotated_loop.loop_stmt.local_declaration.check(cs_loop);
					if(assign_check_local_v!=null){
						v_local.internal_id = assign_check_local_v.internal_id;
					}
				}
				
				BoolExpr enter_loop_condition = null;
				if(this.possibly_annotated_loop.loop_stmt.expression!=null){
					enter_loop_condition = (BoolExpr) this.possibly_annotated_loop.loop_stmt.expression.check(cs_loop).expr;
				}else{
					enter_loop_condition = cs.ctx.mkBool(true);
				}
				
				
				
				//PC�Ƀ��[�v�ɓ��������������
				System.out.println("loop init condition");
				cs_loop.add_path_condition(enter_loop_condition);
				
				for(loop_invariant li : this.possibly_annotated_loop.loop_invariants){
					BoolExpr ex = li.predicate.check(cs_loop);
					cs_loop.assert_constraint(ex);
				}
				
				//���g�p�ɕϐ�����V
				System.out.println("loop fields refresh");
				if(assigned_fields.snd){//���ł�����ł��郁�\�b�h���Ăяo�����Ƃ�
					System.out.println("assigned all in loop");
					for(Variable v : cs_loop.variables){
						v.tmp_plus(cs_loop);
					}
					for(Field f : cs_loop.fields){
						f.tmp_plus(cs_loop);
					}
				}else{//��������t�B�[���h�̂��ꂼ��̃C���f�b�N�X�Ƀt���b�V���Ȓl��������
					for(Variable v_loop : cs_loop.variables){//���[�J���ϐ�
						Pair<Field,List<List<IntExpr>>> v_assign_indexs = null;
						for(Pair<Field,List<List<IntExpr>>> variable_assign_indexs : assigned_fields.fst){
							if(v_loop.equals(variable_assign_indexs.fst, cs)){
								v_assign_indexs = variable_assign_indexs;
								break;
							}
						}
						
						if(v_assign_indexs!=null){
							List<Pair<BoolExpr,List<List<IntExpr>>>> b_is = new ArrayList<Pair<BoolExpr,List<List<IntExpr>>>>();
							b_is.add(new Pair(cs.ctx.mkBool(true), v_assign_indexs.snd));
							F_Assign fa = new F_Assign(v_loop, b_is);
							fa.assign_fresh_value(cs_loop);
						}
						
					}
					for(Field f_loop : cs_loop.fields){//�t�B�[���h
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
							F_Assign fa = new F_Assign(f_loop, b_is);
							fa.assign_fresh_value(cs_loop);
						}
					}
				}
				
				//�O�Œ�`���ꂽ�ϐ�
				for(Variable v : cs_loop.variables){
					v.out_loop_v = true;
				}

				
				//���g�̏�������
				System.out.println("a loop pre condition");
				if(this.possibly_annotated_loop.loop_stmt.expression!=null){
					enter_loop_condition = (BoolExpr) this.possibly_annotated_loop.loop_stmt.expression.check(cs_loop).expr;
				}else{
					enter_loop_condition = cs.ctx.mkBool(true);
				}
				cs_loop.add_path_condition_tmp(enter_loop_condition);//�r����Unreachble���ǂ����͊֌W�Ȃ��͂�
				for(loop_invariant li : this.possibly_annotated_loop.loop_invariants){
					BoolExpr ex = li.predicate.check(cs_loop);
					cs_loop.add_constraint(ex);
				}
				
				//���g
				this.possibly_annotated_loop.loop_stmt.statement.check(cs_loop);
				if(this.possibly_annotated_loop.loop_stmt.expression_list!=null)this.possibly_annotated_loop.loop_stmt.expression_list.check(cs_loop);
				
				
				//���g�̎������
				System.out.println("a loop post condition");
				for(loop_invariant li : this.possibly_annotated_loop.loop_invariants){
					BoolExpr ex = li.predicate.check(cs_loop);
					cs_loop.assert_constraint(ex);
				}
				
				//�S�̂̎������
				System.out.println("loop post condition");
				//�ύX����Ă������͓̂�������
				//cs.fields�Ɋ܂܂�Ȃ����̂�����Βǉ�����
				for(Field f : cs_loop.fields){
					Field cs_f = cs.search_field(f.field_name, f.class_object, cs);
					if(f.internal_id!=cs_f.internal_id){//���[�v���Œǉ����ꂽ���̂́A�����l�������ł���Ƃ��������������
						int pre_tmp_num = f.temp_num;
						f.temp_num = 0;
						Expr expr = f.get_Expr(cs);
						f.temp_num = pre_tmp_num;
						expr = cs.ctx.mkEq(expr, cs_f.get_Expr(cs));//���̎��_�ł�cs_f��tmp_num��0�ł���͂�
						cs.add_constraint((BoolExpr) expr);
					}
				}
				for(Field f : cs.fields){//�t�B�[���h�Ɋւ��Ēl���X�V
					Field f_loop = cs_loop.search_field(f.field_name,f.class_object, cs);
					if(f.temp_num<f_loop.temp_num){
						if(cs.in_constructor && f.modifiers!=null && f.modifiers.is_final){//�R���X�g���N�^��final�̏�������loop�̂Ȃ��ł͂ł��Ȃ�
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
					
					//helper���\�b�h��R���X�g���N�^�[�̒��ŁA�Q�����ȏ�̔z��Ƃ��ăG�C���A�X�����ꍇ
					f.alias_2d_in_helper_or_consutructor = f_loop.alias_2d_in_helper_or_consutructor;
				}
				for(Variable v :cs.variables){//���[�J���ϐ��Ɋւ��Ēl���X�V
					Variable v_loop = cs_loop.get_variable(v.field_name);
					if(v.temp_num<v_loop.temp_num){
						Expr e1 = v.get_Expr(cs);
						Expr e2 = v_loop.get_Expr(cs_loop);
						Expr e3 = v.get_Expr_assign(cs);
						cs.add_constraint(cs.ctx.mkEq(e3, cs.ctx.mkITE(enter_loop_condition, e2, e1)));
						v.temp_num++;
					}
					//�z��̃G�C���A�X
					if(((Variable) v).alias == null){
						((Variable) v).alias = v_loop.alias;
					}else{//v.alias!=null�Ȃ�v_loop.alias!=null�ł���͂�
						((Variable) v).alias = cs.ctx.mkOr(((Variable) v).alias, v_loop.alias);
					}
					
					//helper���\�b�h��R���X�g���N�^�[�̒��ŁA�Q�����ȏ�̔z��Ƃ��ăG�C���A�X�����ꍇ
					v.alias_2d_in_helper_or_consutructor = v_loop.alias_2d_in_helper_or_consutructor;
				}
				
				
				//helper���\�b�h�̑�����ꂽ�t�B�[���h
				if(!cs_loop.after_return){
					for(Helper_assigned_field assigned_field : cs_loop.helper_assigned_fields){
						if(!cs.helper_assigned_fields.contains(assigned_field)){
							cs.helper_assigned_fields.add(assigned_field);
						}
					}
				}
				
				
				//���[�v�o����̏���
				if(this.possibly_annotated_loop.loop_stmt.expression!=null){
					enter_loop_condition = (BoolExpr) this.possibly_annotated_loop.loop_stmt.expression.check(cs_loop).expr;
				}else{
					enter_loop_condition = cs.ctx.mkBool(true);
				}
				BoolExpr post_loop = cs.ctx.mkNot(enter_loop_condition);
				
				BoolExpr pre_pathcondition = cs.pathcondition;
				
				for(loop_invariant li : this.possibly_annotated_loop.loop_invariants){
					BoolExpr li_expr = li.predicate.check(cs_loop);
					post_loop = cs.ctx.mkAnd(post_loop, li_expr);
					cs.add_path_condition_tmp(li_expr);
				}
				
				cs.pathcondition = pre_pathcondition;
				
				
				
				cs.add_constraint(cs.ctx.mkImplies(enter_loop_condition, post_loop));
				
				//v_local�͒��g�ł̕ϐ��Ȃ̂ŏ���
				if(v_local!=null)cs.variables.remove(v_local);
				
				
				
			}else{
				System.out.println("statement�̂܂������ĂȂ��Ƃ���");
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
		
		//�O�ƒl�������ł͂Ȃ�
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
				//�Ȃɂ����Ȃ�
			}else if(this.is_if){
				BoolExpr pc = (BoolExpr)this.expression.check(cs).expr;
				
				BoolExpr pre_pathcondition = cs.pathcondition;
				cs.add_path_condition_tmp(pc);
				this.true_statement.loop_assign(assigned_fields,cs);
				cs.pathcondition = pre_pathcondition;
				
				if(this.false_statement!=null){
					cs.add_path_condition_tmp(cs.ctx.mkNot(pc));
					this.false_statement.loop_assign(assigned_fields,cs);
					cs.pathcondition = pre_pathcondition;
				}
				
			}else if(this.compound_statement!=null){
				for(statement statement : this.compound_statement.statements){
					statement.loop_assign(assigned_fields,cs);
				}
			}else if(this.def_type_clause!=null){
				//�Ȃɂ����Ȃ�
			}else if(this.is_return){
				//�Ȃɂ����Ȃ�
			}else if(this.possibly_annotated_loop!=null){
				BoolExpr enter_loop_condition = null;
				if(this.possibly_annotated_loop.loop_stmt.expression!=null){
					enter_loop_condition = (BoolExpr) this.possibly_annotated_loop.loop_stmt.expression.check(cs).expr;
				}else{
					enter_loop_condition = cs.ctx.mkBool(true);
				}
				BoolExpr pre_pathcondition = cs.pathcondition;
				System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + enter_loop_condition);
				cs.add_path_condition_tmp(enter_loop_condition);
				
				this.possibly_annotated_loop.loop_stmt.statement.loop_assign(assigned_fields, cs);
				if(this.possibly_annotated_loop.loop_stmt.expression_list!=null){
					for(expression expression : this.possibly_annotated_loop.loop_stmt.expression_list.expressions){
						expression.loop_assign(assigned_fields, cs);
					}
				}
				
				cs.pathcondition = pre_pathcondition;
			}
		}
	
	}
	
