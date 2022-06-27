class assign_array {
	
	/*@ requires 0 <= x;
	  @ ensures 0 <= \result;
	  @also
	  @ requires x == 1;
	  @ ensures \result == 2;
      @also
	  @ requires x==3;
	  @ ensures \result == 3;*/
	int method1(int x){
		if(x == 1){
			x = method2(x);
		}
		return x;
	}
	
	//@ requires 1 == x;
	//@ ensures \result == x * 2;
	void method2(int x){
		return x * 2;
	}
}