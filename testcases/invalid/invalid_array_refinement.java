class A{
    //`@ def_type NatArray = {int[] x | (\forall int i; 0 <= i && i < x.length; x >= 0)};
    int[] /*`@refinement_type NatArray*/ x;

    //`@ def_type PosArray = {int[] x | (\forall int i; 0 <= i && i < x.length; x[i] > 0)};
    int[] /*`@refinement_type PosArray*/ p;

    //`@ def_type PosArray2 = {int[][] x | (\forall int i,j; 0 <= i && i < x.length && 0 <= j && j < x.length; x[i][j] > 0)};
    int[][] /*`@refinement_type PosArray2*/ p2;

    void test1(){
        int[] local;
        local = new int[1];
        local[0] = 771;
        local[1] = 771;
        int[] local2;
        local = local2;
        x = local;
    }

    void test1_2(){
        int[] local;
        local = new int[1];
        local[0] = 771;
        local[1] = 771;
        int[] local2;
        x = local;
        local = local2;
    }

    void test2(){
        int[] local;
        local = new int[1];
        local[0] = 771;
        local[1] = 771;
        int[] local2;
        local2 = local;
        x = local;
    }

    void test2_2(){
        int[] local;
        local = new int[1];
        local[0] = 771;
        local[1] = 771;
        int[] local2;
        x = local;
        local2 = local;
    }

    void test3(int[] local2){
        int[] local;
        local = new int[1];
        local[0] = 771;
        local[1] = 771;
        local = local2;
        x = local;
    }

    void test3_2(int[] local2){
        int[] local;
        local = new int[1];
        local[0] = 771;
        local[1] = 771;
        x = local;
        local = local2;
    }

    void test4(int[] local2){
        int[] local;
        local = new int[1];
        local[0] = 771;
        local[1] = 771;
        local = local2;
        x = local;
    }

    void test4_2(int[] local2){
        int[] local;
        local = new int[1];
        local[0] = 771;
        local[1] = 771;
        x = local;
        local = local2;
    }

    int[] test5(){
        int[] local;
        local = new int[1];
        local[0] = 771;
        local[1] = 771;
        x = local;
        return local;
    }



    void test6(int[] a2){
        x = a2;
    }

    int[] test7(){
        return x;
    }

    void test8(int[] a2){
        x = p;
    }

    void test9(int[] a2){
        p = x;
    }

    void test10(int[] a2){
        x = p2;
    }

    void test11(int[] a2){
        p2 = x;
    }

    void test12(int[] a2){
        int[]/*`@refinement_type NatArray*/ local = a2;
    }
}