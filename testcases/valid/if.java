class A{
    int x;
    
	
    /*@requires 1 == x;*/
    /*@ensures 771 == \result;*/
	int test(boolean b){
        x = 771;
        int y = 0;
        if(b){
            y = 771;
        }else{
            y = x;
        }
		return y;
	}
	
    //@ensures x == \old(x) + 1;
    int test2(boolean b){
        if(b){
            x = x + 1;
        }else{
            x = x + 1;
        }
		return 3;
	}
}