class model2 {
	//@model int x;
	int y; //@ in x;
    //@represents x = y + 5;
	
	//@requires x == 10;
	void hoge (int a) {
        //@assert y == 5;
		if(a >= 0)y = 8;
		if(a < 0){
			//@assert x == 10;
		}else{
            //@assert x == 13;
        }
	}
}