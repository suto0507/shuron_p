package system.parsers;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;

import system.Check_status;
import system.Field;
import system.Parser;
import system.Parser_status;
import system.Source;

public class class_block implements Parser<String>{
	
	List<invariant> invariants;
	List<def_type_clause> def_type_clauses;
	List<method_decl> method_decls;
	List<variable_definition> variable_definitions;
	List<override_refinement_type_clause> override_refinement_type_clauses;
	
	public String parse(Source s,Parser_status ps)throws Exception{
		invariants = new ArrayList<invariant>();
		def_type_clauses = new ArrayList<def_type_clause>();
		method_decls = new ArrayList<method_decl>();
		variable_definitions = new ArrayList<variable_definition>();
		override_refinement_type_clauses = new ArrayList<override_refinement_type_clause>();

		new string("{").parse(s,ps);
		new newLines().parse(s,ps);
		Source s_backup = s.clone();
		try{
			while(true){
				s_backup = s.clone();
				field f = new field();
				Parser p = f.parse(s,ps);
				if(p instanceof invariant){
					invariants.add((invariant) p);
				}else if(p instanceof def_type_clause){
					def_type_clauses.add((def_type_clause) p);
				}else if(p instanceof method_decl){
					method_decls.add((method_decl) p);
				}else if(p instanceof variable_definition){
					variable_definitions.add((variable_definition) p);
				}else if(p instanceof override_refinement_type_clause){
					override_refinement_type_clauses.add((override_refinement_type_clause)p);
				}
			}
		}catch (Exception e){
			s.revert(s_backup);
		}
		new spaces().parse(s,ps);
		new cha('}').parse(s,ps);
		
		String ret = "";
		
		ret = ret + "{\n";
		ret = ret + "//def_types are \n";
		for(def_type_clause p : def_type_clauses){
			ret = ret + "<----------\n" + p.st + "\n------->\n";
		}
		ret = ret + "//variables are \n";
		for(variable_definition p : variable_definitions){
			ret = ret + "<----------\n" + p.st + "\n------->\n";
		}
		ret = ret + "//invariants are \n";
		for(invariant p : invariants){
			ret = ret + "<----------\n" + p.st + "\n------->\n";
		}
		ret = ret + "//methods are \n";
		for(method_decl p : method_decls){
			ret = ret + "<----------\n" + p.st + "\n------->\n";
		}
		ret = ret + "}\n";
		
		
		return ret;
	}
	
	public void check(Check_status cs) throws Exception{
		//invariantとフィールドについて書く
		cs.invariants = this.invariants;
		
		for(method_decl method :method_decls){
			Check_status csc =  cs.clone();
			csc.clone_list();
			csc.solver = csc.ctx.mkSolver();
			
			csc.return_exprs = new ArrayList<Expr>();
			csc.return_pathconditions = new ArrayList<BoolExpr>();
			method.check(csc);
		}
	}
	
}
