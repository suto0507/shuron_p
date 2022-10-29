class A{
    /*`@ def_type Nat = {int x | x > 0};*/
	
	int/*`@ refinement_type Nat*/ x;
    A a;

    //10まで保証

    //@requires x > 0;
    //@requires a.x > 0;
    //@requires a.a.x > 0;
    //@requires a.a.a.x > 0;
    //@requires a.a.a.a.x > 0;
    //@requires a.a.a.a.a.x > 0;
    //@requires a.a.a.a.a.a.x > 0;
    //@requires a.a.a.a.a.a.a.x > 0;
    //@requires a.a.a.a.a.a.a.a.x > 0;
    //@requires a.a.a.a.a.a.a.a.a.x > 0;
    //@helper
    void helper_m(){
        test();
    }

    void test(){
        x = 7;
    }
}