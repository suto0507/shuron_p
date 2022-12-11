package system;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.*;

import system.parsers.refinement_type_clause;
import system.parsers.variable_definition;
import system.parsers.class_declaration;
import system.parsers.invariant;
import system.parsers.modifiers;
import system.parsers.refinement_type;

public class Field {
	public int id;
	public int internal_id;//if文とかループとかでクローンしても変わらない。メソッド呼び出しでは、引数のこれは渡したフィールドのものと同じになる
	public int temp_num;
	public String field_name;
	public String type;
	public int dims;
	public refinement_type_clause refinement_type_clause;
	public modifiers modifiers;
	
	//要素が無かったら、何にも代入できない状態であるべき
	public List<Pair<BoolExpr,List<Pair<Expr, List<IntExpr>>>>> assinable_cnst_indexs;//配列の要素に代入できる条件
	
	public boolean new_array;//newとかで生成された配列　
	
	//コンストラクタのfinalの初期化状態
	public boolean final_initialized;
	
	//宣言されたクラスの名前
	public String class_type_name;
	
	//helperメソッドやコンストラクターの中で、２次元以上の配列としてエイリアスした場合
	//対応するclass_exprが含まれていない場合はtrueとして扱う
	public ArrayList<Pair<Expr, BoolExpr>> alias_in_helper_or_consutructor;
	public ArrayList<Pair<Expr, BoolExpr>> alias_2d_in_helper_or_consutructor;
	
	//データフィールド
	public ArrayList<Model_Field> model_fields;
	
	//新しくフィールドを作る時には、alias_in_helper_or_consutructorとalias_2d_in_helper_or_consutructorは同じ引数から初期化する
	public Field(int id, String field_name, String type, int dims, refinement_type_clause refinement_type_clause, modifiers modifiers, String class_type_name, ArrayList<Pair<Expr, BoolExpr>> alias_in_helper_or_consutructor, ArrayList<Model_Field> model_fields) throws Exception{
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
		this.alias_in_helper_or_consutructor = alias_in_helper_or_consutructor;
		this.alias_2d_in_helper_or_consutructor = alias_in_helper_or_consutructor;
		this.model_fields = model_fields;
	}
	
	public Field(){}
	
	
	public Field clone_e() throws Exception{
		Field ret = new Field(this.internal_id, this.field_name, this.type, this.dims, this.refinement_type_clause, this.modifiers, class_type_name, alias_in_helper_or_consutructor, model_fields);
		ret.temp_num = this.temp_num;
		ret.assinable_cnst_indexs = this.assinable_cnst_indexs;
		
		ret.final_initialized = final_initialized;
		ret.alias_2d_in_helper_or_consutructor = this.alias_2d_in_helper_or_consutructor;//新しくフィールドを作る時には、alias_in_helper_or_consutructorとalias_2d_in_helper_or_consutructorは同じ引数から初期化する
		return ret;
	}
	
	
	public Expr get_Expr(Check_status cs) throws Exception{
		if(this.type.equals("int")&&this.dims==0){
			String ret = field_name + "_temp_" + this.id + "_"  + this.temp_num;
			return cs.ctx.mkArrayConst(ret, cs.ctx.mkUninterpretedSort("Ref"), cs.ctx.mkIntSort());
		}else if(this.type.equals("boolean")&&this.dims==0){
			String ret = field_name + "_temp_" + this.id + "_"  + this.temp_num;
			return cs.ctx.mkArrayConst(ret, cs.ctx.mkUninterpretedSort("Ref"), cs.ctx.mkBoolSort());
		}else if(this.type.equals("void")){
			//throw new Exception("void variable ?");
			return null;
		}else if(this.dims==0){
			//クラス
			String ret = field_name + "_temp_" + this.id + "_"  + this.temp_num;
			return cs.ctx.mkArrayConst(ret, cs.ctx.mkUninterpretedSort("Ref"), cs.ctx.mkUninterpretedSort("Ref"));
		}else if(this.dims>0){
			String ret = field_name + "_temp_" + this.id + "_"  + this.temp_num;
			return cs.ctx.mkArrayConst(ret, cs.ctx.mkUninterpretedSort("Ref"), cs.ctx.mkUninterpretedSort("ArrayRef"));
		}
		throw new Exception("unexpect variable");
	}
	
	

	
	public Expr get_Expr_assign(Check_status cs) throws Exception{
		this.temp_num ++;
		Expr ex =  this.get_Expr(cs);
		this.temp_num --;
		return ex;
	}
	
	//値を更新する
	//配列の要素への代入でないならば、tmp_numを更新する
	public void assign_value(Expr class_expr, ArrayList<IntExpr> indexs, Expr value, Check_status cs) throws Exception{
		
		if(indexs.size()>0){
			Expr expr = cs.ctx.mkSelect(this.get_Expr(cs), class_expr);
			for(int i = 0; i < indexs.size()-1; i++){
				expr = cs.array_arrayref.index_access_array(expr, indexs.get(i), cs);
			}
			Array array = null;
			if(indexs.size()>this.dims){
				array = cs.array_arrayref;
			}else{
				if(this.type.equals("int")){
					array = cs.array_int;
				}else if(this.type.equals("boolean")){
					array = cs.array_int;
				}else{
					array = cs.array_ref;
				}
			}
			array.update_array(class_expr, indexs.get(indexs.size()-1), value, cs);
		}else{
			cs.add_constraint(cs.ctx.mkEq(this.get_Expr_assign(cs), cs.ctx.mkStore(this.get_Expr(cs), class_expr, value)));
			this.tmp_plus_with_data_group(class_expr, cs);
		}
	}
	
	
	//自分と同じ型のフレッシュなExprを返す
	public Expr get_fresh_value(Check_status cs) throws Exception{
		if(this.type.equals("int")&&this.dims==0){
			String ret = "tmpInt" + cs.Check_status_share.get_tmp_num();
			return cs.ctx.mkIntConst(ret);
		}else if(this.type.equals("boolean")&&this.dims==0){
			String ret = "tmpBool" + cs.Check_status_share.get_tmp_num();
			return cs.ctx.mkBoolConst(ret);
		}else if(this.type.equals("void")){
			//throw new Exception("void variable ?");
			return null;
		}else if(this.dims==0){
			//クラス
			String ret = "tmpRef" + cs.Check_status_share.get_tmp_num();;
			return cs.ctx.mkConst(ret, cs.ctx.mkUninterpretedSort("Ref"));
		}else if(this.type.equals("int")&&this.dims==1){ //配列
			String ret = "tmpIntArray" + cs.Check_status_share.get_tmp_num();
			return cs.ctx.mkArrayConst(ret, cs.ctx.mkIntSort(), cs.ctx.mkIntSort());
		}else if(this.type.equals("boolean")&&this.dims==1){
			String ret = "tmpBoolArray" + cs.Check_status_share.get_tmp_num();
			return cs.ctx.mkArrayConst(ret, cs.ctx.mkIntSort(), cs.ctx.mkBoolSort());
		}else if(this.dims==1){
			String ret = "tmpRefArray" + cs.Check_status_share.get_tmp_num();;
			return cs.ctx.mkArrayConst(ret, cs.ctx.mkIntSort(), cs.ctx.mkUninterpretedSort("Ref"));
		}
		throw new Exception("unexpect variable");
	}
	
	
	public boolean equals(Field f, Check_status cs){
		//if(this.field_name.equals(f.field_name) && ((this.class_object==null&&f.class_object==null) || this.class_object.equals(f.class_object, cs) ) ){
		if(this.internal_id == f.internal_id){
			return true;
		}
		
		return false;
	}
	
	public void tmp_plus(Check_status cs) throws Exception{
		if(this.modifiers!=null && this.modifiers.is_final && this.dims == 0){
			return;
		}
		this.temp_num++;
	}
	
	//tmp_numをインクリメントし、データグループの値も更新する　tmp_numのインクリメントした結果との制約は、データグループのモデルフィールドに対してだけ追加され、このフィールドに関しては制約は追加されない
	public void tmp_plus_with_data_group(Check_status cs) throws Exception{
		if(this.modifiers!=null && this.modifiers.is_final && this.dims == 0){
			return;
		}
		this.temp_num++;
		for(Model_Field mf : this.model_fields){
			mf.tmp_plus_with_data_group(cs);
		}
	}
	
	public void tmp_plus_with_data_group(Expr class_expr, Check_status cs) throws Exception{
		if(this.modifiers!=null && this.modifiers.is_final && this.dims == 0){
			return;
		}
		this.temp_num++;
		for(Model_Field mf : this.model_fields){
			mf.tmp_plus_with_data_group(class_expr, cs);
		}
	}
	
	public void tmp_plus_with_data_group(BoolExpr condition, Check_status cs) throws Exception{//conditionは値を変更する条件
		if(this.modifiers!=null && this.modifiers.is_final && this.dims == 0){
			return;
		}
		this.temp_num++;
		for(Model_Field mf : this.model_fields){
			mf.tmp_plus_with_data_group(condition, cs);
		}
	}
	
	public void tmp_plus_with_data_group(Expr class_expr, BoolExpr condition, Check_status cs) throws Exception{//conditionは値を変更する条件
		if(this.modifiers!=null && this.modifiers.is_final && this.dims == 0){
			return;
		}
		this.temp_num++;
		for(Model_Field mf : this.model_fields){
			mf.tmp_plus_with_data_group(class_expr, condition, cs);
		}
	}
	
	
	public BoolExpr assign_index_expr(Expr class_expr, List<IntExpr> index_expr, Check_status cs){
		BoolExpr equal_cnsts = cs.ctx.mkBool(false);
		BoolExpr not_equal_cnsts = cs.ctx.mkBool(true);
		for(Pair<BoolExpr,List<Pair<Expr, List<IntExpr>>>> assinable_cnst_index :assinable_cnst_indexs){
			BoolExpr equal = cs.ctx.mkBool(false);
			for(Pair<Expr, List<IntExpr>> expr_index : assinable_cnst_index.snd){
				if(!expr_index.fst.equals(class_expr))break;//同じclass_exprだけ考える
				if(expr_index.snd.size()==0 && expr_index.snd.size() == index_expr.size()){
					equal = cs.ctx.mkBool(true);
				}else if(expr_index.snd.size()>0 && expr_index.snd.size() == index_expr.size()){
					BoolExpr index_equal = null;
					for(int i = 0; i<expr_index.snd.size(); i++){
						if(index_equal == null){
							index_equal = cs.ctx.mkEq(index_expr.get(i), expr_index.snd.get(i));
						}else{
							index_equal = cs.ctx.mkAnd(index_equal, cs.ctx.mkEq(index_expr.get(i), expr_index.snd.get(i)));
						}
					}
					equal = cs.ctx.mkOr(equal, index_equal);
				}
			}
			BoolExpr not_equal = cs.ctx.mkNot(equal);
			equal_cnsts = cs.ctx.mkOr(equal_cnsts, cs.ctx.mkAnd(equal, assinable_cnst_index.fst));
			not_equal_cnsts = cs.ctx.mkAnd(not_equal_cnsts, cs.ctx.mkImplies(not_equal, cs.ctx.mkNot(assinable_cnst_index.fst)));
		}
		
		return cs.ctx.mkAnd(equal_cnsts, not_equal_cnsts);
	}
	
	
	public void add_refinement_constraint(Check_status cs, Expr class_Expr, ArrayList<IntExpr> indexs) throws Exception{
		add_refinement_constraint(cs, class_Expr, indexs, false);
	}
	
	//class_Fieldは篩型を持つフィールド、変数を持つクラス
	//add_onceは、一度だけhelperやin_cnstructorの制約を無視して篩型の述語をaddする//つまり、篩型の述語の中に記述されたフィールドの篩型は無視したい時に使う
	public void add_refinement_constraint(Check_status cs, Expr class_Expr, ArrayList<IntExpr> indexs, boolean add_once) throws Exception{
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
		
		if(this.refinement_type_clause!=null){
			Expr ex = null;
			if(this instanceof Variable){
				ex = get_Expr(cs);
			}else{
				ex = cs.ctx.mkSelect(get_Expr(cs), class_Expr);
			}
			
			if(this.refinement_type_clause.refinement_type!=null){
				this.refinement_type_clause.refinement_type.add_refinement_constraint(cs, this, ex, class_Expr, indexs, add_once);
			}else if(this.refinement_type_clause.ident!=null){
				refinement_type rt = cs.search_refinement_type(this.class_type_name, this.refinement_type_clause.ident);
				if(rt!=null){
					rt.add_refinement_constraint(cs, this, ex, class_Expr, indexs, add_once);
				}else{
	                throw new Exception("can't find refinement type " + this.refinement_type_clause.ident);
	            }
			}
		}
		
		for(Model_Field mf : this.model_fields){
			mf.add_refinement_constraint(cs, class_Expr, indexs, add_once);
		}
		
		if(root_check){
			cs.checked_refinement_type_field = new ArrayList<Pair<Field, Expr>>();
		}
		
	}
	
	public void assert_refinement(Check_status cs, Expr class_Expr, ArrayList<IntExpr> indexs) throws Exception{
		
		boolean root_check = false;
		if(cs.checked_refinement_type_field.size()==0){
			root_check = true;
		}else{
			for(Pair<Field, Expr> checked_field : cs.checked_refinement_type_field){
				if(checked_field.fst == this && checked_field.snd == class_Expr){
					return;
				}
			}
		}
		cs.checked_refinement_type_field.add(new Pair(this, class_Expr));
		
		if(this.refinement_type_clause!=null){
			Expr ex = null;
			if(this instanceof Variable){
				ex = get_Expr(cs);
			}else{
				ex = cs.ctx.mkSelect(get_Expr(cs), class_Expr);
			}
			
			if(this.refinement_type_clause.refinement_type!=null){
				this.refinement_type_clause.refinement_type.assert_refinement(cs, this, ex, class_Expr, indexs);
			}else if(this.refinement_type_clause.ident!=null){
				refinement_type rt = cs.search_refinement_type(this.class_type_name, this.refinement_type_clause.ident);
				if(rt!=null){
					rt.assert_refinement(cs, this, ex, class_Expr, indexs);
				}else{
	                throw new Exception("can't find refinement type " + this.refinement_type_clause.ident);
	            }
			}
		}
		
		for(Model_Field mf : this.model_fields){
			mf.assert_refinement(cs, class_Expr, indexs);
		}
		if(root_check){
			cs.checked_refinement_type_field = new ArrayList<Pair<Field, Expr>>();
		}
		
	}
	
	public boolean hava_refinement_type(){
		if(this.refinement_type_clause!=null){
			return true;
		}
		for(Model_Field mf : this.model_fields){
			if(mf.hava_refinement_type()) return true;
		}
		return false;
	}
	
	//篩型が配列の要素に言及しているかどうか
	public boolean have_index_access(Check_status cs) throws Exception{
		boolean have = false;
		
		if(this.refinement_type_clause.refinement_type!=null){
			have = have || this.refinement_type_clause.refinement_type.have_index_access(cs);
		}else if(this.refinement_type_clause.ident!=null){
			refinement_type rt = cs.search_refinement_type(this.class_type_name, this.refinement_type_clause.ident);
			if(rt!=null){
				have = have || rt.have_index_access(cs);
			}else{
                throw new Exception("can't find refinement type " + this.refinement_type_clause.ident);
            }
		}
		for(Model_Field mf : this.model_fields){
			have = have || mf.have_index_access(cs);
		}

		return have;
	}
	
	//finalのフィールドにinitializerが付いていた場合
	public void set_initialize(Expr class_expr, Check_status cs) throws Exception{
		variable_definition vd = cs.Check_status_share.compilation_unit.search_field(class_type_name, this.field_name, false);
		//initializerが付いていた場合
		if(vd.variable_decls.initializer!=null && vd.modifiers.is_final){
			Check_return init_Expr = vd.variable_decls.initializer.check(cs);
			Expr field_Expr = cs.ctx.mkSelect(this.get_Expr(cs), class_expr);
			cs.add_constraint(cs.ctx.mkEq(field_Expr, init_Expr.expr));
		}
	}
	
	//このフィールドが持つinvariantに関しての制約を返す
	//配列に関しては、任意のインデックスに対する制約
	public BoolExpr invariants_expr(Check_status cs) throws Exception{
		BoolExpr ret_expr =  cs.ctx.mkBool(true);
		class_declaration cd = cs.Check_status_share.compilation_unit.search_class(type);
		if(cd==null) throw new Exception("cannot find class " + type);
		if(cd.class_block.invariants.size()!=0){
			Expr pre_instance_expr = cs.instance_expr;
			Field pre_instance_Field = cs.instance_Field;
			ArrayList<IntExpr> pre_instance_indexs = cs.instance_indexs;
			
			BoolExpr pre_pathcondition = cs.pathcondition;
			
			Pair<Expr, ArrayList<IntExpr>> expr_indexs =  fresh_index_full_expr(cs);
			cs.instance_expr = expr_indexs.fst;
			cs.instance_Field = this;
			cs.instance_indexs = expr_indexs.snd;
			
			for(invariant invariant : cd.class_block.invariants){
				if(invariant.is_private==true){//可視性が同じものしか使えない
					cs.ban_default_visibility = true;
				}else{
					cs.ban_private_visibility = true;
				}
				
				Expr invariant_expr = invariant.check(cs);
				
				cs.ban_default_visibility = false;
				cs.ban_private_visibility = false;
				
				if(expr_indexs.snd.size() > 0){
					IntExpr[] tmps = new IntExpr[expr_indexs.snd.size()];
					for(int i = 0; i < expr_indexs.snd.size(); i++){
						tmps[i] = expr_indexs.snd.get(i);
					}
					invariant_expr = cs.ctx.mkForall(tmps, invariant_expr, 1, null, null, null, null);
				}

				cs.add_path_condition_tmp((BoolExpr) invariant_expr);
				ret_expr = cs.ctx.mkAnd(ret_expr, invariant_expr);
			}
			
			
			cs.instance_expr = pre_instance_expr;
			cs.instance_Field = pre_instance_Field;
			cs.instance_indexs = pre_instance_indexs;

			cs.pathcondition = pre_pathcondition;
		}
		
		return ret_expr;
	}
	

}
