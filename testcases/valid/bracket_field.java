class A{
    int[] x;
    
    /*@requires x.length > 0;*/
    /*@ensures 771 == \result;*/
	int test(boolean b){
        (this).x[0] = 771;
		return x[0];
	}
}