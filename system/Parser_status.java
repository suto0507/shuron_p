package system;

import java.util.ArrayList;
import java.util.List;

public class Parser_status {
	public boolean in_jml;
	public boolean in_ensures;
	public String class_type_name;
	public List<Pair<String, String>> extends_pairs;
	
	public Parser_status(){
		this.extends_pairs = new ArrayList<Pair<String, String>>();
	}
	
	public Parser_status jml(){
		Parser_status ps = (Parser_status) this.clone();
		ps.in_jml = true;
		return ps;
	}
	
	public Parser_status ensures(){
		Parser_status ps = (Parser_status) this.clone();
		ps.in_ensures = true;
		return ps;
	}
	
	@Override
	public Parser_status clone(){
		Parser_status ps = new Parser_status();
		ps.in_jml = this.in_jml;
		ps.in_ensures = this.in_ensures;
		return ps;
	}
}
