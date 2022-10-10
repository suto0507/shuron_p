class A{
    int x;

    A(){
        x = 0;
        //@assert false;
    }

    void test(){
        A a = new A();
        a.x = 3;
        m1(a);
        //@assert a.x == 3;
    }

    
    void m1(A a){
        a.x = 1;
        //@assert false;
    }
}