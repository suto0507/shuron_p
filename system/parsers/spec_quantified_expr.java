package system.parsers;

import com.microsoft.z3.*;

import system.Check_status;
import system.Pair;
import system.Parser;
import system.Parser_status;
import system.Source;

public class spec_quantified_expr implements Parser<String>{
	quantifier quantifier;
	quantified_var_decls quantified_var_decls;
	predicate guard;
	spec_expression body;
	
	public String parse(Source s,Parser_status ps)throws Exception{
		String st = "";
		st += new string("(").parse(s, ps);
		st += new spaces().parse(s, ps);
		
		quantifier = new quantifier();
		st += quantifier.parse(s, ps);
		st += new spaces().parse(s, ps);
		
		quantified_var_decls = new quantified_var_decls();
		st += quantified_var_decls.parse(s, ps);
		st += new spaces().parse(s, ps);
		st += new string(";").parse(s, ps);
		st += new spaces().parse(s, ps);
		
		Source s_backup = s.clone();
		try{
			String st2 = "";
			predicate predicate = new predicate();
			st2 += predicate.parse(s, ps);
			st2 += new spaces().parse(s, ps);
			st2 += new string(";").parse(s, ps);
			st2 += new spaces().parse(s, ps);
			this.guard = predicate;
			st += st2;
		}catch (Exception e){
			s.revert(s_backup);
		}
		
		body = new spec_expression();
		st += body.parse(s, ps);
		st += new spaces().parse(s, ps);
		
		st += new string(")").parse(s, ps);
		
		return st;
	}
	
	public Quantifier check(Check_status cs) throws Exception{
		//量化子の型
		Sort sort;
		int dims = this.quantified_var_decls.type_spec.dims;
		String type = this.quantified_var_decls.type_spec.type.type;
		if(type.equals("int") && dims==0){
			sort = cs.ctx.mkIntSort();
		}else if(type.equals("boolean") && dims==0){
			sort =  cs.ctx.mkBoolSort();
		}else if(type.equals("void")){
			throw new Exception("void quantifier ?");
		}else if(dims==0){
			//クラス
			sort =  cs.ctx.mkUninterpretedSort("Ref");
		}else if(type.equals("int")&&dims==1){ //配列
			sort =  cs.ctx.mkArraySort(cs.ctx.mkIntSort(), cs.ctx.mkIntSort());
		}else if(type.equals("boolean")&&dims==1){
			sort =  cs.ctx.mkArraySort(cs.ctx.mkIntSort(), cs.ctx.mkBoolSort());
		}else if(dims==1){
			sort =  cs.ctx.mkArraySort(cs.ctx.mkIntSort(), cs.ctx.mkUninterpretedSort("Ref"));
		}else{
			throw new Exception("unexpect quantifier");
		}
		
		Expr[] exprs = new Expr[this.quantified_var_decls.quantified_var_declarators.size()];
		Pair<String, Expr>[] quantifiers = new Pair[this.quantified_var_decls.quantified_var_declarators.size()];
		for(int i = 0; i < this.quantified_var_decls.quantified_var_declarators.size(); i++){
			quantified_var_declarator qvd = this.quantified_var_decls.quantified_var_declarators.get(i);
			String symbol_name = qvd.ident + cs.Check_status_share.get_tmp_num();
			Symbol symbol = cs.ctx.mkSymbol(symbol_name);
			exprs[i] = cs.ctx.mkConst(symbol_name, sort);
			quantifiers[i] = new Pair<String, Expr>(qvd.ident, exprs[i]);
			cs.quantifiers.add(quantifiers[i]);
		}
		BoolExpr guard;
		if(this.guard==null){
			guard = cs.ctx.mkBool(true);
		}else{
			guard = (BoolExpr) this.guard.check(cs);
		}
		BoolExpr body = (BoolExpr) this.body.check(cs);
		for(int i = 0; i < quantifiers.length; i++){
			cs.quantifiers.remove(quantifiers[i]);
		}
		
		
		if(this.quantifier.quantifier == "forall"){
			return cs.ctx.mkForall(exprs, cs.ctx.mkImplies(guard, body), 1, null, null, null, null);
		}else if(this.quantifier.quantifier == "exists"){
			return cs.ctx.mkExists(exprs, cs.ctx.mkAnd(guard, body), 1, null, null, null, null);
		}else{
			throw new Exception("unexpect quantifier");
		}
		
		
	}
}
