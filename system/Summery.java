package system;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Summery {
	public Path file;
	public List<String> valids;
	public List<String> invalids;
	
	Summery(){
		valids = new ArrayList<String>();
		invalids = new ArrayList<String>();
	}
	
	public void print_summery(){
		System.out.println("///////////// Summery //////////////");
		System.out.println("valid method list");
		for(String valid : valids){
			System.out.println(" - " + valid);
		}
		
		System.out.println("invalid method list");
		for(String invalid : invalids){
			System.out.println(" - " + invalid);
		}
		
		System.out.println("number of valid methods id " + valids.size() + "/" + (valids.size() + invalids.size()));
		
	}
}
