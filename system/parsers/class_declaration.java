package system.parsers;

import java.util.ArrayList;
import java.util.List;

import system.Check_status;
import system.Field;
import system.Pair;
import system.Parser;
import system.Parser_status;
import system.Source;
import system.Summery;
import system.Variable;

import com.microsoft.z3.*;

public class class_declaration implements Parser<String>{
	String class_name;
	class_block class_block;
	class_declaration super_class = null;
	public String parse(Source s,Parser_status ps)throws Exception{
		new spaces().parse(s,ps);
		new string("class").parse(s,ps);
		new spaces().parse(s,ps);
		this.class_name = new ident().parse(s,ps);
		String extends_clause = "";
		new spaces().parse(s,ps);
		Source s_backup = s.clone();
		try{
			String extends_class_name = new class_extends_clause().parse(s, ps);
			ps.extends_pairs.add(new Pair<String,String>(class_name, extends_class_name));//継承関係を登録
			extends_clause = "extends " + extends_class_name + " "; 
		}catch (Exception e){
			s.revert(s_backup);
		}
		ps.class_type_name = class_name;
		this.class_block = new class_block();
		String content = class_block.parse(s,ps);
		new newLines().parse(s,ps);
		
		return "class " + class_name + " " + extends_clause + content;
	}
	
	public void check(Check_status cs, Summery summery) throws Exception{
		System.out.println("Verify class " + this.class_name);
		Check_status csc =  cs.clone();
		modifiers m = new modifiers();
		m.is_final = true;
		Field this_field = new Variable(cs.Check_status_share.get_tmp_num(), "this", this.class_name, 0, null, m, null );
		this_field.temp_num = 0;
		//csc.fields.add(this_field);
		csc.this_field = this_field;
		//初期化
		csc.instance_expr = this_field.get_Expr(csc);
		csc.instance_Field = this_field;
		csc.instance_indexs = new ArrayList<IntExpr>();
		
		this.class_block.check(csc, summery, this);
	}
	
	//全てのフィールドと、そのフィールドまで返す
	public ArrayList<Field> all_field(int deep, int deep_limmit, Field class_object, Check_status cs) throws Exception{
		if(deep >= deep_limmit) return new ArrayList<Field>();
		ArrayList<Field> fields = new ArrayList<Field>();
		for(variable_definition vd : this.class_block.variable_definitions){
			Field field = cs.search_field(vd.variable_decls.ident, class_object, cs);
			fields.add(field);
			if(!vd.variable_decls.type_spec.type.type.equals("int") && !vd.variable_decls.type_spec.type.type.equals("boolean")){
				class_declaration cd = cs.Check_status_share.compilation_unit.search_class(vd.variable_decls.type_spec.type.type);
				if(cd != null){
					ArrayList<Field> field_fields = cd.all_field(deep+1, deep_limmit, field, cs);
					fields.addAll(field_fields);
				}else{
					throw new Exception("class " + vd.variable_decls.type_spec.type.type + " don't exist.");
				}
			}
		}
		
		return fields;
	}
	
}
