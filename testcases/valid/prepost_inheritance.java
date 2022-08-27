class A{
    /*@requires 1 == x;*/
    /*@ensures 1 == \result;*/
	int test(int x){
		return 1;
	}
}

class B extends A{
    /*@also
      @ requires 2 == x;
      @ ensures 2 == \result;*/
	int test(int x){
        if (x == 2) return 2;
		return 1;
	}
}