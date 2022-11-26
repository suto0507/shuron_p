class model_refinement {
    /*`@ def_type Nat = {int x | x > 0};*/
	//@model int refinement_type Nat x;
	int y; //@ in x;
    //@represents x = y - 5;
	
	void hoge (int a) {
        y = 5;
	}
}