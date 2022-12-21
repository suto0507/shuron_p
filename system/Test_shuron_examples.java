package system;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import system.parsers.compilation_unit;

public class Test_shuron_examples {
	public static void main(String[] args) throws Exception {
		List<Path> paths = new ArrayList<Path>(); 
		
		paths.add(Paths.get("\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\refinement_type_example.java"));
		paths.add(Paths.get("\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\override_type_example.java"));
		paths.add(Paths.get("\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\inheritance_1_2.java"));
		paths.add(Paths.get("\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\inheritance_1.java"));
		paths.add(Paths.get("\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\inheritance_2.java"));
		paths.add(Paths.get("\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\inheritance_3.java"));
		paths.add(Paths.get("\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\model_refinement_type.java"));
		paths.add(Paths.get("\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\model_example.java"));
		paths.add(Paths.get("\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\no_local_alias_ok_2.java"));
		paths.add(Paths.get("\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\no_local_alias_ok_2_helper.java"));
		paths.add(Paths.get("\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\ok_local_alias.java"));
		paths.add(Paths.get("\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\if_alias.java"));
		paths.add(Paths.get("\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\while_alias.java"));
		paths.add(Paths.get("\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\method_call_in_rt_bad.java"));
		paths.add(Paths.get("\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\method_call_in_rt_good.java"));
		paths.add(Paths.get("\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\helper_example.java"));
		paths.add(Paths.get("\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\helper_array.java"));
		paths.add(Paths.get("\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\while_alias.java"));
		
		paths.add(Paths.get("\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\invalid_examples\\helper_array_loop_invalid.java"));
		paths.add(Paths.get("\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\invalid_examples\\helper_bad_array_assign_2d.java"));
		paths.add(Paths.get("\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\invalid_examples\\helper_bad_array_assign.java"));
		
		paths.add(Paths.get("\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\benchmark\\Clock\\clock_sjml.java"));
		paths.add(Paths.get("\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\benchmark\\Clock\\clock_sjml_refinement_type.java"));
		paths.add(Paths.get("\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\benchmark\\Clock\\clock_sjml_no_comment.java"));
		paths.add(Paths.get("\\Users\\suto0\\Documents\\ラボ\\shuron\\shuron_examples\\benchmark\\Clock\\clock_sjml_refinement_type_no_comment.java"));
		
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
