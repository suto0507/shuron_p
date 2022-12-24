package system;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import system.parsers.compilation_unit;

public class Runner {
	public static void main(String[] args) throws Exception {
		Option option = new Option();
		List<Path> paths = new ArrayList<Path>(); 
		
		for(int i = 0; i < args.length; i++){//“ü—Í
			if(args[i].equals("-refinement_type_limmit")){
				option.refinement_deep_limmit = Integer.parseInt(args[++i]);
			}else if(args[i].equals("-field_limmit")){
				option.invariant_refinement_type_deep_limmit = Integer.parseInt(args[++i]);
			}else{
				paths.add(Paths.get(args[i]));
			}
		}
		
		
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
				cu.check(option, summery);
			}catch (Exception e){
				System.out.println(e);
				System.out.println("Exception!!!");
			}
		}
		
		summery.print_summery();
	}
}
