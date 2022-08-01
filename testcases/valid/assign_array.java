class assign_array {
	//@invariant ar.length == 3;
	int[] ar;
	
	/*@ requires x>0;*/
	/*@ assignable ar[1];*/
    /*@also*/
	/*@ requires x==3;*/
	/*@ assignable ar[1], ar[2];*/
	int method1(int x){
		if(x == 1){
			method2(x);
		}else if(x == 3){
			ar[1] = 27;//2には代入できない
		}
		return x;
	}
	
	//@ requires 1 == x;
	//@ assignable ar[x];
	void method2(int x){
		ar[x] = 771;
	}
}