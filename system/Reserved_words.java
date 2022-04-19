package system;

import java.util.ArrayList;
import java.util.List;

public class Reserved_words {
	public List<String> reserved_words;
	public Reserved_words(){
		this.reserved_words = new ArrayList<String>();
		this.reserved_words.add("class");
		this.reserved_words.add("private");
		this.reserved_words.add("final");
		this.reserved_words.add("void");
		this.reserved_words.add("boolean");
		this.reserved_words.add("int");
		this.reserved_words.add("if");
		this.reserved_words.add("else");
		this.reserved_words.add("return");
		this.reserved_words.add("for");
		this.reserved_words.add("this");
		this.reserved_words.add("new");
		this.reserved_words.add("true");
		this.reserved_words.add("false");
		
		//ˆÈ‰ºjmlA•K—v‚É‚È‚é‚©‚à
		/*
		this.reserved_words.add("spec_public");
		this.reserved_words.add("maintaining");
		this.reserved_words.add("assert");
		this.reserved_words.add("requires");
		this.reserved_words.add("ensures");
		this.reserved_words.add("assignable");
		this.reserved_words.add("\\nothing");
		this.reserved_words.add("invariant");
		this.reserved_words.add("\\result");
		this.reserved_words.add("\\old");
		this.reserved_words.add("def_type");
		this.reserved_words.add("refinement_type");
		*/
		
	}
	
}
