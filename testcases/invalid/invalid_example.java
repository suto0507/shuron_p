class A{
    /*`@def_type Nat = {int x | x > 0};*/
    /*`@def_type Even = {int x | x % 2 == 0};*/
    /*`@def_type NatEven = {Nat x | x % 2 == 0};*/

    int/*`@ refinement_type Nat*/ x1;

    int/*`@ refinement_type Even*/ test(int/*`@ refinement_type NatEven*/ x){
        //このメソッドは正しい
        //@assert false;
		return 2;
	}
}

class B extends A{
    /*`@override_refinement_type {Super_type x | x % 2 == 0} x1;*/

	int/*`@ refinement_type {Super_type x | x > 0}*/ test(int x){
		return x1 + x + 1;
	}
}