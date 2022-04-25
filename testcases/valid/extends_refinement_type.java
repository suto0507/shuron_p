class A{
	/*`@ def_type Nat = {int x | x > 0};*/
	
	int/*`@ refinement_type Nat*/ test(){
		return 1;
	}
	
	int/*`@ refinement_type Nat*/ test2(){
		return 3;
	}
	
	int/*`@ refinement_type Nat*/ test3(){
		return 5;
	}
	
	int/*`@ refinement_type {int x | x > 0}*/ test4(){
		return 7;
	}
}

class B extends A{
	/*`@def_type NatEven = {int x | x % 2 == 0};*/

	int/*`@ refinement_type Nat*/ test(){
		return 2;
	}
	
	int/*`@ refinement_type {Nat x | x % 2 == 0}*/ test2(){
		return 4;
	}
	
	int/*`@ refinement_type {Super_type x | x % 2 == 0}*/ test3(){
		return 6;
	}
	
	int/*`@ refinement_type {Super_type x | x % 2 == 0}*/ test4(){
		return 8;
	}
}
