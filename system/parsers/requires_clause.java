package system.parsers;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;

import system.Check_status;
import system.Parser;
import system.Parser_status;
import system.Source;

public class requires_clause implements Parser<String>{
	predicate predicate;
	BoolExpr expr; //���\�b�h�̒��g�����s����O�̏q���ێ�����K�v������
	BoolExpr call_expr; //���̃��\�b�h�Ăяo���p
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		st = st + new string("requires").parse(s, ps);
		st = st + new spaces().parse(s, ps);
		this.predicate = new predicate();
		st = st + this.predicate.parse(s, ps);
		st = st + new spaces().parse(s, ps);
		st = st + new string(";").parse(s, ps);
		return st;
	}
	
	public Expr check(Check_status cs) throws Exception{
		return this.predicate.check(cs);
	}
	
	public void set_expr(BoolExpr ex, Check_status cs){
		if(cs.in_method_call){
			call_expr = ex;
		}else{
			expr = ex;
		}
	}
	
	public BoolExpr get_expr(Check_status cs){
		if(cs.in_method_call){
			return call_expr;
		}else{
			return expr;
		}
	}

}