package system.parsers;

import java.util.ArrayList;
import java.util.List;

import system.Parser;
import system.Parser_status;
import system.Source;

public class possibly_annotated_loop implements Parser<String>{
	List<loop_invariant> loop_invariants;
	loop_stmt loop_stmt;
	public String parse(Source s,Parser_status ps)throws Exception{
		Source s_backup = s.clone();
		String st = "";
		this.loop_invariants = new ArrayList<loop_invariant>();
		try {
			while(true){
				s_backup = s.clone();
				loop_invariant li = new loop_invariant();
				st = st + new jml_anotation_newLine(li).parse(s,ps);
				this.loop_invariants.add(li);
				st = st + new spaces().parse(s, ps);
			}
		}catch (Exception e){
			s.revert(s_backup);
		}
		this.loop_stmt = new loop_stmt();
		st = st + this.loop_stmt.parse(s, ps);
		return st;
	}
}