class A{
	/*`@ def_type Nat = {int x | x > 0};*/
    /*`@def_type NatEven = {Nat x | x % 2 == 0};*/
	/*`@def_type NatEvenMini = {NatEven x | x < 20};*/
	
	int test(int/*`@ refinement_type Nat*/ x){
		return 1;
	}
	
	int test2(int/*`@ refinement_type {Nat x | x % 2 == 0}*/ x){
		return 3;
	}
	
	int test3(int/*`@ refinement_type NatEven*/ x){
		return 9;
	}

	int test4(int/*`@ refinement_type NatEvenMini*/ x){
		return 11;
	}

    int test5(final int/*`@ refinement_type {int x | x > 1}*/ x, int/*`@ refinement_type {int y | y < 2}*/ y){
		return 11;
	}
}

class B extends A{
	

	int test(int/*`@ refinement_type Nat*/ x){
		return 2;
	}
	
	int test2(int/*`@ refinement_type Nat*/ x){
		return 4;
	}
	
	int test3(int/*`@ refinement_type Nat*/ x){
		return 10;
	}

	int test4(int/*`@ refinement_type Nat*/ x){
		return 12;
	}

    int test5(final int/*`@ refinement_type {int x | x > 0}*/ x, int/*`@ refinement_type {int y | y < x}*/ y){
		return 11;
	}
}
