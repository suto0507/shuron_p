package system.parsers;

import system.Parser;
import system.Parser_status;
import system.Source;

public class newLines implements Parser<String>{
	
	public String parse(Source s,Parser_status ps)throws Exception{
		//return new manyString(new newLine()).parse(s,ps);
		//���s���������Ă���p�[�X���邱�Ƃɂ����̂�
		return new spaces().parse(s, ps);
	}
}

