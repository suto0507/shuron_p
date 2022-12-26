package system;

import java.io.IOException;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import system.parsers.compilation_unit;

public class TestAll_invalid {

	public static void main(String[] args) throws Exception {
		List<Path> paths = new ArrayList<Path>(); 
		

		//paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_override_type_variable.java"));
		//paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_example.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_array_refinement.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_localarray_index_access.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_method_assign.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_loop_rt_local_alias.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_helper.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_method_in_helper.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_helper_array.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_helper_array_2d.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_helper_array_2d_method.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_model.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_model2.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_model_refinement.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_ensures_same_method.java"));
		
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
			st = st.replace('\n', ' ');
			st = st.replaceAll("[ \t]+", " ");
			
			System.out.println(st + "is erase comment");
			
			compilation_unit cu = new compilation_unit();
			try{
				Parser_status ps = new Parser_status();
				Source s = new Source(st);
				String parsed = cu.parse(s, ps);
				System.out.println(parsed + "is parsed");
				s.is_parsed(summery);
				cu.preprocessing(ps.extends_pairs, summery);
				Option option = new Option();
				cu.check(option, summery);
			}catch (Exception e){
				System.out.println(e);
				System.out.println("Exception!!!");
			}
		}
		
		summery.print_summery();
	}

}
