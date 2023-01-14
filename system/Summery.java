package system;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Summery {
	public Path file;
	public List<String> valids;
	public List<String> invalids;
	public List<String> timeouts;
	public List<String> invalid_classes;
	public List<String> parse_faileds;
	public List<String> preprocessing_faileds;
	
	Summery(){
		valids = new ArrayList<String>();
		invalids = new ArrayList<String>();
		timeouts = new ArrayList<String>();
		invalid_classes = new ArrayList<String>();
		parse_faileds = new ArrayList<String>();
		preprocessing_faileds = new ArrayList<String>();
	}
	
	public void print_summery(){
		System.out.println("///////////// Summery //////////////");
		
		System.out.println("parse fail list");
		for(String parse_failed : parse_faileds){
			System.out.println(" - " + parse_failed);
		}
		
		System.out.println("preprocessing fail list");
		for(String preprocessing_fail : preprocessing_faileds){
			System.out.println(" - " + preprocessing_fail);
		}
		
		System.out.println("valid method list");
		for(String valid : valids){
			System.out.println(" - " + valid);
		}
		
		System.out.println("invalid method list");
		for(String invalid : invalids){
			System.out.println(" - " + invalid);
		}
		
		System.out.println("time out method list");
		for(String timeout : timeouts){
			System.out.println(" - " + timeout);
		}
		
		System.out.println("invalid class list");
		for(String invalid : invalid_classes){
			System.out.println(" - " + invalid);
		}
		
		System.out.println("number of preprocessing fail " + preprocessing_faileds.size());
		System.out.println("number of parse fail " + parse_faileds.size());
		System.out.println("number of valid methods id " + valids.size() + "/" + (valids.size() + invalids.size() + timeouts.size()));
		
	}
}
