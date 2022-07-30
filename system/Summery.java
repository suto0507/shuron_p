package system;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Summery {
	public Path file;
	public List<String> valids;
	public List<String> invalids;
	public List<String> parse_faileds;
	public List<String> inheritance_faileds;
	
	Summery(){
		valids = new ArrayList<String>();
		invalids = new ArrayList<String>();
		parse_faileds = new ArrayList<String>();
		inheritance_faileds = new ArrayList<String>();
	}
	
	public void print_summery(){
		System.out.println("///////////// Summery //////////////");
		
		System.out.println("parse fail list");
		for(String parse_failed : parse_faileds){
			System.out.println(" - " + parse_failed);
		}
		
		System.out.println("inheritance fail list");
		for(String inheritance_fail : inheritance_faileds){
			System.out.println(" - " + inheritance_fail);
		}
		
		System.out.println("valid method list");
		for(String valid : valids){
			System.out.println(" - " + valid);
		}
		
		System.out.println("invalid method list");
		for(String invalid : invalids){
			System.out.println(" - " + invalid);
		}
		
		System.out.println("number of inheritance fail " + inheritance_faileds.size());
		System.out.println("number of parse fail " + parse_faileds.size());
		System.out.println("number of valid methods id " + valids.size() + "/" + (valids.size() + invalids.size()));
		
	}
}
