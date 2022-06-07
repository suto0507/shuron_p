package system.parsers;

import java.util.List;

import com.microsoft.z3.BoolExpr;

import system.Check_status;
import system.Parser;
import system.Parser_status;
import system.Source;

public class method_specification implements Parser<String> {
	spec_case_seq spec_case_seq;
	extending_specification extending_specification;
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		Source s_backup = s.clone();
		try{
			spec_case_seq scs = new spec_case_seq();
			st = scs.parse(s,ps);
			this.spec_case_seq = scs;
		}catch (Exception e){
			extending_specification es = new extending_specification();
			st = es.parse(s,ps);
			this.extending_specification = es;
		}
		return st;
	}
	
	public BoolExpr requires_expr(Check_status cs) throws Exception{
		if(spec_case_seq != null){
			return spec_case_seq.requires_expr(cs);
		}else{
			return extending_specification.requires_expr(cs);
		}
		
	}
	
	public BoolExpr ensures_expr(Check_status cs) throws Exception{
		if(spec_case_seq != null){
			return spec_case_seq.ensures_expr(cs);
		}else{
			return extending_specification.ensures_expr(cs);
		}
	}
}
