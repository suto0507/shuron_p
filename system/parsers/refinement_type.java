package system.parsers;


import java.util.ArrayList;

import com.microsoft.z3.*;

import system.Check_status;
import system.Field;
import system.Pair;
import system.Parser;
import system.Parser_status;
import system.Source;
import system.Variable;

public class refinement_type implements Parser<String>{
	type type;
	int dims;
	String ident;
	predicate predicate;
	public String class_type_name;
	
	public String parse(Source s,Parser_status ps)throws Exception{
		String st;
		dims = 0;
		st = new string("{").parse(s,ps);
		st = st + new spaces().parse(s,ps);
		this.type = new type();
		st = st + type.parse(s,ps);
		st = st + new spaces().parse(s,ps);
		
		Source s_backup = s.clone();
		try{
			dims ds = new dims();
			st += ds.parse(s, ps);
			this.dims = ds.dims;
			st = st + new spaces().parse(s,ps);
		}catch (Exception e){
			s.revert(s_backup);
		}
		
		this.ident = new ident().parse(s,ps);
		st = st + this.ident;
		st = st + new spaces().parse(s,ps);
		st = st + new string("|").parse(s,ps);
		st = st + new spaces().parse(s,ps);
		predicate = new predicate();
		st = st + this.predicate.parse(s,ps);
		st = st + new spaces().parse(s,ps);
		st = st + new string("}").parse(s,ps);

		this.class_type_name = ps.class_type_name;
		
		return st;
	}
	
	//class_Fieldは篩型を持つフィールド、変数を持つクラス
	public void assert_refinement(Check_status cs, Field refined_Field, Expr refined_Expr, Field class_Field, Expr class_Expr, ArrayList<IntExpr> indexs) throws Exception{
		
		if(cs.in_helper && !(refined_Field instanceof Variable))return;//helperメソッドの中では、フィールドの篩型が成り立つことを前提とできない
		
		//バックアップ
	    Field pre_refined_Field = cs.refined_Field;
	    Expr pre_refined_Expr = cs.refined_Expr;
	    String pre_refinement_type_value = cs.refinement_type_value;
	    boolean pre_in_refinement_predicate = cs.in_refinement_predicate;
	    Expr pre_instance_expr = cs.instance_expr;
		Field pre_instance_Field = cs.instance_Field;
		ArrayList<IntExpr> pre_instance_indexs = cs.instance_indexs;
		boolean pre_use_only_helper_method = cs.use_only_helper_method;

	    //篩型の処理のための事前準備
		cs.instance_expr = class_Expr;
		cs.instance_Field = class_Field;
		cs.instance_indexs = (ArrayList<IntExpr>) indexs.clone();
		
		cs.in_refinement_predicate = true;
		cs.refinement_type_value = ident;
		cs.refined_Field = refined_Field;
		cs.refined_Expr = refined_Expr;
		cs.use_only_helper_method = true;
		
		String pre_class_type_name = cs.instance_Field.type;
		cs.instance_Field.type = this.class_type_name;
		
		BoolExpr expr = this.predicate.check(cs);
		cs.instance_Field.type = pre_class_type_name;
		
		cs.assert_constraint(expr);
		cs.in_refinement_predicate = false;
		if(this.type.type.equals("boolean")){
			if(!(refined_Field.type.equals("boolean"))){
				//System.out.println("this variable is not boolean");
				throw new Exception("this variable is not boolean");
			}
		}else if(this.type.type.equals("int")){
			if(!(refined_Field.type.equals("int"))){
				//System.out.println("this variable is not int");
				throw new Exception("this variable is not int");
			}
		}else if(this.type.type.equals(refined_Field.type)){
			//クラス型
		}else{
			refinement_type rt = cs.search_refinement_type(this.class_type_name, type.type);
			if(rt!=null){
				rt.assert_refinement(cs, refined_Field, refined_Expr, class_Field, class_Expr, indexs);
			}else{
				throw new Exception("can't find refinement type " + type.type);
			}
		}
		
		cs.refined_Expr = pre_refined_Expr;
	    cs.refined_Field = pre_refined_Field;
	    cs.refinement_type_value = pre_refinement_type_value;
	    cs.in_refinement_predicate = pre_in_refinement_predicate;
	    cs.instance_expr = pre_instance_expr;
		cs.instance_Field = pre_instance_Field;
		cs.instance_indexs = pre_instance_indexs;
		cs.use_only_helper_method = pre_use_only_helper_method;
	}
	
	
	//class_Fieldは篩型を持つフィールド、変数を持つクラス
	public void add_refinement_constraint(Check_status cs, Field refined_Field, Expr refined_Expr, Field class_Field, Expr class_Expr, ArrayList<IntExpr> indexs) throws Exception{
		
		if(cs.in_constructor){//コンストラクタ内では篩型は保証されない
			return;
		}
		
		//バックアップ
	    Field pre_refined_Field = cs.refined_Field;
	    Expr pre_refined_Expr = cs.refined_Expr;
	    String pre_refinement_type_value = cs.refinement_type_value;
	    boolean pre_in_refinement_predicate = cs.in_refinement_predicate;
	    Expr pre_instance_expr = cs.instance_expr;
		Field pre_instance_Field = cs.instance_Field;
		ArrayList<IntExpr> pre_instance_indexs = cs.instance_indexs;

	    //篩型の処理のための事前準備
		cs.instance_expr = class_Expr;
		cs.instance_Field = class_Field;
		cs.instance_indexs = (ArrayList<IntExpr>) indexs.clone();
		
		cs.in_refinement_predicate = true;
		cs.refinement_type_value = ident;
		cs.refined_Field = refined_Field;
		cs.refined_Expr = refined_Expr;
		
		String pre_class_type_name = cs.instance_Field.type;
		cs.instance_Field.type = this.class_type_name;
		
		BoolExpr expr = this.predicate.check(cs);
		
		cs.instance_Field.type = pre_class_type_name;
		
		cs.add_constraint(expr);
		cs.in_refinement_predicate = false;
		if(this.type.type.equals("boolean")){
			if(!(refined_Field.type.equals("boolean"))){
				//System.out.println("this variable is not boolean");
				throw new Exception("this variable is not boolean");
			}
		}else if(this.type.type.equals("int")){
			if(!(refined_Field.type.equals("int"))){
				//System.out.println("this variable is not int");
				throw new Exception("this variable is not int");
			}
		}else if(this.type.type.equals(refined_Field.type)){
			//クラス型
		}else{
			refinement_type rt = cs.search_refinement_type(this.class_type_name, type.type);
			if(rt!=null){
				rt.add_refinement_constraint(cs, refined_Field, refined_Expr, class_Field, class_Expr, indexs);
			}else{
				throw new Exception("can't find refinement type " + type.type);
			}
		}
		
		cs.refined_Expr = pre_refined_Expr;
	    cs.refined_Field = pre_refined_Field;
	    cs.refinement_type_value = pre_refinement_type_value;
	    cs.in_refinement_predicate = pre_in_refinement_predicate;
	    cs.instance_expr = pre_instance_expr;
		cs.instance_Field = pre_instance_Field;
		cs.instance_indexs = pre_instance_indexs;
	}
	
	// subtype <= this
	// addしてしまうので、必要であればcs.solver.push()をしておく必要はある。
	public void check_subtype(Variable refined_variable, Field class_Field, Expr class_Expr, ArrayList<IntExpr> indexs, refinement_type sub_type, Field sub_type_class_Field, Expr sub_type_class_Expr, ArrayList<IntExpr> sub_type_indexs, Check_status cs) throws Exception{
		cs.in_refinement_predicate = true;
		
		
		
		sub_type.add_refinement_constraint(cs, refined_variable, refined_variable.get_Expr(cs), class_Field , class_Expr, indexs);
		this.assert_refinement(cs, refined_variable, refined_variable.get_Expr(cs), class_Field , class_Expr, indexs);
		
		
		
		//BoolExpr expr = cs.ctx.mkImplies(sub_expr,this_expr);
		//cs.assert_constraint(expr);
		cs.in_refinement_predicate = false;
	}
	
	//使わないかも
	public Pair<String, Integer> base_type(compilation_unit cu) throws Exception{
		if(this.type.type.equals("boolean") || this.type.type.equals("int") || cu.search_class(this.type.type)!=null){
			return new Pair(this.type.type, this.dims);
		}else{
			
			
			refinement_type rt = cu.search_refinement_type(this.class_type_name, type.type);
			if(rt!=null){
				return rt.base_type(cu);
			}else{
				throw new Exception("can't find base type " + type.type);
			}
		}
	}
	
	
	//配列の篩型同士の比較
	//assign_field_exprは長さに関する制約で使う
	public void equal_predicate(ArrayList<IntExpr> indexs, Expr assign_field_expr, Field class_Field, Expr class_Expr, refinement_type comparative_refinement_type, ArrayList<IntExpr> comparative_indexs, Expr comparative_assign_field_expr, Field comparative_class_Field, Expr comparative_class_Expr, Check_status cs) throws Exception{
		String type = this.base_type(cs.Check_status_share.compilation_unit).fst;
		int dims = this.base_type(cs.Check_status_share.compilation_unit).snd;
		Variable v = new Variable(cs.Check_status_share.get_tmp_num(), "tmp", type, dims, null, new modifiers(), class_Field );
		v.temp_num++;
		
		String  comparative_type = comparative_refinement_type.base_type(cs.Check_status_share.compilation_unit).fst;
		int  comparative_dims = comparative_refinement_type.base_type(cs.Check_status_share.compilation_unit).snd;
		Variable comparative_v = new Variable(cs.Check_status_share.get_tmp_num(), "comparative_tmp", comparative_type, comparative_dims, null, new modifiers(), comparative_class_Field );
		comparative_v.temp_num++;
		
		ArrayList<IntExpr> v_indexs = new ArrayList<IntExpr>(indexs.subList(class_Field.dims_sum(), indexs.size()));
		ArrayList<IntExpr> comparative_v_indexs = new ArrayList<IntExpr>(comparative_indexs.subList(comparative_class_Field.dims_sum(), comparative_indexs.size()));
		
		
		
		//エイリアスする部分に関する配列の長さは変わらない
		String array_type;
		if(type.equals("int")){
			array_type = "int";
		}else if(type.equals("boolean")){
			array_type = "boolean";
		}else{
			array_type = "ref";
		}
		for(int i = 0; i <= v_indexs.size(); i++){
			Expr ex1 = assign_field_expr;
			if(i>0){
				for(IntExpr index : v_indexs.subList(0, i)){
					ex1 = cs.ctx.mkSelect(ex1, index);
				}
			}
			Expr ex2 = v.get_full_Expr(new ArrayList<IntExpr>(indexs.subList(class_Field.dims_sum(), class_Field.dims_sum() + i)), cs);
			
			//長さに関する制約
			int array_dim = v.dims_sum() - i;
			
			IntExpr length1 = (IntExpr) cs.ctx.mkSelect(cs.ctx.mkArrayConst("length_" + array_dim + "d_" + array_type, ex1.getSort(), cs.ctx.mkIntSort()), ex1);
			IntExpr length2 = (IntExpr) cs.ctx.mkSelect(cs.ctx.mkArrayConst("length_" + array_dim + "d_" + array_type, ex2.getSort(), cs.ctx.mkIntSort()), ex2);
			cs.add_constraint(cs.ctx.mkEq(length1, length2));
		}
		for(int i = 0; i <= comparative_v_indexs.size(); i++){
			Expr ex1 = comparative_assign_field_expr;
			if(i>0){
				for(IntExpr index : comparative_v_indexs.subList(0, i)){
					ex1 = cs.ctx.mkSelect(ex1, index);
				}
			}
			Expr ex2 = comparative_v.get_full_Expr(new ArrayList<IntExpr>(comparative_indexs.subList(comparative_class_Field.dims_sum(), comparative_class_Field.dims_sum() + i)), cs);
			
			//長さに関する制約
			int array_dim = comparative_v.dims_sum() - i;
			IntExpr length1 = (IntExpr) cs.ctx.mkSelect(cs.ctx.mkArrayConst("length_" + array_dim + "d_" + array_type, ex1.getSort(), cs.ctx.mkIntSort()), ex1);
			IntExpr length2 = (IntExpr) cs.ctx.mkSelect(cs.ctx.mkArrayConst("length_" + array_dim + "d_" + array_type, ex2.getSort(), cs.ctx.mkIntSort()), ex2);
			cs.add_constraint(cs.ctx.mkEq(length1, length2));
		}
		
		//この型を持つ変数の値が変わっても他方の述語は満たされる
		System.out.println("check refinement type equality 1");
		cs.solver.push();
		
		this.add_refinement_constraint(cs, v, v.get_Expr(cs), class_Field, class_Expr, new ArrayList<IntExpr>(indexs.subList(0, class_Field.dims_sum())));
		
		if(comparative_v_indexs.size() == 0){
			cs.add_constraint(cs.ctx.mkEq(comparative_v.get_Expr(cs), v.get_full_Expr((ArrayList<IntExpr>) v_indexs.clone(), cs)));
			
			comparative_refinement_type.assert_refinement(cs, comparative_v, comparative_v.get_Expr(cs), comparative_class_Field, comparative_class_Expr, new ArrayList<IntExpr>(comparative_indexs.subList(0, comparative_class_Field.dims_sum())));
		}else{
			comparative_refinement_type.add_refinement_constraint(cs, comparative_v, comparative_v.get_Expr(cs), comparative_class_Field, comparative_class_Expr, new ArrayList<IntExpr>(comparative_indexs.subList(0, comparative_class_Field.dims_sum())));
			cs.add_constraint(cs.ctx.mkEq(comparative_v.get_Expr_assign(cs), comparative_v.assign_value(comparative_v_indexs, v.get_full_Expr((ArrayList<IntExpr>) v_indexs.clone(), cs), cs)));
			
			comparative_refinement_type.assert_refinement(cs, comparative_v, comparative_v.get_Expr_assign(cs), comparative_class_Field, comparative_class_Expr, new ArrayList<IntExpr>(comparative_indexs.subList(0, comparative_class_Field.dims_sum())));
			//comparative_v.temp_num++;
		}
		
		cs.solver.pop();
		
		//他方の篩型を持つ変数の値が変わってもこの型の述語は満たされる
		System.out.println("check refinement type equality 2");
		cs.solver.push();
		
		comparative_refinement_type.add_refinement_constraint(cs, comparative_v, comparative_v.get_Expr(cs), comparative_class_Field, comparative_class_Expr, new ArrayList<IntExpr>(comparative_indexs.subList(0, comparative_class_Field.dims_sum())));
		
		if(v_indexs.size() == 0){
			cs.add_constraint(cs.ctx.mkEq(v.get_Expr(cs), comparative_v.get_full_Expr((ArrayList<IntExpr>) comparative_v_indexs.clone(), cs)));
			
			this.assert_refinement(cs, v, v.get_Expr(cs), class_Field, class_Expr, new ArrayList<IntExpr>(indexs.subList(0, class_Field.dims_sum())));
		}else{
			this.add_refinement_constraint(cs, v, v.get_Expr(cs), class_Field, class_Expr, new ArrayList<IntExpr>(indexs.subList(0, class_Field.dims_sum())));
			cs.add_constraint(cs.ctx.mkEq(v.get_Expr_assign(cs), v.assign_value(v_indexs, comparative_v.get_full_Expr((ArrayList<IntExpr>) comparative_v_indexs.clone(), cs), cs)));
			
			this.assert_refinement(cs, v, v.get_Expr_assign(cs), class_Field, class_Expr, new ArrayList<IntExpr>(indexs.subList(0, class_Field.dims_sum())));
			//v.temp_num++;
		}
		
		cs.solver.pop();

	}
	
	public boolean have_index_access(Check_status cs){
		boolean have = predicate.have_index_access(cs);
		
		refinement_type rt = cs.search_refinement_type(class_type_name, type.type);
		if(rt!=null){
			return have || rt.have_index_access(cs);
		}else{
			return have;
		}

	}

}

