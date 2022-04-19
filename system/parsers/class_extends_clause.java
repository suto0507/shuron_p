package system.parsers;

import system.Parser_status;
import system.Source;

public class class_extends_clause {
	public String parse(Source s,Parser_status ps)throws Exception{
		new string("extends").parse(s,ps);
		new spaces().parse(s,ps);
		String class_name = new ident().parse(s,ps);
		return class_name;//extends‚ð•Ô‚³‚È‚¢
	}
}
