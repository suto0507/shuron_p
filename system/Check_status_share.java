package system;

import java.util.ArrayList;
import java.util.List;

import system.parsers.compilation_unit;

public class Check_status_share {
	public int tmp_num;
	public compilation_unit compilation_unit;
	
	Check_status_share(compilation_unit cu){
		this.compilation_unit = cu;
		this.tmp_num = 0;
	}
	
	public int get_tmp_num(){
		tmp_num++;
		return tmp_num;
	}
	
	public String new_temp(){
		String st = "_JML_tmp_" + tmp_num;
		tmp_num++;
		return st;
	}

}
