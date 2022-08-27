class A{
    /*@requires 0 == x;*/
    /*@ensures -1 == \result;*/
	int test(int x){
		return x - 1;
	}
}