package system;

public class Option {
	public int refinement_deep_limmit;
	public int invariant_refinement_type_deep_limmit;
	public int timeout;
	
	//�f�t�H���g�l
	public Option(){
		refinement_deep_limmit = 10;
		invariant_refinement_type_deep_limmit = 10;
		timeout = -1;
	}
}
