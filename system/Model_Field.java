package system;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;

import system.parsers.class_declaration;
import system.parsers.method_decl;
import system.parsers.modifiers;
import system.parsers.refinement_type;
import system.parsers.refinement_type_clause;
import system.parsers.spec_expression;
import system.parsers.represents_clause;

public class Model_Field extends Field{
	
	represents_clause represents_clause;
	
	//新しくフィールドを作る時には、alias_1d_in_helperとalias_2d_in_helper_or_consutructorは同じ引数から初期化する
	public Model_Field(int id, String field_name, String type, int dims, refinement_type_clause refinement_type_clause, modifiers modifiers, String class_type_name, BoolExpr alias_1d_in_helper, ArrayList<Model_Field> model_fields) throws Exception{
		this.id = id;
		this.internal_id = id;
		this.temp_num = 0;
		this.field_name = field_name;
		this.type = type;
		this.dims = dims;
		this.refinement_type_clause = refinement_type_clause;
		this.modifiers = modifiers;
		this.assinable_cnst_indexs = new ArrayList<Pair<BoolExpr,List<Pair<Expr, List<IntExpr>>>>>();
		this.class_type_name = class_type_name;
		this.new_array = false;
		this.alias_1d_in_helper = alias_1d_in_helper;
		this.alias_in_consutructor_or_2d_in_helper = alias_1d_in_helper;
		this.model_fields = model_fields;
		if(this.modifiers.is_final){
			throw new Exception("model fields can't be modified with final");
		}
	}
	
	public Model_Field clone_e() throws Exception{
		Model_Field ret = new Model_Field(this.internal_id, this.field_name, this.type, this.dims, this.refinement_type_clause, this.modifiers, class_type_name, alias_1d_in_helper, model_fields);
		ret.temp_num = this.temp_num;
		ret.assinable_cnst_indexs = this.assinable_cnst_indexs;
		
		ret.final_initialized = final_initialized;
		ret.alias_in_consutructor_or_2d_in_helper = this.alias_in_consutructor_or_2d_in_helper;//新しくフィールドを作る時には、alias_in_helper_or_consutructorとalias_2d_in_helper_or_consutructorは同じ引数から初期化する
		
		ret.represents_clause = this.represents_clause;
		return ret;
	}
	
	public void set_repersents(Check_status cs){
		represents_clause rc = cs.Check_status_share.compilation_unit.search_represents_clause(this.class_type_name, this.field_name, cs.this_field.type);
		represents_clause = rc;
	}
	
	//class_exprを渡すこと場合、representsに関する制約も追加する
	public Expr get_Expr(Expr class_expr, Check_status cs) throws Exception{
		Expr expr = get_Expr(cs);
		if(this.represents_clause!=null){
			Expr full_expr = cs.ctx.mkSelect(expr, class_expr);
			Expr pre_instance_expr = cs.instance_expr;
			String pre_instance_class_name = cs.instance_class_name;
			boolean pre_ban_private_visibility = cs.ban_private_visibility;
			boolean pre_ban_default_visibility = cs.ban_default_visibility;
			
			cs.instance_expr = class_expr;
			cs.instance_class_name = this.class_type_name;
			//可視性について
			cs.ban_default_visibility = false;
			if(this.represents_clause.is_private){
				cs.ban_private_visibility = false;
			}else{
				cs.ban_private_visibility = true;
			}
			
			cs.add_constraint(cs.ctx.mkEq(full_expr, this.represents_clause.spec_expression.check(cs).expr));
			
			cs.instance_expr = pre_instance_expr;
			cs.instance_class_name = pre_instance_class_name;
			cs.ban_private_visibility = pre_ban_private_visibility;
			cs.ban_default_visibility = pre_ban_default_visibility;
		}
		
		return expr;
	}
	
	public void tmp_plus_with_data_group(Expr class_expr, Check_status cs) throws Exception{
		if(this.modifiers!=null && this.modifiers.is_final && this.dims == 0){
			return;
		}
		
		cs.add_constraint(cs.ctx.mkEq(this.get_Expr_assign(cs), cs.ctx.mkStore(this.get_Expr(cs), class_expr, this.get_fresh_value(cs))));
		
		this.temp_num++;
		for(Model_Field mf : this.model_fields){
			mf.tmp_plus_with_data_group(class_expr, cs);
		}
	}
	
	public void tmp_plus_with_data_group(BoolExpr condition, Check_status cs) throws Exception{
		if(this.modifiers!=null && this.modifiers.is_final && this.dims == 0){
			return;
		}
		
		Expr expr = this.get_Expr(cs);
		cs.add_constraint(cs.ctx.mkEq(this.get_Expr_assign(cs), cs.ctx.mkITE(condition, cs.ctx.mkConst("fresh_value_" + cs.Check_status_share.get_tmp_num(), expr.getSort()), expr)));
		
		this.temp_num++;
		for(Model_Field mf : this.model_fields){
			mf.tmp_plus_with_data_group(condition, cs);
		}
	}
	
	public void tmp_plus_with_data_group(Expr class_expr, BoolExpr condition, Check_status cs) throws Exception{
		if(this.modifiers!=null && this.modifiers.is_final && this.dims == 0){
			return;
		}
		
		cs.add_constraint(cs.ctx.mkEq(this.get_Expr_assign(cs), cs.ctx.mkITE(condition, cs.ctx.mkStore(this.get_Expr(cs), class_expr, this.get_fresh_value(cs)), this.get_Expr(cs))));
		
		this.temp_num++;
		for(Model_Field mf : this.model_fields){
			mf.tmp_plus_with_data_group(class_expr, condition, cs);
		}
	}
	
	//class_Fieldは篩型を持つフィールド、変数を持つクラス
	//add_onceは、一度だけhelperやin_cnstructorの制約を無視して篩型の述語をaddする//つまり、篩型の述語の中に記述されたフィールドの篩型は無視したい時に使う
	public void add_refinement_constraint(Check_status cs, Expr class_Expr, boolean add_once) throws Exception{
		boolean root_check = false;
		if(cs.checked_refinement_type_field.size()==0){
			root_check = true;
		}else{
			for(Pair<Field, Expr> checked_field : cs.checked_refinement_type_field){
				if(checked_field.fst == this && checked_field.snd.equals(class_Expr)){
					return;
				}
			}
		}
		cs.checked_refinement_type_field.add(new Pair(this, class_Expr));
		
		Expr ex = cs.ctx.mkSelect(get_Expr(class_Expr, cs), class_Expr);//ここでget_Exprでrepresentsの制約を追加する
		
		if(this.refinement_type_clause!=null){
			if(this.refinement_type_clause.refinement_type!=null){
				this.refinement_type_clause.refinement_type.add_refinement_constraint(cs, this, ex, class_Expr, add_once);
			}else if(this.refinement_type_clause.ident!=null){
				refinement_type rt = cs.search_refinement_type(this.class_type_name, this.refinement_type_clause.ident);
				if(rt!=null){
					rt.add_refinement_constraint(cs, this, ex, class_Expr, add_once);
				}else{
	                throw new Exception("can't find refinement type " + this.refinement_type_clause.ident);
	            }
			}
		}
		
		for(Model_Field mf : this.model_fields){
			mf.add_refinement_constraint(cs, class_Expr, add_once);
		}
		
		if(root_check){
			cs.checked_refinement_type_field = new ArrayList<Pair<Field, Expr>>();
		}
		
	}
	
	public void assert_refinement(Check_status cs, Expr class_Expr) throws Exception{
		
		boolean root_check = false;
		if(cs.checked_refinement_type_field.size()==0){
			root_check = true;
		}else{
			for(Pair<Field, Expr> checked_field : cs.checked_refinement_type_field){
				if(checked_field.fst == this && checked_field.snd.equals(class_Expr)){
					return;
				}
			}
		}
		cs.checked_refinement_type_field.add(new Pair(this, class_Expr));
		
		Expr ex = cs.ctx.mkSelect(get_Expr(class_Expr, cs), class_Expr);//ここでget_Exprでrepresentsの制約を追加する
		
		if(this.refinement_type_clause!=null){
			if(this.refinement_type_clause.refinement_type!=null){
				this.refinement_type_clause.refinement_type.assert_refinement(cs, this, ex, class_Expr);
			}else if(this.refinement_type_clause.ident!=null){
				refinement_type rt = cs.search_refinement_type(this.class_type_name, this.refinement_type_clause.ident);
				if(rt!=null){
					rt.assert_refinement(cs, this, ex, class_Expr);
				}else{
	                throw new Exception("can't find refinement type " + this.refinement_type_clause.ident);
	            }
			}
		}
		
		for(Model_Field mf : this.model_fields){
			mf.assert_refinement(cs, class_Expr);
		}
		
		if(root_check){
			cs.checked_refinement_type_field = new ArrayList<Pair<Field, Expr>>();
		}
		
	}
	
	
}
