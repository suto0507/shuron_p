class test2 {
	int[] ar;
	
	/*@ requires x<=1;*/
	/*@ assignable ar[1];*/
	/*@also*/
	/*@ requires x>-1;*/
	/*@ assignable ar[1], ar[2];*/
    /*@also*/
	/*@ requires x==3;*/
	/*@ assignable ar[2];*/
	int method1(int x){
		if(x == 1){
			method2(x);
		}
		return x;
	}
	
	//@ requires 1 == x;
	//@ assignable ar[x];
	void method2(int x){
		ar[x] = 771;
	}
}