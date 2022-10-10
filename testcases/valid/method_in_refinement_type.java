class A{
    int/*`@refinement_type{int x | x > plus(1,2)}*/ x;

    A(){
        x = 5;
    }

    //@ensures \result == \old(a) + \old(b);
    //@pure helper
    int plus(int a, int b){
        return a + b;
    }
}