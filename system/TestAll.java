package system;

import java.io.IOException;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import system.parsers.compilation_unit;

public class TestAll {

	public static void main(String[] args) throws Exception {
		List<Path> paths = new ArrayList<Path>(); 
		
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\���{\\shuron\\shuron\\src\\testcases\\valid\\extends_refinement_type.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\���{\\shuron\\shuron\\src\\testcases\\valid\\extends_refinement_type_3class.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\���{\\shuron\\shuron\\src\\testcases\\valid\\extends_refinement_type_param.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\���{\\shuron\\shuron\\src\\testcases\\valid\\override_type_variable.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\���{\\shuron\\shuron\\src\\testcases\\invalid\\invalid_override_type_variable.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\���{\\shuron\\shuron\\src\\testcases\\valid\\valid_example.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\���{\\shuron\\shuron\\src\\testcases\\invalid\\invalid_example.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\���{\\shuron\\shuron\\src\\testcases\\valid\\array_field.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\���{\\shuron\\shuron\\src\\testcases\\valid\\example_Even.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\���{\\shuron\\shuron\\src\\testcases\\valid\\example_loop.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\���{\\shuron\\shuron\\src\\testcases\\valid\\assign_array.java"));
		
		Summery summery = new Summery();
		
		for(Path file : paths){
			summery.file = file;
			
			String st = "";
			List<String> list = Files.readAllLines(file);
			Iterator<String> ite = list.iterator();
			while(ite.hasNext()){
				st = st + ite.next() + "\n";
			}
			
			System.out.println(st + "is file");
			
			st = new Comment_analysis().comment_analysis(st);
			
			System.out.println(st + "is erase comment");
			
			compilation_unit cu = new compilation_unit();
			try{
				Parser_status ps = new Parser_status();
				String parsed = cu.parse(new Source(st), ps);
				System.out.println(parsed + "is parsed");
				cu.link_inheritance(ps.extends_pairs);
				cu.check(10, summery);
			}catch (Exception e){
				System.out.println(e);
				System.out.println("Exception!!!");
			}
		}
		
		summery.print_summery();
	}

}