class A{
    int x;
    //@invariant x > 0;

    /*@requires y > 0;
      @ensures \result > 0;*/
    int hoge(int y){
        return x * y;
    }
}


class B extends A{
    //@invariant x % 2 == 0;

    /*@also
      @ requires y % 2 == 0;
      @ ensures \result % 2 == 0;*/
    int hoge(int y){
        return x + y;
    }
}