class model {
	//@model int x;
	int y; //@ in x;
	
	//@requires x == 2;
	void hoge (int a) {
		if(a > 0)y = 8;
		if(a < 0){
			//@assert x == 2;
		}else{
			//@assert x == 2;
		}
	}
}