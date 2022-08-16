class A{
    int x;
    
	
    /*@requires 1 == x;*/
    /*@ensures 1 == \old(x);*/
	int test(){
        x == 771;
		return 0;
	}
	

}