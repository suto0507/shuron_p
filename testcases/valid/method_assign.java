class A{
    int x;

    A(){
        x = 0;
    }

    void test(){
        A a = new A();
        a.x = 3;
        m1(a);
        //@assert a.x == 1;
    }

    //@ensures a.x == 1;
    //@assignable a.x;
    void m1(A a){
        a.x = 1;
    }
}