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
		
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\extends_refinement_type.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\extends_refinement_type_3class.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\extends_refinement_type_param.java"));
		//paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\override_type_variable.java"));
		//paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\valid_example.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\array_field.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\example_Even.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\example_loop.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\assign_array.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\also_ensures.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\same_v.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\old.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\if.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\prepost_inheritance.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\minus.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\bracket_field.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\array_refinement.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\array_method.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\array_branch_alias.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\method_in_refinement_type.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\method_assign.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\loop_rt_local_alias.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\helper.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\method_in_helper.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\helper_array.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\helper_array_2d.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\model.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\model2.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\model_refinement.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\sub_constructor.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\overload.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\local_in_rt.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\invariant.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\assign_test.java"));
		paths.add(Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\assign_test2.java"));
		
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
				Parser_status ps = new Parser_status(file.toString());
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
