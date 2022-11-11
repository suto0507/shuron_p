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
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\example_Even.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\example_loop.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\same_v.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\also_ensures.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\assign_array.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\quantifier.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\inharitance_test.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\old.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\if.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\prepost_inheritance.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\minus.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\bracket_field.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\array_refinement.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\method_in_refinement_type.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\invalid\\invalid_array_refinement_type_override\\1.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\invalid\\invalid_array_refinement_type_override\\2.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\invalid\\invalid_array_refinement_type_override\\3.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\invalid\\invalid_array_refinement_type_override\\4.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\invalid\\invalid_array_refinement_type_override\\5.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\array_method.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\invalid\\invalid_localarray_index_access.java");
		Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\array_branch_alias.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\method_assign.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\invalid\\invalid_method_assign.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\loop_rt_local_alias.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\invalid\\invalid_loop_rt_local_alias.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\if_return.java");
		
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\helper.java");

		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\helper2.java");//エイリアスの処理をどうにかしないとinvalidになる
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\invalid\\invalid_helper.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\method_in_helper.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\invalid\\invalid_method_in_helper.java");
		
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\helper_array.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\invalid\\invalid_helper_array.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\valid\\helper_array_2d.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\invalid\\invalid_helper_array_2d.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron\\src\\testcases\\invalid\\invalid_helper_array_2d_method.java");
		
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\override_type_example.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\inheritance_1.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\inheritance_1_2.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\inheritance_2.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\inheritance_3.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\10_3_zemi\\ok_local_alias.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\10_3_zemi\\no_local_alias_ok_2.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\10_3_zemi\\if_alias.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\10_3_zemi\\while_alias.java");
		
		Summery summery = new Summery();
		summery.file = file;
		
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
			cu.check(10, 10, summery);
		}catch (Exception e){
			System.out.println(e);
			System.out.println("Exception!!!");
		}
		
		
		summery.print_summery();
	}
}
