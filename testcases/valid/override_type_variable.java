class A{
	/*`@ def_type Nat = {int x | x > 0};*/
    int x1;
    int/*`@ refinement_type Nat*/ x2;
    int/*`@ refinement_type Nat*/ x3;
    int/*`@ refinement_type Nat*/ x4;
    int/*`@ refinement_type {int x | x > 0}*/ x5;
    int/*`@ refinement_type Nat*/ x6;
    int/*`@ refinement_type Nat*/ x7;
    int/*`@ refinement_type Nat*/ x8;
	
	int/*`@ refinement_type Nat*/ test(){
		return 1;
	}
	

}

class B extends A{
	/*`@def_type NatEven = {Nat x | x % 2 == 0};*/
	/*`@def_type NatEvenMini = {NatEven x | x < 20};*/

    /*`@override_refinement_type Nat x1;*/
    /*`@override_refinement_type Nat x2;*/
    /*`@override_refinement_type {Nat x | x % 2 == 0} x3;*/
    /*`@override_refinement_type {Super_type x | x % 2 == 0} x4;*/
    /*`@override_refinement_type {Super_type x | x % 2 == 0} x5;*/
    /*`@override_refinement_type NatEven x6;*/
    /*`@override_refinement_type NatEvenMini x7;*/
    /*`@override_refinement_type int x8;*/ 

	int/*`@ refinement_type Nat*/ test(){
		return 2;
	}
	
}