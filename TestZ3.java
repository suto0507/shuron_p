package soturon;
import com.microsoft.z3.*;

import java.util.HashMap;

public class TestZ3 {
	public static void main(String[] args) {
		Context context = new Context(new HashMap<>());
		Solver solver = context.mkSolver();
		
		//st2ÇÃçƒåª
		IntExpr a = context.mkIntConst("a");
		BoolExpr assert1 = context.mkGt(a, context.mkInt(0));
		IntExpr a2 = context.mkIntConst("a");
		BoolExpr assert2 = context.mkNot(context.mkGt(a2, context.mkInt(1)));
		IntExpr b = context.mkIntConst("b");
		BoolExpr assert3 = context.mkEq(b, context.mkUnaryMinus(context.mkInt(13)));
		//BoolExpr a1 = context.parseSMTLIB2String(st2,null,null,null,null);
		//System.out.println(a1);
		//solver.add(a1);
		System.out.println(a);
		System.out.println(assert1);
		solver.add(assert1); 
		System.out.println(assert2);
		solver.add(assert2); 
		System.out.println(assert3);
		solver.push();
		solver.add(assert3); 
		//solver.pop();
		System.out.println("hoge " + (-13)%5);
		System.out.println(assert3);
        if(solver.check() == Status.SATISFIABLE) {
            Model model = solver.getModel();
            System.out.println(model.toString());

        }else{
        	System.out.println("not sat");
        }
        solver.pop();
        
        solver.add(context.mkNot(assert3)); 
        if(solver.check() == Status.SATISFIABLE) {
            Model model = solver.getModel();
            System.out.println(model.toString());

        }else{
        	System.out.println("not sat");
        }
        
  
	}
}
