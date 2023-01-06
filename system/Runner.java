package system;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import system.parsers.compilation_unit;
import system.parsers.class_declaration;

public class Runner {
	public static void main(String[] args) throws Exception {
		Option option = new Option();
		List<Path> paths = new ArrayList<Path>(); 
		
		for(int i = 0; i < args.length; i++){//ì¸óÕ
			if(args[i].equals("-refinement_type_limmit")){
				option.refinement_deep_limmit = Integer.parseInt(args[++i]);
			}else if(args[i].equals("-field_limmit")){
				option.invariant_refinement_type_deep_limmit = Integer.parseInt(args[++i]);
			}else{
				paths.add(Paths.get(args[i]));
			}
		}
		
		
		Summery summery = new Summery();
		compilation_unit base_cu = new compilation_unit();
		base_cu.classes = new ArrayList<class_declaration>();
		ArrayList<Pair<String, String>> extends_pairs = new ArrayList<Pair<String, String>>();
		
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
				
				//base_cuÇ…èàóùÇèWñÒÇ∑ÇÈ
				base_cu.classes.addAll(cu.classes);
				extends_pairs.addAll(ps.extends_pairs);
			}catch (Exception e){
				System.out.println(e);
				System.out.println("Exception!!!");
			}
		}
		

		base_cu.preprocessing(extends_pairs, summery);
		base_cu.check(option, summery);
		
		summery.print_summery();
	}
}
