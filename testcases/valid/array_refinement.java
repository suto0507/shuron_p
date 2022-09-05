class A{
    //`@ def_type NatArray = {int[] x | (\forall int i; 0 <= i && i < x.length; x[i] >= 0)};
    int[] /*`@refinement_type NatArray*/ x;

    //`@ def_type NatArray2 = {int[][] x | (\forall int i,j; 0 <= i && i < x.length && 0 <= j && j < x[i].length; x[i][j] >= 0)};
    int[][] /*`@refinement_type NatArray2*/ x2;


    void test1(){
        int[] local;
        local = new int[10];//ここを7にしたときだけUnreachbleがlocal[0] = 771;の時に出る。8以上でもvalidになるし意味が分からん//4,5もダメになった？
        local[0] = 771;
        local[1] = 771;
        x = local;
    }

    void test2(int[]/*`@refinement_type NatArray*/ a2){
        x = a2;
    }

    int[]/*`@refinement_type NatArray*/ test3(){
        return x;
    }

    //@ requires x.length == 5 && a2 > 0;
    void test4(int a2){
        x[0] = a2;
    }

    
    void test5(){
        x = new int[3];
    }

    //@ requires x2.length == 3;
    void test6(){
        x = x2[0];
    }

    void test7(){
        int[]/*`@refinement_type NatArray*/ local;
        int[][]/*`@refinement_type NatArray2*/ local2;
        local = new int[3];
        local2 = new int[2][3];
        x = local;
        x = local2[1];
        local[0] = 1;
        local2[1][1] = 2;
        local2[0] = local;
    }

    void test8(){
        int[]/*`@refinement_type NatArray*/ local = x;
        int[][]/*`@refinement_type NatArray2*/ local2 = x2;
    }


}