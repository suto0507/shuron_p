package soturon;
import soturon.Parsers.*;
import system.Parser_status;
import system.Parsers;
import system.Source;
public class Test3 {
	

    //final static Parser<String> test7 = new string("ab");
	
	public static void parseTest(Parser p, String src) {
		Source s = new Source(src);
		try {
			System.out.println(p.parse(s,new Parser_status()));
			System.out.println("success");
			System.out.println(s.positionChar());
		} catch (Exception e) {
			System.out.println(e.getMessage());
			s.where();
			s.deepestwhere();
	    }
		
		System.out.println("\n");
	}
	
	public static void parseTest_inv(Parser p, String src) {
		Source s = new Source(src);
		try {
			System.out.println(((Parsers.invariant)p.parse(s,new Parser_status())).st);
			System.out.println(s.positionChar());
		} catch (Exception e) {
			System.out.println(e.getMessage());
			s.where();
	    }
		
		System.out.println("\n");
	}
	
	public static void main(String[] args) {
        Parsers p = new Parsers();
        System.out.println("--test1----");
        parseTest(p.new mult_expr(), "i_1 * y * z");
        System.out.println("--test2----");
        parseTest(p.new spaces(), "");
        System.out.println("--test3----");
        parseTest(p.new generic_spec_case(), "/*@ensures evennum > evennum;*/ \n");
        System.out.println("--test4----");
        parseTest(p.new expression(), "x > 0 || x % 2 && 0");
        System.out.println("--test4----");
        parseTest(p.new statement(), "/*`@def_type NatEven = {int x | x >= 0 && x % 2 == 0};*/");
    }
}
