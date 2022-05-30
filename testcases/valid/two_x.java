class A{
	/*`@ def_type Zero = {int x | x == 0};*/
    int /*`@ refinement_type Nat*/ x;
    
	
    /*@ensures \result == x;*/
	int test(){
		return 0;
	}
	

}

class B extends A{
	/*`@ def_type One = {int x | x == 1};*/
    int /*`@ refinement_type One*/ x;

	int test(){
		return x - 1;
	}
	
}