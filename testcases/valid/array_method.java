class A{
    //`@ def_type NatArray = {int[] x | (\forall int i; 0 <= i && i < x.length; x[i] >= 0)};
    int[] /*`@refinement_type NatArray*/ x;

    //`@ def_type NatArray2 = {int[][] x | (\forall int i,j; 0 <= i && i < x.length && 0 <= j && j < x[i].length; x[i][j] >= 0)};
    int[][] /*`@refinement_type NatArray2*/ x2;

    //篩型を返すメソッド

    //@ensures \result == x;
    //@pure
    int[]/*`@refinement_type NatArray*/ rt_return(){
        return x;
    }

    //@ensures \result == x2;
    //@pure
    int[][]/*`@refinement_type NatArray2*/ rt_return2(){
        return x2;
    }




    void test2(int[]/*`@refinement_type NatArray*/ a2){
        a2 = rt_return();
    }

    int[]/*`@refinement_type NatArray*/ test3(){
        return rt_return();
    }

    //@ requires x.length == 5 && a2 > 0;
    void test4(int a2){
        rt_return()[0] = a2;
    }


    //@ requires x.length == 3;
    void test7(){
        int[]/*`@refinement_type NatArray*/ local;
        int[][]/*`@refinement_type NatArray2*/ local2;
        local = new int[3];
        local2 = new int[2][3];
        local = rt_return();
        local2[1] = rt_return();
        local[0] = 1;
        local2[1][1] = 2;
    }

    void test8(){
        int[]/*`@refinement_type NatArray*/ local = rt_return();
        int[][]/*`@refinement_type NatArray2*/ local2 = rt_return2();
    }


}