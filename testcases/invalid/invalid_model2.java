class model2 {
	//@model int x;
	int y; //@ in x;
    //@represents x = y + 5;
	
	//@requires x == 10;
	void hoge1 (int a) {
        //@assert y == 5;
		if(a > 0)y = 8;
		if(a < 0){
			//@assert x == 10;
		}else{
            //@assert x == 13;
        }
	}

	//@requires x == 10;
	void hoge2 (int a) {
        //@assert y == 5;
		if(a >= 0)y = 8;
		if(a < 0){
			//@assert x == 9;
		}else{
            //@assert x == 13;
        }
	}

	//@requires x == 10;
	void hoge3 (int a) {
        //@assert y == 5;
		if(a >= 0)y = 8;
		if(a < 0){
			//@assert x == 10;
		}else{
            //@assert x == 12;
        }
	}

	//@requires x == 10;
	void hoge4 (int a) {
        //@assert y == 4;
		if(a >= 0)y = 8;
		if(a < 0){
			//@assert x == 10;
		}else{
            //@assert x == 13;
        }
	}
}