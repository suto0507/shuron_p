package system.parsers;

import java.util.List;

import com.microsoft.z3.BoolExpr;

import system.Check_status;
import system.Pair;
import system.Parser;
import system.Parser_status;
import system.Source;
import system.parsers.spec_case_seq.F_Assign;

public class extending_specification implements Parser<String>  {

	spec_case_seq spec_case_seq;
	
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";


		st = st + new spaces().parse(s,ps);
		st = st + new string("also").parse(s, ps);
		st = st + new newLines().parse(s, ps);
		spec_case_seq scs = new spec_case_seq();
		st = scs.parse(s,ps);
		this.spec_case_seq = scs;
		return st;
	}
	
	public BoolExpr requires_expr(Check_status cs) throws Exception{
		return spec_case_seq.requires_expr(cs);
	}
	
	public BoolExpr ensures_expr(Check_status cs) throws Exception{
		return spec_case_seq.ensures_expr(cs);
	}
	
	public Pair<List<F_Assign>, BoolExpr> assignables(Check_status cs) throws Exception{
		return spec_case_seq.assignables(cs);
	}
}