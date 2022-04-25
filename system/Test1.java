package system;

import java.io.IOException;

import java.nio.file.*;
import java.util.Iterator;
import java.util.List;

import system.parsers.compilation_unit;

public class Test1 {
	public static void main(String[] args) throws Exception {
		String st = "";
		Path file = Paths.get("C:\\Users\\suto0\\Documents\\ƒ‰ƒ{\\shuron\\shuron\\src\\testcases\\valid\\extends_refinement_type.java");
		List<String> list = Files.readAllLines(file);
		Iterator<String> ite = list.iterator();
		while(ite.hasNext()){
			st = st + ite.next() + "\n";
		}
		
		System.out.println(st + "is file");
		compilation_unit cu = new compilation_unit();
		try{
			Parser_status ps = new Parser_status();
			String parsed = cu.parse(new Source(st), ps);
			cu.link_inheritance(ps.extends_pairs);
			System.out.println(parsed + "is parsed");
			cu.check(10);
		}catch (Exception e){
			System.out.println(e);
			System.out.println("Exception!!!");
		}
		
		
		
	}
}
