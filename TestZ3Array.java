package soturon;

import java.util.HashMap;

import com.microsoft.z3.*;

public class TestZ3Array {
	public static void main(String[] args) {
		Context context = new Context(new HashMap<>());
		Solver solver = context.mkSolver();
		
		//st2ÇÃçƒåª
		IntExpr a = context.mkIntConst("a");
		BoolExpr assert1 = context.mkGt(a, context.mkInt(0));
		IntExpr b = context.mkIntConst("b");
		BoolExpr assert2 = context.mkGt(b, context.mkInt(1));
		ArrayExpr array = context.mkArrayConst("array", context.mkIntSort(), context.mkIntSort());
		ArrayExpr array_1 = context.mkStore(array,context. mkInt(0), context.mkInt(1));
		BoolExpr assert3 = context.mkEq(context.mkSelect(array_1, context.mkInt(0)), a);
		ArrayExpr array_2 = context.mkStore(array,context. mkInt(0), context.mkInt(2));
		BoolExpr assert4 = context.mkEq(context.mkSelect(array_2, context.mkInt(0)), b);
		
		Sort RefSort = context.mkUninterpretedSort("Ref");
		Expr x = context.mkConst("x", context.mkUninterpretedSort("Ref"));
		ArrayExpr array_ref = context.mkArrayConst("array_ref", context.mkUninterpretedSort("Ref"), context.mkIntSort());
		ArrayExpr array_ref_1 = context.mkStore(array_ref, x, context.mkInt(1));
		BoolExpr assert5_0 = context.mkEq(context.mkArrayConst("array_ref_1", context.mkUninterpretedSort("Ref"), context.mkIntSort()), array_ref_1);
		System.out.println(assert5_0);
		solver.add(assert5_0); 
		IntExpr c = context.mkIntConst("c");
		BoolExpr assert5 = context.mkEq(context.mkSelect(context.mkArrayConst("array_ref_1", context.mkUninterpretedSort("Ref"), context.mkIntSort()), context.mkConst("x", context.mkUninterpretedSort("Ref"))), c);
		System.out.println(assert5);
		solver.add(assert5); 
		
		IntExpr d = context.mkIntConst("d");
		BoolExpr assert6 = context.mkEq(context.mkSelect(context.mkArrayConst("array_ref_1", context.mkUninterpretedSort("Ref"), context.mkIntSort()), context.mkConst("undifined", context.mkUninterpretedSort("Ref"))), d);
		System.out.println(assert6);
		solver.add(assert6); 
		
		/*
		System.out.println(a);
		System.out.println(assert1);
		solver.add(assert1); 
		System.out.println(assert2);
		solver.add(assert2); 
		System.out.println(assert3);
		solver.add(assert3); 
		System.out.println(assert4);
		solver.add(assert4); 
		*/
		
		 
        if(solver.check() == Status.SATISFIABLE) {
            Model model = solver.getModel();
            System.out.println(model.toString());

        }else{
        	System.out.println("not sat");
        	Model model = solver.getModel();
            System.out.println(model.toString());
        }
        
  
	}
}
