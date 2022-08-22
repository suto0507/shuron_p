package system.parsers;

import java.util.ArrayList;

import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;

import system.Check_status;
import system.Field;
import system.Parser;
import system.Parser_status;
import system.Source;

public class refinement_type_clause implements Parser<String>{
	
	public String ident;
	public refinement_type refinement_type;
	
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		st = st + new spaces().parse(s, ps);
		st = st + new string("refinement_type").parse(s, ps);
		st = st + new spaces().parse(s, ps);
		Source s_backup = s.clone();
		try{
			this.ident = new ident().parse(s, ps);
			st = st + this.ident;
		}catch (Exception e){
			s.revert(s_backup);
			this.refinement_type = new refinement_type();
			st = st + this.refinement_type.parse(s, ps);
		}
		
		return st;
	}
	
	public boolean have_index_access(String class_object_type, Check_status cs){
		if(this.refinement_type!=null){
			return this.refinement_type.have_index_access(cs);
		}else{
			refinement_type rt = cs.search_refinement_type(class_object_type, ident);
			return rt.have_index_access(cs);
		}
	}
	
	public void equal_predicate(ArrayList<IntExpr> indexs, Field class_Field, Expr class_Expr, refinement_type_clause comparative_refinement_type_clause, ArrayList<IntExpr> comparative_indexs, Field comparative_class_Field, Expr comparative_class_Expr, Check_status cs) throws Exception{
		refinement_type rt;
		if(this.refinement_type!=null){
			rt = this.refinement_type;
		}else{
			rt = cs.search_refinement_type(class_Field.type, this.ident);
		}
		
		refinement_type crt;
		
		if(comparative_refinement_type_clause.refinement_type!=null){
			crt = comparative_refinement_type_clause.refinement_type;
		}else{
			crt = cs.search_refinement_type(comparative_class_Field.type, comparative_refinement_type_clause.ident);
		}
		
		rt.equal_predicate(indexs, class_Field, class_Expr, crt, comparative_indexs, comparative_class_Field, comparative_class_Expr, cs);
		
	}
}
