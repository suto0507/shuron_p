package system.parsers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;

import system.Check_status;
import system.Field;
import system.Helper_assigned_field;
import system.Pair;
import system.Parser;
import system.Parser_status;
import system.Source;
import system.Summery;
import system.Variable;
import system.parsers.spec_case_seq.F_Assign;

public class method_decl implements Parser<String>{
	String st;
	method_specification method_specification;
	modifiers modifiers;
	type_spec type_spec;
	String ident;
	int args_num;
	formals formals;
	compound_statement compound_statement;
	
	public String class_type_name;
	
	
	public String parse(Source s,Parser_status ps)throws Exception{
		this.st = "";
		
		Source s_backup = s.clone();
		try{
			method_specification ms = new method_specification();
			this.st = this.st + ms.parse(s, ps);
			st = st + new spaces().parse(s, ps);
			this.method_specification = ms;
		}catch (Exception e){
			s.revert(s_backup);
		}	
		
		this.modifiers = new modifiers();
		this.st = this.st + this.modifiers.parse(s, ps);
		this.st = this.st + new newLines().parse(s, ps);
		
		this.st = this.st + new spaces().parse(s, ps);
		s_backup = s.clone();
		try{
			String st2 = "";
			type_spec ts = new type_spec();
			st2 = st2 + ts.parse(s, ps);
			this.type_spec = ts;
			st2 = st2 + new spaces().parse(s, ps);
			
			method_head mh = new method_head();
			st2 = st2 + mh.parse(s, ps);
			this.ident = mh.ident;
			this.formals = mh.formals;
			this.st = this.st + st2;
		}catch (Exception e){
			s.revert(s_backup);
			method_head mh = new method_head();
			this.st = this.st + mh.parse(s, ps);
			this.ident = mh.ident;
			this.formals = mh.formals;
			this.type_spec = null;
		}	
		

		
		this.st = this.st + new newLines().parse(s, ps);
		
		this.compound_statement = new compound_statement();
		this.st = this.st + this.compound_statement.parse(s, ps);
		
		this.class_type_name = ps.class_type_name;
		
		return st;
	}
	
	public void check(Check_status cs, Summery summery){
		
		System.out.println("Verify method " + this.ident);
		
		//初期化
		cs.md = this;
		cs.return_conditions = new ArrayList<BoolExpr>();
		
		if(this.modifiers.is_helper) cs.in_helper = true;
		
		
		//色々の処理を後で追加
		try{//returnの準備
			if(this.type_spec!=null){
				cs.return_v = new Variable(cs.Check_status_share.get_tmp_num(), "return", this.type_spec.type.type, this.type_spec.dims, this.type_spec.refinement_type_clause, this.modifiers, cs.this_field, cs.ctx.mkBool(false));
				cs.return_v.temp_num++;
				cs.return_v.alias = cs.ctx.mkBool(true);
			}else{
				//コンストラクタでの初期化
				
				for(Field f : cs.fields){
					if(f.type.equals("int") && f.dims == 0){
						BoolExpr ex = cs.ctx.mkEq(cs.ctx.mkSelect((ArrayExpr) f.get_Expr(cs), cs.this_field.get_Expr(cs)), cs.ctx.mkInt(0));	
						cs.add_constraint(ex);
					}else if(f.type.equals("boolean") && f.dims == 0){
						BoolExpr ex = cs.ctx.mkEq(cs.ctx.mkSelect((ArrayExpr) f.get_Expr(cs), cs.this_field.get_Expr(cs)), cs.ctx.mkFalse());
						cs.add_constraint(ex);			
					}
				}
			}
			
			//引数
			this.formals.check(cs);
			
			//old用
			Check_status csc = cs.clone();
			csc.clone_list();
			cs.this_old_status = csc;
			csc.this_old_status = cs;

			if(this.type_spec!=null){
				
				//事前条件
				System.out.println("precondition invariant");
				BoolExpr pre_invariant_expr = null;
				if(cs.invariants!=null&&cs.invariants.size()>0 && !cs.in_helper){
					for(invariant inv : cs.invariants){
						if(inv.is_private==true){//可視性が同じものしか使えない
							cs.ban_default_visibility = true;
						}else{
							cs.ban_private_visibility = true;
						}

						if(pre_invariant_expr == null){
							pre_invariant_expr = (BoolExpr) inv.check(cs);
						}else{
							pre_invariant_expr = cs.ctx.mkAnd(pre_invariant_expr, (BoolExpr)inv.check(cs));
						}

						cs.ban_default_visibility = false;
						cs.ban_private_visibility = false;
					}
					cs.add_constraint(pre_invariant_expr);
				}

			
			}else{//コンストラクタ
				cs.in_constructor = true;
			}
			

			System.out.println("precondition requires");
			BoolExpr require_expr = null;
			
			if(this.method_specification != null){
				if(this.modifiers.is_privte==false){
					cs.ban_private_visibility = true;
				}
				
				require_expr = this.method_specification.requires_expr(cs);
				cs.add_constraint(require_expr);
				
				cs.ban_private_visibility = false;
			}
			
			
		
			

			if(this.method_specification != null){
				if(this.modifiers.is_privte==false){
					cs.ban_private_visibility = true;
				}
				Pair<List<F_Assign>, BoolExpr> assign_cnsts = this.method_specification.assignables(cs);
				//各フィールドの代入条件を各フィールドに持たせる
				for(F_Assign fa : assign_cnsts.fst){
					fa.field.assinable_cnst_indexs = fa.cnst_array;
				}
				//どのフィールドにも代入できる条件
				cs.assinable_cnst_all = assign_cnsts.snd;
				
				
				cs.ban_private_visibility = false;
			}else{//何でも代入していい
				cs.assinable_cnst_all = cs.ctx.mkBool(true);
			}
			
			
			String pre_class_type_name = cs.this_field.type;
			cs.this_field.type = this.class_type_name;
			//中身
			this.compound_statement.check(cs);
			
			cs.this_field.type = pre_class_type_name;
			
			//returnしなかった場合の検証
			if(cs.after_return==false && (this.type_spec==null||this.type_spec.type.type.equals("void"))){
				this.check_post_condition(cs);
			}
			if(cs.after_return==false && !(this.type_spec==null||this.type_spec.type.type.equals("void"))){
				throw new Exception("In some cases, \"return\" has not been done.");
			}
			
			
			System.out.println("method \"" + this.ident + "\" is valid\n\n");
			summery.valids.add("" + this.ident + "(class : " + this.class_type_name + ")" + " " + summery.file.toString());
		}catch(Exception e){
			System.out.println(e);
			System.out.println("!!!!!!!!!! method \"" + this.ident + "\" is invalid !!!!!!!!!!\n\n");
			summery.invalids.add("" + this.ident + "(class : " + this.class_type_name + ")" + " " + summery.file.toString());
		}
	}
	
	
	//事後条件の検証
	public void check_post_condition(Check_status cs) throws Exception{
		//returnの処理

		
		//事後条件
		System.out.println("postcondition invariant");
		BoolExpr post_invariant_expr = null;
		if(cs.invariants!=null&&cs.invariants.size()>0 && !cs.in_helper){
			for(invariant inv : cs.invariants){
				if(inv.is_private==true){//可視性が同じものしか使えない
					cs.ban_default_visibility = true;
				}else{
					cs.ban_private_visibility = true;
				}
				
				if(post_invariant_expr == null){
					post_invariant_expr = (BoolExpr) inv.check(cs);
				}else{
					post_invariant_expr = cs.ctx.mkAnd(post_invariant_expr, (BoolExpr)inv.check(cs));
				}
				
				cs.ban_default_visibility = false;
				cs.ban_private_visibility = false;
			}
			cs.assert_constraint(post_invariant_expr);
		}
		System.out.println("postcondition ensures");
		BoolExpr ensures_expr = null;
		if(this.method_specification!=null){
			if(this.modifiers.is_privte==false){
				cs.ban_private_visibility = true;
			}
			

			ensures_expr = this.method_specification.ensures_expr(cs);

			cs.assert_constraint(ensures_expr);
			
			cs.ban_private_visibility = false;
		}
		
		
		
		//返り値のrefinement_type
		//返り値がモデルフィールドと結び付けられることはないはず
		//なので、cs.return_exprを篩型が持つ変数の値として渡すためにここはそのまま
		if(this.type_spec!=null&&cs.return_v.refinement_type_clause!=null){
			if(cs.return_v.refinement_type_clause.refinement_type!=null){
				cs.return_v.refinement_type_clause.refinement_type.assert_refinement(cs, cs.return_v, cs.return_expr, cs.this_field, cs.this_field.get_Expr(cs), new ArrayList<IntExpr>());
			}else if(cs.return_v.refinement_type_clause.ident!=null){
				refinement_type rt = cs.search_refinement_type(cs.return_v.class_object.type, cs.return_v.refinement_type_clause.ident);
				if(rt!=null){
					rt.assert_refinement(cs, cs.return_v, cs.return_expr, cs.this_field, cs.this_field.get_Expr(cs), new ArrayList<IntExpr>());
				}else{
					throw new Exception("can't find refinement type " + cs.return_v.refinement_type_clause.ident);
				}
			}
		}
		
		
		
		
		if(cs.in_constructor){
			//コンストラクタの最後での篩型のチェック
			cs.constructor_refinement_check();
			
			//コンストラクタ内で初期化されていないfinalがないか
			for(Field f : cs.fields){
				if(f.modifiers!=null && f.modifiers.is_final){
					if(f.class_object!=null && f.class_object.equals(cs.this_field) && f.final_initialized==false){
						throw new Exception("final variable " + f.field_name + " was not initialized.");
					}
				}
			}
		}
		
		if(cs.in_helper){
			
			System.out.println("refinement type check : helper method");
			
			//helperの最後での篩型のチェック
			
			for(Helper_assigned_field assigned_fields : cs.helper_assigned_fields){
				cs.solver.push();
				
				cs.add_constraint(assigned_fields.assigned_pathcondition);
				Field v = cs.search_internal_id(assigned_fields.field.internal_id);
				if(v == null)v = assigned_fields.field;//if内、ループ内で定義された変数　これも最終的に篩型を満たすかの検証を行う必要がある
				Field old_v = cs.this_old_status.search_internal_id(v.internal_id);
				
				Expr v_class_object_expr = assigned_fields.class_object_expr;

				Expr old_assign_field_expr = null;
				Expr assign_field_expr = null;
				if(v instanceof Variable){
					assign_field_expr = v.get_Expr(cs);
				}else{
					old_assign_field_expr = cs.ctx.mkSelect(old_v.get_Expr(cs.this_old_status), v_class_object_expr);
					assign_field_expr = cs.ctx.mkSelect(v.get_Expr(cs), v_class_object_expr);
				}
				
				//メソッドの最初では篩型が満たしていることを仮定していい
				//フィールドだけ
				if(!(v instanceof Variable)){
					old_v.add_refinement_constraint(cs.this_old_status, v_class_object_expr, assigned_fields.indexs, true);
				}
				
				v.assert_refinement(cs, v_class_object_expr, assigned_fields.indexs);
				
				cs.solver.pop();
			}
			
			
		}
	}
	
	

	public void inheritance_refinement_types(class_declaration class_decl, compilation_unit cu) throws Exception{
		System.out.println("check method type inheritance : " + this.ident);
		
		class_declaration super_class = class_decl.super_class;
		
		//返り値の型の篩型
		
		//配列には新しい篩型をつけることができない
		if(this.type_spec.dims > 0 && this.type_spec.refinement_type_clause!=null) throw new Exception("array can not override refinement type");
		
		boolean exist_super_md = false;
		while(true){
			method_decl super_md = cu.search_method(super_class.class_name, this.ident);
			if(super_md != null) exist_super_md = true;
			if(super_md == null || super_md.type_spec.refinement_type_clause==null){//篩型が見つかるまでsuper classを探索
				if(super_class.super_class == null){
					if(this.type_spec.refinement_type_clause!=null && this.type_spec.refinement_type_clause.refinement_type!=null &&
							this.type_spec.refinement_type_clause.refinement_type.type.type.equals("Super_type")){
							if(exist_super_md == false){
								throw new Exception("super class has not method " + this.ident);
							}
						this.type_spec.refinement_type_clause.refinement_type.type.type = this.type_spec.type.type;
					}
					break;//親に篩型がないなら良い
				}else{
					super_class = super_class.super_class;
				}
			}else if(this.type_spec.refinement_type_clause!=null){//篩型を持つ親クラスが見つかった場合、かつ篩型を持っている場合
				if(this.type_spec.refinement_type_clause == super_md.type_spec.refinement_type_clause){//スーパークラスのrefinement_type_clauseと同じインスタンスだったら何もしない
					//何もしない
					break;
				}else if(super_md.type_spec.refinement_type_clause.ident!=null){//親がident
					String rt_name;
					if(this.type_spec.refinement_type_clause.ident!=null){//identの場合は、親が篩型を持っていない、または親もidentかつBaseタイプと同名
						rt_name = this.type_spec.refinement_type_clause.ident;
					}else{//refinement_typeを持っている場合
						rt_name = this.type_spec.refinement_type_clause.refinement_type.type.type;
						if(rt_name.equals("Super_type")){
							this.type_spec.refinement_type_clause.refinement_type.type.type = super_md.type_spec.refinement_type_clause.ident;
							break;
						}
					}
					while(true){
						if(super_md.type_spec.refinement_type_clause.ident.equals(rt_name)){
							break;//同じならよい
						}
						refinement_type rt = cu.search_refinement_type(class_decl.class_name, rt_name);
						if(rt == null){
							throw new Exception(this.ident + ": return value: Base type must be the refinement type that this method has in the super class.");
						}
						rt_name = rt.type.type;
					}
					break;
				}else{//親がrefinement_typeを持っている
					if(this.type_spec.refinement_type_clause.refinement_type!=null){//refinement_typeを持っている場合はSuper_typeが必要
						if(this.type_spec.refinement_type_clause.refinement_type.type.type.equals("Super_type")){
							def_type_clause tmp_def_type = new def_type_clause();
							//名前は一意のはずだが、この篩型名を宣言する怖い人がいるかもしれないので数字をくっつける
							tmp_def_type.ident = class_decl.class_name + "_" + this.ident + "_ret_val_tmp_refinement_type" + (new Random().nextInt(88889) + 11111);
							tmp_def_type.refinement_type = super_md.type_spec.refinement_type_clause.refinement_type;
							class_decl.class_block.def_type_clauses.add(tmp_def_type);
							this.type_spec.refinement_type_clause.refinement_type.type.type = tmp_def_type.ident;
							break;
						}	
					}
					throw new Exception(this.ident + ": return value: Base type must be the refinement type that this method has in the super class.");
				}
			}else{//篩型を持つ親クラスが見つかった場合、かつ篩型を持っていなかった場合
				//親のrefinement_typeのインスタンスをそのまま受け継ぐ
				this.type_spec.refinement_type_clause = super_md.type_spec.refinement_type_clause;
				break;
			}
		}
		
		//引数の型の篩型
		
		//Check_statusの用意
		Check_status cs = new Check_status(cu);
		modifiers m = new modifiers();
		m.is_final = true;
		Field this_field = new Variable(cs.Check_status_share.get_tmp_num(), "this", class_decl.class_name, 0, null, m, null, cs.ctx.mkBool(false));
		this_field.temp_num = 0;
		cs.this_field = this_field;
		
		Field super_this_field = new Variable(cs.Check_status_share.get_tmp_num(), "this", super_class.class_name, 0, null, m, null, cs.ctx.mkBool(false));
		super_this_field.temp_num = 0;
		
		//初期化
		cs.instance_expr = this_field.get_Expr(cs);
		cs.instance_Field = this_field;
		cs.instance_indexs = new ArrayList<IntExpr>();
		
		//各引数のチェック
		for(int i = 0; i < this.formals.param_declarations.size(); i++){
			
			//配列には新しい篩型をつけることができない
			if(this.formals.param_declarations.get(i).type_spec.dims > 0 && this.formals.param_declarations.get(i).type_spec.refinement_type_clause!=null) throw new Exception("array can not override refinement type");
			
			//処理する引数を表すVariable
			modifiers modi = new modifiers();
			modi.is_final = this.formals.param_declarations.get(i).is_final;
			modi.is_privte = false;
			modi.is_spec_public = false;
			//↓のvに篩型をつけなくても動く(制約は追加していくので)
			Variable v = cs.add_variable(this.formals.param_declarations.get(i).ident, this.formals.param_declarations.get(i).type_spec.type.type
					, this.formals.param_declarations.get(i).type_spec.dims, null, modi, cs.ctx.mkBool(true));
			
			v.temp_num=0;
			
			while(true){
				method_decl super_md = cu.search_method(super_class.class_name, this.ident);
				if(super_md == null || super_md.formals.param_declarations.get(i).type_spec.refinement_type_clause==null){//篩型が見つかるまでsuper classを探索
					if(super_class.super_class == null){
						if(this.formals.param_declarations.get(i).type_spec.refinement_type_clause!=null){
							throw new Exception(this.formals.param_declarations.get(i).ident + " in " + class_decl.super_class.class_name + "don't have refinement type");
						}
						break;//親に篩型がないなら良い
					}else{
						super_class = super_class.super_class;
					}
				}else if(this.formals.param_declarations.get(i).type_spec.refinement_type_clause!=null){//篩型を持つ親クラスが見つかった場合、かつ篩型を持っている場合
					refinement_type this_refinement_type,super_refinement_type;
					if(this.formals.param_declarations.get(i).type_spec.refinement_type_clause.refinement_type != null){
						this_refinement_type = this.formals.param_declarations.get(i).type_spec.refinement_type_clause.refinement_type;
					}else{
						this_refinement_type = cu.search_refinement_type(class_decl.class_name, this.formals.param_declarations.get(i).type_spec.refinement_type_clause.ident);
						if(this_refinement_type==null) throw new Exception("can't find refinement type " + this.formals.param_declarations.get(i).type_spec.refinement_type_clause.ident);
					}
					
					if(super_md.formals.param_declarations.get(i).type_spec.refinement_type_clause.refinement_type != null){
						super_refinement_type = super_md.formals.param_declarations.get(i).type_spec.refinement_type_clause.refinement_type;
					}else{
						super_refinement_type = cu.search_refinement_type(class_decl.class_name, super_md.formals.param_declarations.get(i).type_spec.refinement_type_clause.ident);
						if(super_refinement_type==null) throw new Exception("can't find refinement type " + super_md.formals.param_declarations.get(i).type_spec.refinement_type_clause.ident);
					}
					
					this_refinement_type.check_subtype(v, this_field, this_field.get_Expr(cs), new ArrayList<IntExpr>(), super_refinement_type, super_this_field, super_this_field.get_Expr(cs), new ArrayList<IntExpr>(), cs);
					break;
					
				}else{//篩型を持つ親クラスが見つかった場合、かつ篩型を持っていなかった場合
					//親のrefinement_typeのインスタンスをそのまま受け継ぐ
					this.formals.param_declarations.get(i).type_spec.refinement_type_clause = super_md.formals.param_declarations.get(i).type_spec.refinement_type_clause;
					break;
				}
			}

		}
	}
	
	
	public void pure_modifier(){
		if(this.modifiers.is_pure && this.method_specification!=null){
			for(generic_spec_case gsc : this.method_specification.spec_case_seq.generic_spec_cases){
				gsc.simple_spec_body.assignable_nothing = true;
			}
		}
	}

	//このインスタンスの、篩型を持たないcloneを返す
	public method_decl clone_no_refinemet_type(){
		method_decl clone_md = new method_decl();
		clone_md.st = this.st;
		clone_md.method_specification = this.method_specification;
		clone_md.modifiers = this.modifiers;
		clone_md.ident = this.ident;
		clone_md.args_num = this.args_num;
		clone_md.compound_statement = this.compound_statement;
		clone_md.class_type_name = this.class_type_name;
		
		clone_md.type_spec = new type_spec();
		clone_md.type_spec.type = this.type_spec.type;
		clone_md.type_spec.dims = this.type_spec.dims;
		
		clone_md.formals = new formals();
		clone_md.formals.param_declarations = new ArrayList<param_declaration>();
		for(param_declaration pd : this.formals.param_declarations){
			param_declaration clone_pd = new param_declaration();
			clone_pd.is_final = pd.is_final;
			clone_pd.ident = pd.ident;
			clone_pd.type_spec = new type_spec();
			clone_pd.type_spec.type = pd.type_spec.type;
			clone_pd.type_spec.dims = pd.type_spec.dims;
			clone_md.formals.param_declarations.add(clone_pd);
		}
		return clone_md;
	}
}

