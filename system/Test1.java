package system;

import java.io.IOException;

import java.nio.file.*;
import java.util.Iterator;
import java.util.List;

import system.parsers.compilation_unit;

public class Test1 {
	public static void main(String[] args) throws Exception {
		String st = "";
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\extends_refinement_type.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\extends_refinement_type_3class.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\extends_refinement_type_param.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\override_type_variable.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_override_type_variable.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\valid_example.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_example.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\array_field.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\example_Even.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\example_loop.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\same_v.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\also_ensures.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\assign_array.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\quantifier.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\inharitance_test.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\old.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\if.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\prepost_inheritance.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\minus.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\bracket_field.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\array_refinement.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\method_in_refinement_type.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_array_refinement_type_override\\1.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_array_refinement_type_override\\2.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_array_refinement_type_override\\3.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_array_refinement_type_override\\4.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_array_refinement_type_override\\5.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\array_method.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_localarray_index_access.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\array_branch_alias.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_array_branch_alias.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\method_assign.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_method_assign.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\loop_rt_local_alias.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_loop_rt_local_alias.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\if_return.java");
		
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\helper.java");

		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\helper2.java");//エイリアスの処理をどうにかしないとinvalidになる
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_helper.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\method_in_helper.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_method_in_helper.java");
		
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\helper_array.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_helper_array.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\helper_array_2d.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_helper_array_2d.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_helper_array_2d_method.java");
		
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\model.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_model.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\model2.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_model2.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\model_refinement.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_model_refinement.java");
		
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_ensures_same_method.java");
		
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\sub_constructor.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_sub_constructor.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\overload.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_overload.java");
		
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\local_in_rt.java");
		
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\valid\\invariant.java");
		Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\invalid\\invalid_invariant.java");
		
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\shuron_examples\\override_type_example.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\shuron_examples\\inheritance_1.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\shuron_examples\\inheritance_1_2.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\shuron_examples\\inheritance_2.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\shuron_examples\\inheritance_3.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\shuron_examples\\helper.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\shuron_examples\\helper_array.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\shuron_examples\\model_example.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\shuron_examples\\invalid_examples\\helper_bad_array_assign.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\shuron_examples\\invalid_examples\\helper_bad_array_assign_2d.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\shuron_examples\\10_3_zemi\\ok_local_alias.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\shuron_examples\\no_local_alias_ok_2.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\shuron_examples\\10_3_zemi\\if_alias.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\shuron_examples\\10_3_zemi\\while_alias.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\shuron_examples\\refinement_type_example.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\shuron_examples\\rt_transitivity.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\shuron_examples\\invalid_example.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\shuron_examples\\invalid_examples\\local_bad_alias.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\shuron_examples\\invalid_examples\\bad_array_refinement.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\shuron_examples\\loop_exaple.java");
		
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\benchmark\\Clock\\clock_sjml.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\benchmark\\Clock\\clock_sjml_refinement_type.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\benchmark\\Clock\\clock_sjml_no_comment.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\benchmark\\Clock\\clock_sjml_refinement_type_no_comment.java");
		

		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\shuron_examples\\run_example\\inheritance.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\shuron_examples\\run_example\\model.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\shuron_examples\\run_example\\array.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\shuron_examples\\run_example\\method.java");
		//Path file = Paths.get("C:\\Users\\suto0\\Documents\\ラボ\\shuron\\testcases\\shuron_examples\\run_example\\helper.java");
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
		
		
		summery.print_summery();
	}
}
