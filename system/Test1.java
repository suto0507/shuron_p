package system;

import java.io.IOException;

import java.nio.file.*;
import java.util.Iterator;
import java.util.List;

import system.parsers.compilation_unit;

public class Test1 {
	public static void main(String[] args) throws Exception {
		String st = "";
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\extends_refinement_type.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\extends_refinement_type_3class.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\extends_refinement_type_param.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\override_type_variable.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\invalid\\invalid_override_type_variable.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\valid_example.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\invalid\\invalid_example.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\array_field.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\example1.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\example2.java");
		Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\assign_array.java");
		List<String> list = Files.readAllLines(file);
		Iterator<String> ite = list.iterator();
		while(ite.hasNext()){
			st = st + ite.next() + "\n";
		}
		
		System.out.println(st + "is file");
		
		st = new Comment_analysis().comment_analysis(st);
		
		compilation_unit cu = new compilation_unit();
		try{
			Parser_status ps = new Parser_status();
			String parsed = cu.parse(new Source(st), ps);
			System.out.println(parsed + "is parsed");
			cu.link_inheritance(ps.extends_pairs);
			cu.check(10);
		}catch (Exception e){
			System.out.println(e);
			System.out.println("Exception!!!");
		}
		
		
		
	}
}
