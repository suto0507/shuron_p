class A{
	/*@' def_type Nat = {int x | x > 0};*/
	
	/*@' refinement_type Nat*/int test(){
		return 1;
	}
	
	/*@' refinement_type Nat*/int test2(){
		return 3;
	}
	
	/*@' refinement_type Nat*/int test3(){
		return 5;
	}
	
	/*@' refinement_type {int x | x > 0}*/int test4(){
		return 7;
	}
}

class B extends A{
	/*@'def_type NatEven = {int x | x % 2 == 0};*/

	/*@' refinement_type Nat*/int test(){
		return 2;
	}
	
	/*@' refinement_type {Nat x | x % 2 == 0}*/int test2(){
		return 4;
	}
	
	/*@' refinement_type {Super_type x | x % 2 == 0}*/int test3(){
		return 6;
	}
	
	/*@' refinement_type {Super_type x | x % 2 == 0}*/int test4(){
		return 8;
	}
}
