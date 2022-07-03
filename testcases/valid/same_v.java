class A{
    /*`@def_type Nat = {int x | x > 0};*/

    int/*`@ refinement_type Nat*/ x;

	//@ ensures \result > 0 && x > 0;
	int method1(){
		return x;
	}

}

class B extends A{
    int/*`@ refinement_type {int x | x % 2 == 0}*/ x;

	//@ ensures \result % 2 == 0 && x % 2 == 0;
	int method2(){
		return x;
	}
}