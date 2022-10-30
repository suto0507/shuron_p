class A{
    A a;
    int/*`@ refinement_type {int v | v > y}*/ x;
    final int y;


    //@requires a.y == 2;
    //@helper
    void test(A arg){
        a.x = -1;
        a.x = 3;
        a = arg;
    }

    //@requires a.y == 2;
    //@requires arg.y == 2;
    //@helper
    void test2(A arg){
        a.x = -1;
        a.x = 3;
        a = arg;
        a.x = 4;
    }
}