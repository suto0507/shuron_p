class if_test {
	int x;
	//@ensures x == 771;
	int test1(){
		if(x > 0){
			x = 771;
			return 9;
		}
		//@assert x <= 0;
		
		x = 771;
		return 6;
	}
}